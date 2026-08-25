import test from "node:test"
import assert from "node:assert/strict"
import { classifyAnomaly, extractNaverPlaceId, isPreOpenCheckWindow, nextCheckAt, normalizeStatus, parseLimit } from "./index.js"

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

test("classifyAnomaly ignores regular closures and detects unexpected closures", () => {
  assert.deepEqual(classifyAnomaly({
    status: "오늘 휴무",
    now: new Date("2026-08-26T00:00:00.000Z"),
    weekly,
  }), { type: "unexpected_close", minutesEarly: null })
  assert.equal(classifyAnomaly({
    status: "오늘 휴무",
    now: new Date("2026-08-26T00:00:00.000Z"),
    weekly: { ...weekly, wed: { ...weekly.wed, closed: true } },
  }), null)
})

test("classifyAnomaly detects early closing before the scheduled close", () => {
  assert.deepEqual(classifyAnomaly({
    status: "영업 종료",
    now: new Date("2026-08-26T09:00:00.000Z"),
    weekly,
  }), { type: "early_close", minutesEarly: 180 })
  assert.equal(classifyAnomaly({
    status: "영업 종료",
    now: new Date("2026-08-26T11:40:00.000Z"),
    weekly,
  }), null)
  assert.equal(classifyAnomaly({
    status: "영업 종료",
    now: new Date("2026-08-26T06:30:00.000Z"),
    weekly,
    breakTimes: { wed: [{ start: "15:00", end: "17:00" }] },
  }), null)
})

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
