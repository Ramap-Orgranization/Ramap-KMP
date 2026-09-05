import { createServiceClient } from "../_shared/event-notifications.ts";

const ADMIN_TIMEOUT_MS = 3_000;

Deno.serve(async (request) => {
  if (request.method !== "POST") return json({ code: "method_not_allowed" }, 405);
  if (!await isAdministrator(request)) return json({ code: "administrator_required" }, 403);

  const body = await request.json().catch(() => null) as Record<string, unknown> | null;
  if (body?.action === "list") return listActiveEvents();
  if (body?.action === "update") return updateEventStatus(body);
  if (body?.action === "edit") return editEvent(body);
  return json({ code: "invalid_action" }, 400);
});

async function listActiveEvents() {
  const today = koreaToday();
  const supabase = createServiceClient();
  const { data, error } = await supabase
    .from("shop_events")
    .select("id,title,description,event_type,start_date,end_date,cancelled_dates,sold_out_dates,shops!inner(name)")
    .lte("start_date", today)
    .or(`end_date.gte.${today},end_date.is.null`)
    .order("end_date");
  if (error) return json({ code: "server_unavailable" }, 503);
  return json((data ?? []).map((event) => ({
    id: event.id,
    title: event.title,
    start_date: event.start_date,
    end_date: event.end_date,
    shop_name: event.shops.name,
    description: event.description,
    event_type: event.event_type,
    cancelled_dates: event.cancelled_dates ?? [],
    sold_out_dates: event.sold_out_dates ?? [],
  })));
}

async function updateEventStatus(body: Record<string, unknown>) {
  const eventId = text(body.event_id);
  const status = text(body.status);
  const scope = text(body.scope);
  const reason = text(body.reason);
  const startDate = text(body.start_date);
  const endDate = text(body.end_date);
  if (!eventId || !isStatus(status) || !isScope(scope) || (status === "cancelled" && !reason) || reason?.length > 1_000 || (scope === "custom_period" && (status !== "cancelled" || !validDate(startDate) || !validDate(endDate) || startDate > endDate))) {
    return json({ code: "invalid_request" }, 400);
  }

  const supabase = createServiceClient();
  const { error } = await supabase.rpc("merge_shop_event_status_dates", {
    p_event_id: eventId,
    p_status: status,
    p_scope: status === "sold_out" ? "today" : scope,
    p_reason: reason,
    p_today: koreaToday(),
    p_start_date: startDate,
    p_end_date: endDate,
  });
  return error ? json({ code: "update_failed" }, 422) : json({ success: true });
}

async function editEvent(body: Record<string, unknown>) {
  const eventId = text(body.event_id);
  const title = text(body.title);
  const description = text(body.description);
  const eventType = text(body.event_type);
  const startDate = text(body.start_date);
  const requestedEndDate = text(body.end_date) ?? startDate;
  if (!eventId || !title || !description || !eventType || !startDate || !validDate(startDate) || !validDate(requestedEndDate) || requestedEndDate < startDate || !isEventType(eventType)) {
    return json({ code: "invalid_request" }, 400);
  }

  const endDate = eventType === "new_menu" ? dateAfterDays(startDate, 6) : requestedEndDate;
  const { data, error } = await createServiceClient()
    .from("shop_events")
    .update({
      title,
      description,
      event_type: eventType,
      start_date: startDate,
      end_date: eventType === "store_renewal" ? null : endDate,
    })
    .eq("id", eventId)
    .or(`end_date.gte.${koreaToday()},end_date.is.null`)
    .select("id")
    .maybeSingle();
  if (error) return json({ code: "update_failed" }, 422);
  return data ? json({ success: true }) : json({ code: "event_not_editable" }, 409);
}

function koreaToday() {
  return new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Seoul" }).format(new Date());
}

async function isAdministrator(request: Request): Promise<boolean> {
  const token = request.headers.get("authorization")?.match(/^Bearer\s+(.+)$/i)?.[1]?.trim();
  const url = Deno.env.get("SUPABASE_URL");
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY");
  const adminEmail = (Deno.env.get("ADMIN_EMAIL") ?? "uni070@naver.com").trim().toLowerCase();
  if (!token || !url || !anonKey || !adminEmail) return false;
  const response = await fetch(`${url}/auth/v1/user`, {
    headers: { apikey: anonKey, Authorization: `Bearer ${token}` },
    signal: AbortSignal.timeout(ADMIN_TIMEOUT_MS),
  }).catch(() => null);
  const user = response?.ok ? await response.json() as { email?: unknown } : null;
  return typeof user?.email === "string" && user.email.toLowerCase() === adminEmail;
}

function isStatus(value: string | null): value is "sold_out" | "cancelled" {
  return value === "sold_out" || value === "cancelled";
}

function isScope(value: string | null): value is "today" | "entire_period" | "custom_period" {
  return value === "today" || value === "entire_period" || value === "custom_period";
}

function validDate(value: string | null): value is string {
  if (value === null || !/^\d{4}-\d{2}-\d{2}$/.test(value)) return false;
  const date = new Date(`${value}T00:00:00Z`);
  return !Number.isNaN(date.valueOf()) && date.toISOString().startsWith(value);
}

function isEventType(value: string): boolean {
  return ["collab", "popup", "limited_menu", "summer_limited", "new_menu", "store_renewal"].includes(value);
}

function dateAfterDays(value: string, days: number): string {
  const date = new Date(`${value}T00:00:00Z`);
  date.setUTCDate(date.getUTCDate() + days);
  return date.toISOString().slice(0, 10);
}

function text(value: unknown): string | null {
  return typeof value === "string" && value.trim() !== "" ? value.trim() : null;
}

function json(body: unknown, status = 200): Response {
  return Response.json(body, { status });
}
