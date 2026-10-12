#!/usr/bin/env node
import { createHash } from "node:crypto";
import { execFileSync } from "node:child_process";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

// Runs real, bounded requests. It never downloads images, changes RLS, or clears Redis.
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");
const options = parseOptions(process.argv.slice(2));
if (!options.fixture || !options.output) {
    console.log("node tools/image-url-benchmark/benchmark.mjs --fixture /private/fixture.json --output report.json [--rounds 6] [--delay-ms 4000] [--max-storage-requests 600]");
    process.exit(0);
}
const rounds = integer(options.rounds ?? 6, 1, 30);
const delayMs = integer(options["delay-ms"] ?? 4000, 1000, 60_000);
const requestBudget = integer(options["max-storage-requests"] ?? 600, 1, 5000);
const properties = readProperties(path.join(root, "local.properties"));
const base = (process.env.SUPABASE_URL ?? properties["supabase.url"])?.replace(/\/$/, "");
const anonKey = process.env.SUPABASE_ANON_KEY ?? properties["supabase.anon_key"];
const bearer = process.env.BENCHMARK_ACCESS_TOKEN ?? anonKey;
if (!base || !anonKey || !bearer) throw new Error("Missing Supabase client configuration");
const baseUrl = new URL(base);
if (baseUrl.protocol !== "https:" || !baseUrl.hostname.endsWith(".supabase.co")) throw new Error("Expected a Supabase HTTPS project URL");
for (const token of [anonKey, bearer]) {
    if (token.startsWith("sb_secret_") || jwtRole(token) === "service_role") throw new Error("Privileged credentials are not allowed in this benchmark");
}
const fixtureText = fs.readFileSync(options.fixture, "utf8");
const { reviews } = JSON.parse(fixtureText);
if (!Array.isArray(reviews) || reviews.length < 1 || reviews.length > 20) throw new Error("Expected 1–20 review fixtures");
const occurrences = reviews.flatMap(review => {
    if (!Array.isArray(review.images)) throw new Error("Invalid fixture images");
    if (review.avatar && review.avatar.bucket !== "profile-avatars") throw new Error("Invalid avatar bucket");
    if (review.images.some(image => image.bucket !== "shop-review-images")) throw new Error("Invalid review bucket");
    return [...review.images, ...(review.avatar ? [review.avatar] : [])];
});
const unique = [...new Map(occurrences.map(image => {
    if (!/^[0-9a-f-]{36}\/[0-9a-f-]{36}\.(jpg|png)$/i.test(image.path)) throw new Error("Invalid fixture path");
    return [identity(image), image];
})).values()];
if (unique.length < 1 || unique.length > 64) throw new Error("Expected 1–64 distinct images");
// Includes warmup GETs, one cold fill and every measured direct sign / Edge authorization.
const maximumExpectedStorageRequests = 2 + 2 * unique.length + rounds * (occurrences.length + 2 * unique.length);
if (maximumExpectedStorageRequests > requestBudget) throw new Error(`Expected ${maximumExpectedStorageRequests} Storage requests exceed the configured ${requestBudget} request budget`);
const headers = { apikey: anonKey, Authorization: `Bearer ${bearer}`, "Content-Type": "application/json" };
const report = {
    schemaVersion: 1,
    startedAt: new Date().toISOString(),
    environment: { node: process.version, platform: process.platform, architecture: process.arch, timezone: Intl.DateTimeFormat().resolvedOptions().timeZone, projectFingerprint: hash(base), functionRegion: "ap-southeast-2", authRole: jwtRole(bearer) ?? "unknown" },
    source: { baselineCommit: execFileSync("git", ["rev-parse", options["baseline-ref"] ?? "33a0d49c2731e42144f2c79704748d7c7e0cf68b"], { cwd: root, encoding: "utf8" }).trim(), serverSourceSha256: serverSourceHash(), benchmarkSha256: hash(fs.readFileSync(fileURLToPath(import.meta.url))), fixtureSha256: hash(fixtureText) },
    workload: { reviews: reviews.length, occurrences: occurrences.length, uniqueImages: unique.length, avatarOccurrences: occurrences.filter(x => x.bucket === "profile-avatars").length, reviewImageOccurrences: occurrences.filter(x => x.bucket === "shop-review-images").length, uniqueAvatars: unique.filter(x => x.bucket === "profile-avatars").length, uniqueReviewImages: unique.filter(x => x.bucket === "shop-review-images").length },
    protocol: { rounds, delayMs, maximumExpectedStorageRequests, requestBudget, signLifetimeSeconds: 300, imageConcurrency: 4, order: ["legacy,dedup,redis", "dedup,redis,legacy", "redis,legacy,dedup", "redis,dedup,legacy", "dedup,legacy,redis", "legacy,redis,dedup"], retries: 0, notes: ["HTTP link resolution only; no image download, rendering, app SDK, or device timing", "Legacy review photos have concurrency 4; each review avatar follows its photos, with no avatar semaphore", "Dedup uses the same flow, sharing promises only within one page; no cross-page cache", "Redis receives the same distinct images as the new app, executes in Sydney and checks caller RLS on every hit", "Operation durationSumMs adds overlapping request durations; it is not elapsed page time", "Redis requests count REST commands including EVAL, not billing units or internal Lua operations", "Small production pilot; not a load test or evidence of a Disk IO decrease"] },
    preparation: [], samples: [], completed: false,
};
fs.mkdirSync(path.dirname(path.resolve(options.output)), { recursive: true });
persist();
try {
    // Establish a connection without issuing links. Timings remain in the raw report.
    for (let index = 0; index < 2; index++) {
        const start = performance.now();
        const response = await fetch(`${base}/storage/v1/object/info/${objectPath(unique[0])}`, { headers, signal: AbortSignal.timeout(15_000) });
        await response.arrayBuffer();
        report.preparation.push({ kind: "transport-warmup", status: response.status, wallMs: round(performance.now() - start) });
        if (!response.ok) throw new Error("Fixture is not readable by this caller");
    }
    const initial = await edgeSample("initial", 0);
    report.preparation.push(initial);
    persist();
    if (initial.errors || initial.server.cacheBypasses) throw new Error("Initial cache fill failed");
    // Rotate the order so time-of-run differences are not all assigned to one variant.
    for (let iteration = 0; iteration < rounds; iteration++) {
        const order = report.protocol.order[iteration % report.protocol.order.length].split(",");
        for (const variant of order) {
            await new Promise(resolve => setTimeout(resolve, delayMs));
            const sample = variant === "redis" ? await edgeSample(variant, iteration + 1) : await directSample(variant, iteration + 1);
            report.samples.push(sample);
            persist();
            console.log(JSON.stringify({ variant, round: iteration + 1, wallMs: sample.wallMs, signRequests: sample.signRequests, cacheHits: sample.server?.cacheHits, errors: sample.errors }));
            if (sample.errors || sample.server?.cacheBypasses) throw new Error("Stopped after a failed sample or cache bypass; keep the partial report");
        }
    }
    report.completed = true;
} catch (error) {
    // Do not log fetch error causes, request bodies, signed URLs or tokens.
    report.failure = { type: error.name, reason: "Benchmark interrupted; inspect safe sample statuses and metrics" };
    process.exitCode = 1;
} finally {
    report.finishedAt = new Date().toISOString();
    report.summary = Object.fromEntries(["legacy", "dedup", "redis"].map(variant => [variant, summarize(report.samples.filter(x => x.variant === variant))]));
    persist();
    console.log(JSON.stringify({ completed: report.completed, summary: report.summary }));
}

async function directSample(variant, iteration) {
    const start = performance.now();
    const sample = { variant, round: iteration, startedAt: new Date().toISOString(), clientHttpRequests: 0, signRequests: 0, signSuccesses: 0, errors: 0, statusCounts: {}, requestDurationSumMs: 0 };
    const permits = semaphore(4);
    const pending = new Map();
    function sign(image, isReviewImage) {
        const key = identity(image);
        if (variant === "dedup" && pending.has(key)) return pending.get(key);
        const promise = isReviewImage ? permits(() => send(image)) : send(image);
        pending.set(key, promise);
        return promise;
    }
    async function send(image) {
        const requestStart = performance.now();
        sample.clientHttpRequests++;
        sample.signRequests++;
        try {
            const response = await fetch(`${base}/storage/v1/object/sign/${objectPath(image)}`, { method: "POST", headers, body: JSON.stringify({ expiresIn: 300 }), signal: AbortSignal.timeout(15_000) });
            sample.statusCounts[response.status] = (sample.statusCounts[response.status] ?? 0) + 1;
            const body = await response.json();
            if (!response.ok || typeof body.signedURL !== "string" || !body.signedURL.startsWith(`/object/sign/${objectPath(image)}?`)) sample.errors++;
            else sample.signSuccesses++;
        } catch {
            sample.errors++;
        } finally {
            sample.requestDurationSumMs += performance.now() - requestStart;
        }
    }
    await Promise.all(reviews.map(async review => {
        await Promise.all(review.images.map(image => sign(image, true)));
        if (review.avatar) await sign(review.avatar, false);
    }));
    sample.wallMs = round(performance.now() - start);
    sample.requestDurationSumMs = round(sample.requestDurationSumMs);
    return sample;
}

async function edgeSample(variant, iteration) {
    const start = performance.now();
    const startedAt = new Date().toISOString();
    const response = await fetch(`${base}/functions/v1/image-signed-urls`, { method: "POST", headers: { ...headers, "x-region": "ap-southeast-2" }, body: JSON.stringify({ images: unique }), signal: AbortSignal.timeout(30_000) });
    const body = await response.json();
    const metrics = JSON.parse(response.headers.get("x-image-metrics") ?? "null");
    if (metrics?.schemaVersion !== 1 || !Array.isArray(body.images)) throw new Error("Expected an instrumented image-signed-urls deployment");
    const returned = new Map(body.images.map(image => [identity(image), image]));
    const errors = unique.filter(image => {
        const value = returned.get(identity(image));
        if (!value?.url || value.error || !Number.isSafeInteger(value.expiresAt)) return true;
        const url = new URL(value.url);
        return url.origin !== baseUrl.origin || url.pathname !== `/storage/v1/object/sign/${objectPath(image)}` || !url.searchParams.get("token");
    }).length;
    return { variant, round: iteration, startedAt, wallMs: round(performance.now() - start), clientHttpRequests: 1, signRequests: metrics.operations.sign.requests, signSuccesses: metrics.issued, errors, statusCounts: { [response.status]: 1 }, server: metrics };
}

function summarize(samples) {
    if (!samples.length) return { samples: 0 };
    const sorted = samples.map(x => x.wallMs).sort((a, b) => a - b);
    const middle = Math.floor(sorted.length / 2);
    const sum = key => samples.reduce((total, sample) => total + sample[key], 0);
    return { samples: samples.length, p50Ms: round(sorted.length % 2 ? sorted[middle] : (sorted[middle - 1] + sorted[middle]) / 2), p95Ms: sorted[Math.ceil(sorted.length * 0.95) - 1], minMs: sorted[0], maxMs: sorted.at(-1), clientHttpRequests: sum("clientHttpRequests"), signRequests: sum("signRequests"), signSuccesses: sum("signSuccesses"), errors: sum("errors"), cacheHits: samples.reduce((total, sample) => total + (sample.server?.cacheHits ?? 0), 0), authorizeRequests: samples.reduce((total, sample) => total + (sample.server?.operations.authorize.requests ?? 0), 0), redisRestRequests: samples.reduce((total, sample) => total + (sample.server?.operations.redis.requests ?? 0), 0) };
}
function persist() { fs.writeFileSync(options.output, JSON.stringify(report, null, 2) + "\n"); }
function identity(image) { return `${image.bucket}/${image.path}`; }
function objectPath(image) { return [image.bucket, ...image.path.split("/")].map(encodeURIComponent).join("/"); }
function hash(value) { return createHash("sha256").update(value).digest("hex"); }
function round(value) { return Math.round(value * 100) / 100; }
function integer(value, min, max) { const number = Number(value); if (!Number.isInteger(number) || number < min || number > max) throw new Error(`Expected integer in ${min}–${max}`); return number; }
function jwtRole(value) { try { return JSON.parse(Buffer.from(value.split(".")[1], "base64url")).role; } catch { return null; } }
function parseOptions(args) { const result = {}; for (let i = 0; i < args.length; i += 2) { if (!args[i].startsWith("--") || !args[i + 1]) throw new Error("Expected --option value pairs"); result[args[i].slice(2)] = args[i + 1]; } return result; }
function readProperties(file) {
    if (!fs.existsSync(file)) return {};
    return Object.fromEntries(fs.readFileSync(file, "utf8").split(/\r?\n/).filter(line => !line.startsWith("#") && line.includes("=")).map(line => { const separator = line.indexOf("="); return [line.slice(0, separator).trim(), line.slice(separator + 1).trim().replace(/\\:/g, ":")]; }));
}
function serverSourceHash() {
    return hash(["handler.ts", "index.ts", "metrics.ts", "redis.ts", "storage.ts"].map(name => `${name}\n${fs.readFileSync(path.join(root, "tools/image-url-benchmark/archived-functions/image-signed-urls", name), "utf8")}`).join("\n"));
}
function semaphore(capacity) {
    let available = capacity;
    const queue = [];
    return async action => {
        if (available > 0) available--; else await new Promise(resolve => queue.push(resolve));
        try { return await action(); }
        finally { if (queue.length) queue.shift()(); else available++; }
    };
}
