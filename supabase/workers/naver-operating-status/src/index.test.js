import test from "node:test"
import assert from "node:assert/strict"
import { chromium } from "playwright"
import { extractNaverPlaceId, isPreOpenCheckWindow, nextCheckAt, normalizeStatus, parseLimit, processShop } from "./index.js"

test("extractNaverPlaceId reads a place id from Naver Map URLs", () => {
  assert.equal(extractNaverPlaceId("https://map.naver.com/p/entry/place/2017341952"), "2017341952")
  assert.equal(extractNaverPlaceId("not-a-url"), null)
})

test("normalizeStatus returns the last non-blank status", () => {
  assert.equal(normalizeStatus(["", "오늘 휴무", ""]), "오늘 휴무")
  assert.equal(normalizeStatus([]), null)
})

test("parseLimit bounds cron batches", () => {
  assert.equal(parseLimit(undefined), 10)
  assert.equal(parseLimit("3"), 3)
  assert.equal(parseLimit("100"), 50)
})

const weekly = Object.fromEntries([
  ["mon", { closed: false, open: "11:30", close: "21:00", close_next_day: false, label: null }],
  ["tue", { closed: true, open: null, close: null, close_next_day: false, label: "정기휴무" }],
  ["wed", { closed: false, open: "11:30", close: "21:00", close_next_day: false, label: null }],
  ["thu", { closed: false, open: "11:30", close: "21:00", close_next_day: false, label: null }],
  ["fri", { closed: false, open: "11:30", close: "21:00", close_next_day: false, label: null }],
  ["sat", { closed: false, open: "11:30", close: "21:00", close_next_day: false, label: null }],
  ["sun", { closed: false, open: "11:30", close: "21:00", close_next_day: false, label: null }],
])

test("nextCheckAt keeps the 08:00 pre-open check and opening check", () => {
  const beforeEight = nextCheckAt({ now: new Date("2026-08-25T22:00:00.000Z"), weekly })
  assert.equal(beforeEight.reason, "pre_open")
  assert.equal(beforeEight.at.toISOString(), "2026-08-25T23:00:00.000Z")

  const morning = nextCheckAt({ now: new Date("2026-08-26T00:00:00.000Z"), weekly })
  assert.equal(isPreOpenCheckWindow({ now: new Date("2026-08-25T23:00:00.000Z"), weekly }), true)
  assert.equal(isPreOpenCheckWindow({ now: new Date("2026-08-26T01:00:00.000Z"), weekly }), true)
  assert.equal(morning.reason, "opening_soon")
  assert.equal(morning.at.toISOString(), "2026-08-26T02:00:00.000Z")
})

test("nextCheckAt skips closed days until the next day's 08:00 pre-open check", () => {
  const result = nextCheckAt({ now: new Date("2026-08-25T03:00:00.000Z"), weekly })
  assert.equal(result.reason, "pre_open")
  assert.equal(result.at.toISOString(), "2026-08-25T23:00:00.000Z")
})

test("nextCheckAt schedules the break end and normal interval", () => {
  const duringBreak = nextCheckAt({
    now: new Date("2026-08-26T06:30:00.000Z"),
    weekly,
    breakTimes: { wed: [{ start: "15:00", end: "17:00" }] },
  })
  assert.equal(duringBreak.reason, "break")
  assert.equal(duringBreak.at.toISOString(), "2026-08-26T08:00:00.000Z")

  const open = nextCheckAt({ now: new Date("2026-08-26T03:00:00.000Z"), weekly })
  assert.equal(open.reason, "open")
  assert.equal(open.at.toISOString(), "2026-08-26T04:00:00.000Z")
})

const config = { supabaseUrl: "https://example.invalid", serviceRoleKey: "test-key" }
const shop = {
  id: "11111111-1111-4111-8111-111111111111",
  name: "Test shop",
  naver_place_url: "https://map.naver.com/p/entry/place/12345",
  business_hours_weekly: weekly,
  check_interval_minutes: 15,
  check_reason: "volatile",
  consecutive_failures: 2,
}

test("processShop stores observed status without the removed RPC and returns to the normal interval", async (t) => {
  t.mock.timers.enable({ apis: ["Date"], now: new Date("2026-08-26T03:00:00.000Z") })
  const statusLocator = {
    first() { return this },
    async waitFor() {},
    async allTextContents() { return ["오늘 휴무"] },
  }
  let browserClosed = false
  t.mock.method(chromium, "launch", async () => ({
    async newPage() {
      return {
        setDefaultTimeout() {},
        async goto() {},
        frameLocator() {
          return {
            locator(selector) {
              if (selector === "h1") return { first() { return { async textContent() { return "Test shop" } } } }
              return statusLocator
            },
          }
        },
        async close() {},
      }
    },
    async close() { browserClosed = true },
  }))
  const requests = []
  t.mock.method(globalThis, "fetch", async (url, options) => {
    requests.push({ url, method: options.method, body: JSON.parse(options.body) })
    return new Response("[]", { status: 200 })
  })

  const result = await processShop(config, shop)

  assert.equal(result.status, "오늘 휴무", result.error)
  assert.equal(browserClosed, true)
  assert.equal(requests.length, 1)
  assert.equal(requests[0].url, "https://example.invalid/rest/v1/shop_operating_status?on_conflict=shop_id")
  assert.equal(requests[0].method, "POST")
  assert.deepEqual(requests[0].body, {
    shop_id: shop.id,
    naver_place_id: "12345",
    status: "오늘 휴무",
    source_url: shop.naver_place_url,
    checked_at: "2026-08-26T03:00:00.000Z",
    last_error: null,
    next_check_at: "2026-08-26T04:00:00.000Z",
    check_interval_minutes: 60,
    check_reason: "open",
    consecutive_failures: 0,
  })
})

test("processShop retains retry scheduling when Naver collection fails", async (t) => {
  t.mock.timers.enable({ apis: ["Date"], now: new Date("2026-08-26T03:00:00.000Z") })
  t.mock.method(chromium, "launch", async () => { throw new Error("Naver unavailable") })
  const requests = []
  t.mock.method(globalThis, "fetch", async (url, options) => {
    requests.push({ url, body: JSON.parse(options.body) })
    return new Response("[]", { status: 200 })
  })

  const result = await processShop(config, { ...shop, consecutive_failures: 0 })

  assert.equal(result.status, null)
  assert.equal(requests.length, 1)
  assert.equal(requests[0].body.last_error, "Naver unavailable")
  assert.equal(requests[0].body.check_reason, "retry")
  assert.equal(requests[0].body.check_interval_minutes, 15)
  assert.equal(requests[0].body.next_check_at, "2026-08-26T03:15:00.000Z")
  assert.equal(requests[0].body.consecutive_failures, 1)
})
