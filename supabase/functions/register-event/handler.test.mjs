import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { stripTypeScriptTypes } from "node:module";
import { runInNewContext } from "node:vm";
import test from "node:test";
import { validCalendarDate, validDailySchedules } from "../_shared/operating-notice-schedule.ts";

const source = stripTypeScriptTypes((await readFile(new URL("./index.ts", import.meta.url), "utf8")).replace(/^import .*;\n/gm, ""));
const schedule = { closed: false, open: "11:00", close: "20:00", close_next_day: false, label: null, break_times: [] };
const base = { shop_name: "테스트 매장", title: "연속 일정", description: "테스트 일정", source_url: "https://www.instagram.com/test_shop/", start_date: "2026-10-30", end_date: "2026-11-02" };

function harness({ allowed = true, overlapping = false } = {}) {
  let handler;
  const calls = { inserts: [], filters: [], clients: 0 };
  function from(table) {
    const filters = [];
    let inserted = false;
    const query = {
      select() { return this; },
      eq(key, value) { filters.push(["eq", key, value]); return this; },
      lte(key, value) { filters.push(["lte", key, value]); return this; },
      or(value) { filters.push(["or", value]); return this; },
      not(...args) { filters.push(["not", ...args]); return this; },
      is() { return this; },
      delete() { return this; },
      insert(value) { inserted = true; calls.inserts.push({ table, value }); return this; },
      limit() { return this; },
      async maybeSingle() { return { data: { id: "raw-post" }, error: null }; },
      async single() { return { data: { id: "notice" }, error: null }; },
      then(resolve, reject) {
        calls.filters.push({ table, filters });
        const data = table === "ramen_shops" ? [{ id: "shop" }] :
          !inserted && table === "shop_operating_notices" && overlapping && filters.some(([op]) => op === "not") ? [{ id: "overlap" }] : [];
        return Promise.resolve({ data, error: null }).then(resolve, reject);
      },
    };
    return query;
  }
  runInNewContext(source, {
    Deno: { serve: (value) => { handler = value; } },
    Request, Response, URL, Date, crypto, console,
    validCalendarDate, validDailySchedules,
    isAdministrator: async () => allowed,
    createServiceClient: () => { calls.clients++; return { from }; },
    assertKnownEventType: (value) => { assert.ok(["collab", "popup", "limited_menu", "summer_limited", "new_menu", "store_renewal"].includes(value)); },
    normalizeMapUrl: () => null,
    requiresExistingVenueSelection: () => false,
  });
  return {
    calls,
    invoke: (body) => handler(new Request("https://example.com/register-event", { method: "POST", body: JSON.stringify(body), headers: { "Content-Type": "application/json" } })),
  };
}

test("only administrators can register date ranges", async () => {
  const app = harness({ allowed: false });
  assert.equal((await app.invoke(base)).status, 403);
  assert.equal(app.calls.clients, 0);
});

for (const type of ["limited_menu", "popup"]) {
  test(`${type} retains both dates across a month boundary`, async () => {
    const app = harness();
    assert.equal((await app.invoke({ ...base, registration_type: "event", event_type: type })).status, 200);
    const row = app.calls.inserts.find(({ table }) => table === "shop_events").value;
    assert.equal(row.start_date, base.start_date);
    assert.equal(row.end_date, base.end_date);
  });
}

for (const type of ["operating_notice", "full_close", "early_close", "late_opening"]) {
  test(`${type} retains a continuous multi-day range`, async () => {
    const app = harness();
    const body = { ...base, registration_type: "operating_notice", notice_type: type,
      schedule_override: type === "operating_notice" ? schedule : null,
      start_time: type === "late_opening" ? "12:00" : null,
      end_time: type === "early_close" ? "18:00" : null };
    const response = await app.invoke(body);
    assert.equal(response.status, 200, JSON.stringify(await response.json()));
    const row = app.calls.inserts.find(({ table }) => table === "shop_operating_notices").value;
    assert.equal(row.notice_date, base.start_date);
    assert.equal(row.end_date, base.end_date);
    assert.equal(JSON.stringify(row.schedule_override), JSON.stringify(body.schedule_override));
  });
}

test("a one-day operating change remains valid", async () => {
  const app = harness();
  assert.equal((await app.invoke({ ...base, end_date: base.start_date, registration_type: "operating_notice", notice_type: "operating_notice", schedule_override: schedule })).status, 200);
});

test("a conflicting operating schedule is checked across the whole range", async () => {
  const app = harness({ overlapping: true });
  const response = await app.invoke({ ...base, registration_type: "operating_notice", notice_type: "operating_notice", schedule_override: schedule });
  assert.equal(response.status, 409);
  assert.equal(app.calls.inserts.length, 0);
  const query = app.calls.filters.find(({ filters }) => filters.some(([op]) => op === "not"));
  assert.ok(query.filters.some(([op, key, value]) => op === "lte" && key === "notice_date" && value === base.end_date));
  assert.ok(query.filters.some(([op, value]) => op === "or" && value === `end_date.gte.${base.start_date},end_date.is.null`));
});

test("reversed and impossible ranges are rejected before any write", async () => {
  for (const registration_type of ["event", "operating_notice"]) {
    for (const dates of [{ start_date: "2026-11-02", end_date: "2026-10-30" }, { start_date: "2026-02-30", end_date: "2026-03-01" }]) {
      const app = harness();
      const response = await app.invoke({ ...base, ...dates, registration_type, notice_type: "operating_notice", schedule_override: schedule });
      assert.equal(response.status, 400);
      assert.equal(app.calls.inserts.length, 0);
    }
  }
});

test("explicit monthly dates remain separate from a continuous override", async () => {
  const app = harness();
  const daily_schedules = [{ date: "2026-10-12", closed: true, schedule_override: null }];
  const response = await app.invoke({ ...base, registration_type: "operating_notice", notice_type: "operating_notice", start_date: "2026-10-01", end_date: "2026-10-31", daily_schedules });
  assert.equal(response.status, 200);
  assert.equal(JSON.stringify(app.calls.inserts[0].value.daily_schedules), JSON.stringify(daily_schedules));
});
