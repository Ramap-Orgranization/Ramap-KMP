import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { stripTypeScriptTypes } from "node:module";
import { runInNewContext } from "node:vm";
import test from "node:test";
import { effectiveEndDate, isManageableEvent } from "../_shared/admin-event-policy.ts";

const source = stripTypeScriptTypes((await readFile(new URL("./index.ts", import.meta.url), "utf8")).replace(/^import .*;\n/gm, ""));
function harness(record = { event_type: "limited_menu", start_date: "2026-01-01", end_date: null }) {
  const updates = [];
  const query = {
    select() { return this; }, eq() { return this; },
    async maybeSingle() { return { data: record, error: null }; },
    update(value) { updates.push(value); return { eq: async () => ({ error: null }) }; },
  };
  const context = { Deno: { serve() {} }, Response, Date, Intl, effectiveEndDate, isManageableEvent, createServiceClient: () => ({ from: () => query }) };
  runInNewContext(source + "\nglobalThis.api = { validateChanges, applyCorrection, isActiveRecord };", context);
  return { ...context.api, updates };
}
test("event title and type corrections are retained instead of dropped by the notice branch", () => {
  const api = harness();
  assert.equal(api.validateChanges("event", { title: "Changed" }).title, "Changed");
  assert.equal(api.validateChanges("event", { event_type: "popup" }).event_type, "popup");
  assert.equal(api.validateChanges("operating_notice", { title: "Not supported" }), null);
});
test("title-only correction updates the event", async () => {
  const api = harness();
  const response = await api.applyCorrection("event", "event", { title: "Changed" });
  assert.equal(response.status, 200);
  assert.equal(api.updates[0].title, "Changed");
});
test("schedule-only correction preserves an existing operating notice type", async () => {
  const api = harness({ notice_type: "operating_notice", notice_date: "2099-01-01", end_date: null });
  const response = await api.applyCorrection("operating_notice", "notice", { schedule_override: { open: "11:00", close: "21:00" } });
  assert.equal(response.status, 200);
  assert.equal(api.updates[0].schedule_override.open, "11:00");
});
test("schedule override cannot be applied to a closure", async () => {
  const api = harness({ notice_type: "full_close", notice_date: "2099-01-01", end_date: null });
  assert.equal((await api.applyCorrection("operating_notice", "notice", { schedule_override: { open: "11:00" } })).status, 400);
  assert.equal(api.updates.length, 0);
});
test("expired renewal and new-menu records cannot be corrected", () => {
  const api = harness();
  assert.equal(api.isActiveRecord("event", { event_type: "store_renewal", start_date: "2020-01-01", end_date: null }), false);
  assert.equal(api.isActiveRecord("event", { event_type: "new_menu", start_date: "2020-01-01", end_date: null }), false);
});
