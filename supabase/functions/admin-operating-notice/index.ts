import { createServiceClient } from "../_shared/event-notifications.ts";

const ADMIN_TIMEOUT_MS = 3_000;

Deno.serve(async (request) => {
  if (!await isAdministrator(request)) return json({ code: "administrator_required" }, 403);
  const supabase = createServiceClient();
  const body = await request.json().catch(() => null) as { action?: unknown; id?: unknown } | null;
  const action = body?.action;
  if (request.method === "GET" || action === "list" || (request.method === "POST" && body === null)) {
    const today = koreaToday();
    const { data, error } = await supabase.from("shop_operating_notices")
      .select("id,shop_id,notice_date,start_time,description,manually_released_at,ramen_shops(name,business_hours_weekly)")
      .eq("notice_type", "late_opening").gte("notice_date", today).is("manually_released_at", null)
      .order("notice_date").order("start_time");
    return error ? json({ code: "fetch_failed" }, 422) : json({ notices: data ?? [] });
  }
  if (request.method !== "POST") return json({ code: "method_not_allowed" }, 405);
  if (action !== undefined && action !== "release") return json({ code: "invalid_action" }, 400);
  const id = body?.id;
  if (typeof id !== "string" || !id) return json({ code: "invalid_notice" }, 400);
  const { data, error } = await supabase.rpc("release_shop_operating_notice", { notice_id: id });
  if (error) return json({ code: "release_failed" }, 422);
  if (data == null) return json({ code: "delayed_opening_not_found" }, 404);
  return json({ manually_released_at: data });
});

async function isAdministrator(request: Request): Promise<boolean> {
  const token = request.headers.get("authorization")?.match(/^Bearer\s+(.+)$/i)?.[1]?.trim();
  const url = Deno.env.get("SUPABASE_URL"), anonKey = Deno.env.get("SUPABASE_ANON_KEY");
  const adminEmail = (Deno.env.get("ADMIN_EMAIL") ?? "uni070@naver.com").trim().toLowerCase();
  if (!token || !url || !anonKey) return false;
  const response = await fetch(`${url}/auth/v1/user`, { headers: { apikey: anonKey, Authorization: `Bearer ${token}` }, signal: AbortSignal.timeout(ADMIN_TIMEOUT_MS) }).catch(() => null);
  const user = response?.ok ? await response.json() as { email?: unknown } : null;
  return typeof user?.email === "string" && user.email.toLowerCase() === adminEmail;
}

function koreaToday(): string {
  const parts = new Intl.DateTimeFormat("en", { timeZone: "Asia/Seoul", year: "numeric", month: "2-digit", day: "2-digit" }).formatToParts();
  return `${parts.find((part) => part.type === "year")!.value}-${parts.find((part) => part.type === "month")!.value}-${parts.find((part) => part.type === "day")!.value}`;
}
function json(body: unknown, status = 200): Response { return Response.json(body, { status }); }
