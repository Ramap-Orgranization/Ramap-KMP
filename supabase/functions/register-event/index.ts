import { assertKnownEventType, createServiceClient } from "../_shared/event-notifications.ts";

const EVIDENCE_BUCKET = "news-report-evidence";
const EVENT_BUCKET = "event-images";
const ADMIN_TIMEOUT_MS = 3_000;
type Participant = { name: string; instagramUrl: string | null };

Deno.serve(async (request) => {
  if (request.method !== "POST") return json({ code: "method_not_allowed" }, 405);
  if (!await isAdministrator(request)) return json({ code: "administrator_required" }, 403);

  const body = await request.json().catch(() => null) as Record<string, unknown> | null;
  const registrationType = text(body?.registration_type) ?? "event";
  if (registrationType !== "event" && registrationType !== "operating_notice") {
    return json({ code: "invalid_registration_type" }, 400);
  }
  const supabase = createServiceClient();
  if (registrationType === "operating_notice") return registerOperatingNotice(supabase, body ?? {});

  const shopName = text(body?.shop_name);
  const title = text(body?.title);
  const eventType = text(body?.event_type) ?? "limited_menu";
  const startDate = text(body?.start_date);
  const endDate = text(body?.end_date) ?? startDate;
  const description = text(body?.description);
  const participants = parseParticipants(body?.participants);
  const sourceUrl = normalizeInstagramUrl(text(body?.source_url));
  const evidencePath = text(body?.evidence_path);
  const imageOnly = body?.image_only === true;
  if (!shopName || !title || !startDate || !validDate(startDate) || !validDate(endDate)) {
    return json({ code: "invalid_draft" }, 400);
  }
  if (imageOnly ? !isEvidencePath(evidencePath) : !description || !sourceUrl || !isInstagramUrl(sourceUrl)) {
    return json({ code: "invalid_draft" }, 400);
  }
  if (!participants) return json({ code: "invalid_draft" }, 400);
  const eventDescription = description ?? "";
  const eventSourceUrl = sourceUrl ?? "";
  try {
    assertKnownEventType(eventType);
  } catch {
    return json({ code: "invalid_draft" }, 400);
  }

  const { data: shops, error: shopError } = await supabase.from("shops").select("id").eq("name", shopName).limit(2);
  if (shopError) return json({ code: "server_unavailable" }, 503);
  if (!shops || shops.length !== 1) return json({ code: "shop_not_resolved" }, 422);
  const shopId = shops[0].id as string;

  const { data: duplicates, error: duplicateError } = await supabase
    .from("shop_events")
    .select("id")
    .eq("shop_id", shopId)
    .eq("source_url", eventSourceUrl)
    .eq("title", title)
    .eq("start_date", startDate)
    .limit(1);
  if (duplicateError) return json({ code: "server_unavailable" }, 503);
  if (duplicates && duplicates.length > 0) {
    await deleteEvidence(supabase, evidencePath);
    return json({ code: "duplicate" }, 409);
  }

  const eventId = crypto.randomUUID();
  let imagePath: string | null = null;
  let eventCreated = false;
  try {
    if (evidencePath && /^[\w-]+\.(?:jpe?g|png)$/i.test(evidencePath)) {
      imagePath = `events/${eventId}/1.${evidencePath.split(".").pop()!.toLowerCase()}`;
      const { data: file, error: downloadError } = await supabase.storage.from(EVIDENCE_BUCKET).download(evidencePath);
      if (downloadError || !file) throw downloadError ?? new Error("Evidence not found");
      const { error: uploadError } = await supabase.storage.from(EVENT_BUCKET).upload(imagePath, await file.arrayBuffer(), {
        contentType: file.type || "image/jpeg",
        upsert: false,
      });
      if (uploadError) throw uploadError;
    }

    const { error: insertError } = await supabase.from("shop_events").insert({
      id: eventId,
      shop_id: shopId,
      title,
      description: eventDescription,
      start_date: startDate,
      end_date: eventType === "store_renewal" ? null : endDate,
      source_url: eventSourceUrl,
      event_type: eventType,
      image_paths: imagePath ? [imagePath] : [],
      cancelled_dates: [],
      sold_out_dates: [],
    });
    if (insertError) throw insertError;
    eventCreated = true;
    await saveParticipants(supabase, eventId, shopId, participants);
    await deleteEvidence(supabase, evidencePath);
    return json({ id: eventId });
  } catch (error) {
    if (eventCreated) {
      await supabase.from("shop_event_participants").delete().eq("event_id", eventId);
      await supabase.from("shop_events").delete().eq("id", eventId);
    }
    if (imagePath) await supabase.storage.from(EVENT_BUCKET).remove([imagePath]);
    await deleteEvidence(supabase, evidencePath);
    console.error("register-event failed", error);
    return json({ code: "registration_failed" }, 422);
  }
});

async function deleteEvidence(supabase: ReturnType<typeof createServiceClient>, path: string | null) {
  if (isEvidencePath(path)) await supabase.storage.from(EVIDENCE_BUCKET).remove([path]);
}

function parseParticipants(value: unknown): Participant[] | null {
  if (value === undefined) return [];
  if (!Array.isArray(value)) return null;
  const participants: Participant[] = [];
  const names = new Set<string>();
  const instagramUrls = new Set<string>();
  for (const item of value) {
    if (!item || typeof item !== "object" || Array.isArray(item)) return null;
    const name = text((item as Record<string, unknown>).name);
    const instagramUrl = normalizeInstagramProfileUrl((item as Record<string, unknown>).instagram_url);
    if (!name || ((item as Record<string, unknown>).instagram_url !== null && !instagramUrl)) return null;
    const nameKey = name.toLowerCase();
    if (names.has(nameKey) || (instagramUrl && instagramUrls.has(instagramUrl))) continue;
    names.add(nameKey);
    if (instagramUrl) instagramUrls.add(instagramUrl);
    participants.push({ name, instagramUrl });
  }
  return participants;
}

async function saveParticipants(
  supabase: ReturnType<typeof createServiceClient>,
  eventId: string,
  hostShopId: string,
  participants: Participant[],
) {
  const rows = [];
  const shopIds = new Set<string>();
  for (const participant of participants) {
    const shopId = await resolveParticipantShop(supabase, participant);
    if (shopId === hostShopId || (shopId && shopIds.has(shopId))) continue;
    if (shopId) shopIds.add(shopId);
    rows.push(shopId
      ? { event_id: eventId, shop_id: shopId, external_name: null, external_instagram_url: null }
      : { event_id: eventId, shop_id: null, external_name: participant.name, external_instagram_url: participant.instagramUrl });
  }
  if (rows.length === 0) return;
  const { error } = await supabase.from("shop_event_participants").insert(rows);
  if (error) throw error;
}

async function resolveParticipantShop(supabase: ReturnType<typeof createServiceClient>, participant: Participant) {
  if (participant.instagramUrl) {
    const handle = extractInstagramUsername(participant.instagramUrl);
    const urls = handle ? [
      `https://www.instagram.com/${handle}`,
      `https://www.instagram.com/${handle}/`,
      `https://instagram.com/${handle}`,
      `https://instagram.com/${handle}/`,
    ] : [];
    const { data, error } = await supabase.from("shops").select("id").in("instagram_url", urls).limit(2);
    if (error) throw error;
    if (data?.length === 1) return data[0].id as string;
  }
  const { data, error } = await supabase.from("shops").select("id").eq("name", participant.name).limit(2);
  if (error) throw error;
  return data?.length === 1 ? data[0].id as string : null;
}

async function registerOperatingNotice(
  supabase: ReturnType<typeof createServiceClient>,
  body: Record<string, unknown>,
) {
  const shopName = text(body.shop_name);
  const noticeType = text(body.notice_type);
  const startDate = text(body.start_date);
  const endDate = text(body.end_date) ?? startDate;
  const startTime = text(body.start_time);
  const endTime = text(body.end_time);
  const description = text(body.description);
  const sourceUrl = normalizeInstagramUrl(text(body.source_url));
  const evidencePath = text(body.evidence_path);
  if (
    !shopName || !isSupportedNoticeType(noticeType) || !startDate || !endDate || !description || !sourceUrl ||
    !validDate(startDate) || !validDate(endDate) || (startTime && !validTime(startTime)) ||
    (endTime && !validTime(endTime)) || !isInstagramUrl(sourceUrl)
  ) return json({ code: "invalid_operating_notice_draft" }, 400);

  const { data: shops, error: shopError } = await supabase.from("shops").select("id,instagram_url").eq("name", shopName).limit(2);
  if (shopError) return json({ code: "server_unavailable" }, 503);
  if (!shops || shops.length !== 1) return json({ code: "shop_not_resolved" }, 422);
  const shop = shops[0] as { id: string; instagram_url?: string | null };

  const { data: duplicates, error: duplicateError } = await supabase
    .from("shop_operating_notices")
    .select("id")
    .eq("shop_id", shop.id)
    .eq("source_url", sourceUrl)
    .eq("notice_type", noticeType)
    .eq("notice_date", startDate)
    .limit(1);
  if (duplicateError) return json({ code: "server_unavailable" }, 503);
  if (duplicates && duplicates.length > 0) {
    await deleteEvidence(supabase, evidencePath);
    return json({ code: "duplicate" }, 409);
  }

  try {
    const rawPostId = await ensureRawPost(supabase, shop.id, shop.instagram_url, sourceUrl, description);
    const { data: notice, error: insertError } = await supabase
      .from("shop_operating_notices")
      .insert({
        shop_id: shop.id,
        raw_post_id: rawPostId,
        notice_type: noticeType,
        description,
        notice_date: startDate,
        end_date: endDate,
        start_time: startTime,
        end_time: endTime,
        source_url: sourceUrl,
        review_note: "관리자 미리보기 승인 등록",
      })
      .select("id")
      .single();
    if (insertError || !notice) throw insertError ?? new Error("Operating notice was not created");
    await deleteEvidence(supabase, evidencePath);
    return json({ id: notice.id });
  } catch (error) {
    console.error("register-operating-notice failed", error);
    return json({ code: "registration_failed" }, 422);
  }
}

async function ensureRawPost(
  supabase: ReturnType<typeof createServiceClient>,
  shopId: string,
  instagramUrl: string | null | undefined,
  sourceUrl: string,
  description: string,
) {
  const { data: existing, error: existingError } = await supabase
    .from("instagram_posts_raw")
    .select("id")
    .eq("source_url", sourceUrl)
    .maybeSingle();
  if (existingError) throw existingError;
  if (existing) return existing.id as string;

  const { data: inserted, error } = await supabase
    .from("instagram_posts_raw")
    .insert({
      shop_id: shopId,
      instagram_username: extractInstagramUsername(instagramUrl) ?? "ramap_admin",
      source_type: sourceUrl.includes("/reel/") ? "reel" : "post",
      source_url: sourceUrl,
      source_key: sourceUrl,
      caption: description,
      raw_payload: { source: "admin_manual_registration" },
    })
    .select("id")
    .single();
  if (error || !inserted) throw error ?? new Error("Raw Instagram post was not created");
  return inserted.id as string;
}

async function isAdministrator(request: Request): Promise<boolean> {
  const token = request.headers.get("authorization")?.match(/^Bearer\s+(.+)$/i)?.[1]?.trim();
  const url = Deno.env.get("SUPABASE_URL"), anonKey = Deno.env.get("SUPABASE_ANON_KEY"), adminEmail = (Deno.env.get("ADMIN_EMAIL") ?? "uni070@naver.com").trim().toLowerCase();
  if (!token || !url || !anonKey || !adminEmail) return false;
  const response = await fetch(`${url}/auth/v1/user`, { headers: { apikey: anonKey, Authorization: `Bearer ${token}` }, signal: AbortSignal.timeout(ADMIN_TIMEOUT_MS) }).catch(() => null);
  const user = response?.ok ? await response.json() as { email?: unknown } : null;
  return typeof user?.email === "string" && user.email.toLowerCase() === adminEmail;
}

function isInstagramUrl(value: string) {
  try {
    const url = new URL(value);
    return url.protocol === "https:" && (url.hostname === "instagram.com" || url.hostname.endsWith(".instagram.com"));
  } catch {
    return false;
  }
}
function isEvidencePath(value: string | null): value is string { return value !== null && /^[\w-]+\.(?:jpe?g|png)$/i.test(value); }
function validDate(value: string | null) { return typeof value === "string" && /^\d{4}-\d{2}-\d{2}$/.test(value); }
function validTime(value: string | null) { return typeof value === "string" && /^([01]\d|2[0-3]):[0-5]\d$/.test(value); }
function isSupportedNoticeType(value: string | null) { return value === "operating_notice" || value === "full_close" || value === "early_close" || value === "late_opening"; }
function text(value: unknown): string | null { return typeof value === "string" && value.trim() !== "" ? value.trim() : null; }
function normalizeInstagramUrl(value: string | null) {
  if (!value) return null;
  const match = value.match(/https?:\/\/(?:www\.)?instagram\.com\/(p|reel)\/([^/?#\s\]"']+)/i);
  if (match) return `https://www.instagram.com/${match[1].toLowerCase()}/${match[2]}/`;
  const profileMatch = value.match(/https?:\/\/(?:www\.)?instagram\.com\/([^/?#\s\]"']+)/i);
  if (!profileMatch || ["p", "reel"].includes(profileMatch[1].toLowerCase())) return null;
  return `https://www.instagram.com/${profileMatch[1]}/`;
}
function normalizeInstagramProfileUrl(value: unknown) {
  if (value === null) return null;
  if (typeof value !== "string") return null;
  const match = value.trim().match(/^https?:\/\/(?:www\.)?instagram\.com\/([^/?#\s\]"']+)\/?(?:[?#].*)?$/i);
  if (!match || ["p", "reel"].includes(match[1].toLowerCase())) return null;
  return `https://www.instagram.com/${match[1]}/`;
}
function extractInstagramUsername(value: string | null | undefined) {
  return value?.match(/instagram\.com\/([^/?#]+)/i)?.[1]?.toLowerCase() ?? null;
}
function json(body: Record<string, unknown>, status = 200): Response { return Response.json(body, { status }); }
