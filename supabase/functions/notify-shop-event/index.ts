import {
  assertKnownEventType,
  createServiceClient,
  sendEventNotifications,
  ShopEvent,
} from "../_shared/event-notifications.ts";

type WebhookPayload = { type: "INSERT"; table: "shop_events"; schema: "public"; record: ShopEvent };

Deno.serve(async (request) => {
  const webhookSecret = Deno.env.get("WEBHOOK_SECRET");
  if (!webhookSecret) {
    return new Response("Service unavailable", { status: 503 });
  }
  if (request.headers.get("x-webhook-secret") !== webhookSecret) {
    return new Response("Unauthorized", { status: 401 });
  }

  const payload = await request.json() as WebhookPayload;
  if (payload.type !== "INSERT" || payload.table !== "shop_events") {
    return new Response("Ignored", { status: 202 });
  }

  try {
    assertKnownEventType(payload.record.event_type);
    return Response.json(await sendEventNotifications(createServiceClient(), payload.record, "new_event"));
  } catch (error) {
    console.error(error);
    return Response.json({ error: error instanceof Error ? error.message : String(error) }, { status: 500 });
  }
});
