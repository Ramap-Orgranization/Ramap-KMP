#!/usr/bin/env node
import fs from "node:fs";
import path from "node:path";
import { createHash } from "node:crypto";
import { execFileSync } from "node:child_process";
import { fileURLToPath } from "node:url";
import { batchSign } from "./batch-sign.mjs";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");
const [fixturePath, configPath, outputPath] = process.argv.slice(2);
if (!fixturePath || !configPath || !outputPath) throw new Error("Usage: controlled.mjs /private/fixture.json /private/config.json report.json");
if (fs.existsSync(outputPath)) throw new Error("Refusing to overwrite evidence");
const hash = value => createHash("sha256").update(value).digest("hex");
const config = JSON.parse(fs.readFileSync(configPath));
const fixtureText = fs.readFileSync(fixturePath, "utf8");
if (hash(fixtureText) !== config.fixtureSha256) throw new Error("Fixture differs from deployment allowlist");
if (Date.now() + 15 * 60_000 > config.expiresAt) throw new Error("Experiment token expires too soon");
const { reviews } = JSON.parse(fixtureText);
const occurrences = reviews.flatMap(r => [...r.images, ...(r.avatar ? [r.avatar] : [])]);
const images = [...new Map(occurrences.map(i => [JSON.stringify([i.bucket, i.path]), i])).values()];
if (images.length !== 22) throw new Error("Expected 22-image workload");
const properties = Object.fromEntries(fs.readFileSync(path.join(root,"local.properties"),"utf8").split("\n").filter(line => line.includes("=")).map(line => {
    const split = line.indexOf("="); return [line.slice(0, split).trim(), line.slice(split + 1).trim().replaceAll("\\:", ":")];
}));
const base = properties["supabase.url"].replace(/\/$/, "");
const anonKey = properties["supabase.anon_key"];
const role = JSON.parse(Buffer.from(anonKey.split(".")[1], "base64url")).role;
if (role !== "anon" || !/^https:\/\/[a-z]+\.supabase\.co$/.test(base)) throw new Error("Only the public anon caller is supported by this protocol");
const headers = { apikey: anonKey, Authorization: `Bearer ${anonKey}`, "Content-Type": "application/json", "x-benchmark-token": config.secret, "x-region": "ap-southeast-2" };
const endpoint = `${base}/functions/v1/${config.functionName}`;
const round = n => Math.round(n * 100) / 100;
const orders = [["direct_batch","edge_batch","redis_warm"],["edge_batch","redis_warm","direct_batch"],["redis_warm","direct_batch","edge_batch"],["redis_warm","edge_batch","direct_batch"],["edge_batch","direct_batch","redis_warm"],["direct_batch","redis_warm","edge_batch"]];
const report = {
    schemaVersion: 2, startedAt: new Date().toISOString(), completed: false,
    environment: { node: process.version, platform: process.platform, architecture: process.arch, timezone: Intl.DateTimeFormat().resolvedOptions().timeZone, authRole: role, projectFingerprint: hash(base), storageRegion: "ap-southeast-2", redisRegion: "ap-southeast-2", requestedEdgeRegion: "ap-southeast-2" },
    source: { head: execFileSync("git",["rev-parse","HEAD"],{cwd:root,encoding:"utf8"}).trim(), fixtureSha256:hash(fixtureText), originalServerHashes:config.originalHashes, experimentBundleSha256:config.bundleSha256,
        runnerSha256:hash(fs.readFileSync(fileURLToPath(import.meta.url))), batchHelperSha256:hash(fs.readFileSync(path.join(root,"tools/image-url-benchmark/batch-sign.mjs"))), functionName:config.functionName },
    workload: { reviews: reviews.length, occurrences: occurrences.length, uniqueImages: images.length, buckets: 2, uniqueAvatars: images.filter(i => i.bucket === "profile-avatars").length, uniqueReviewImages:images.filter(i => i.bucket === "shop-review-images").length },
    protocol: { mainRounds:30, coldSamples:6, mixedSamples:6, uncachedControlSamples:10, order:orders, delayMs:2000, storageRequestBudget:2500, lifetimeSeconds:300, cacheRefreshMarginSeconds:60, cacheConcurrency:8, batchConcurrency:2, pageConcurrency:1, retries:0, stopAfterConsecutiveFailedPages:3,
        notes:["Same fixture, anonymous RLS caller, TTL, client machine, Node fetch and no image downloads", "Direct and Edge use the identical batch helper", "Batch needs two bucket requests, cached implementation uses per-image checks; intrinsic architectural differences remain", "Same protected experimental Edge Function serves batch, cached and uncached controls", "Private cache namespace; independent cold/mixed slots; prime warm cache every ten rounds, followed by one recorded readiness request", "Cold means signed URL cache cold, not cold database buffers or cold Edge runtime", "All failures and preparation samples are retained; HTTP200 partial failures are errors", "Protocol revised after two retained preparation failures: no sample retries; stop on three consecutive failed pages, Redis bypass or failed readiness", "Main percentiles include error responses, with failed-page and image-error counts separately; cache state mismatches are recorded", "API latency only, not UI, image transfer or physical database IO", "Redis REST operations are not verified billable command units"] },
    preparation: [], samples: [],
};
let usedStorageRequests = 0;
let slot = 20; // Slots 0–19 belong to the retained aborted preparation runs.
let consecutiveFailedPages = 0;
function persist() { fs.writeFileSync(outputPath,JSON.stringify(report,null,2)+"\n"); }
persist();
async function sample(variant, iteration, selected = images, cacheSlot = 0, preparation = false) {
    if (usedStorageRequests + 44 > report.protocol.storageRequestBudget) throw new Error("Storage request budget exhausted");
    await new Promise(resolve => setTimeout(resolve, report.protocol.delayMs));
    const start = performance.now();
    const result = {variant, round:iteration, startedAt:new Date().toISOString(), imageCount:selected.length, status:0, errors:0};
    try {
        let body;
        if (variant === "direct_batch") {
            const response = await batchSign(selected,base,anonKey,headers.Authorization);
            body = response; result.server = response.metrics; result.status = 200;
            result.clientHttpRequests = response.metrics.operations.sign.requests;
        } else {
            const mode = variant === "edge_batch" ? "batch" : variant === "edge_uncached_control" ? "uncached" : "cached";
            const response = await fetch(endpoint,{method:"POST",headers,body:JSON.stringify({mode,slot:cacheSlot,images:selected}),signal:AbortSignal.timeout(15_000)});
            result.status = response.status; result.clientHttpRequests = 1;
            result.edgeRegion = response.headers.get("x-sb-edge-region");
            if (result.edgeRegion !== "ap-southeast-2") throw new Error("Unexpected Edge execution region");
            result.serverWallMs = round(Number(response.headers.get("x-benchmark-server-ms")));
            const metrics = response.headers.get("x-image-metrics");
            result.server = metrics ? JSON.parse(metrics) : null;
            body = await response.json();
        }
        const resolved = body.images;
        result.errors = selected.filter(image => {
            const matches = Array.isArray(resolved) ? resolved.filter(value => value.bucket === image.bucket && value.path === image.path) : [];
            const value = matches.length === 1 ? matches[0] : null;
            if (!value || value.error || typeof value.url !== "string" || value.expiresAt <= Date.now() + 60_000) return true;
            const url = new URL(value.url);
            return url.origin !== base || !url.searchParams.get("token");
        }).length;
        if (!result.server) throw new Error("Missing counters");
    } catch {
        result.errors = selected.length; result.transportOrProtocolFailure = true;
    }
    result.wallMs = round(performance.now() - start);
    const operations = result.server?.operations;
    result.storageHttpRequests = (operations?.authorize.requests ?? 0) + (operations?.sign.requests ?? 0);
    usedStorageRequests += result.storageHttpRequests;
    (preparation ? report.preparation : report.samples).push(result);
    report.storageHttpRequests = usedStorageRequests;
    persist();
    console.log(JSON.stringify({variant,round:iteration,wallMs:result.wallMs,errors:result.errors,hits:result.server?.cacheHits,issued:result.server?.issued,storage:result.storageHttpRequests}));
    consecutiveFailedPages = result.errors ? consecutiveFailedPages + 1 : 0;
    if (consecutiveFailedPages >= 3 || (variant.startsWith("redis") && result.server?.cacheBypasses)) throw new Error("Failure stop threshold; evidence retained");
    const expectedHits = variant === "redis_warm" ? selected.length : variant === "redis_mixed" ? 11 : variant === "redis_cold" ? 0 : null;
    if (expectedHits !== null && result.server?.cacheHits !== expectedHits) { result.cacheStateMismatch = true; persist(); }
    return result;
}
try {
    // Verify the custom guard without valid benchmark secret. Does not reach Storage.
    const denied = await fetch(endpoint,{method:"POST",headers:{...headers,"x-benchmark-token":"invalid"},body:"{}",signal:AbortSignal.timeout(15000)});
    await denied.arrayBuffer(); report.preparation.push({variant:"unauthorized_guard",status:denied.status}); persist();
    if (denied.status !== 403) throw new Error("Missing experiment guard");
    await sample("direct_batch",0,images,0,true);
    await sample("edge_batch",0,images,0,true);
    await sample("edge_uncached_control",0,images,0,true);
    let warmSlot;
    for (let iteration = 1; iteration <= 30; iteration++) {
        if ((iteration-1) % 10 === 0) {
            warmSlot = ++slot; await sample("redis_cold",iteration,images,warmSlot);
            const ready = await sample("redis_readiness",iteration,images,warmSlot,true);
            if (ready.errors) throw new Error("Warm cache readiness failed");
        }
        for (const variant of orders[(iteration-1)%orders.length]) await sample(variant,iteration,images,warmSlot);
        if (iteration % 3 === 0) await sample("edge_uncached_control",iteration);
        if (iteration % 10 === 0) await sample("redis_cold",iteration,images,++slot);
        if (iteration % 5 === 0) {
            const mixedSlot = ++slot;
            await sample("redis_prefill",iteration,images.slice(0,11),mixedSlot,true);
            await sample("redis_mixed",iteration,images,mixedSlot);
        }
    }
    report.completed = true;
} catch {
    report.failure = "Stopped on consecutive errors, Redis bypass, failed readiness or budget limit; no failed samples were removed";
    process.exitCode = 1;
} finally {
    report.finishedAt = new Date().toISOString();
    report.summary = Object.fromEntries([...new Set(report.samples.map(s=>s.variant))].map(variant=>{
        const samples = report.samples.filter(s=>s.variant===variant);
        const sorted = samples.map(s=>s.wallMs).sort((a,b)=>a-b);
        const percentile = p => {const position=(sorted.length-1)*p; const lower=Math.floor(position); return round(sorted[lower]+(sorted[Math.ceil(position)]-sorted[lower])*(position-lower));};
        return [variant,{n:samples.length,errors:samples.reduce((sum,s)=>sum+s.errors,0),failedPages:samples.filter(s=>s.errors).length,cacheStateMismatches:samples.filter(s=>s.cacheStateMismatch).length,p50Ms:percentile(.5),p95Ms:percentile(.95),meanMs:round(sorted.reduce((a,b)=>a+b,0)/sorted.length),minMs:sorted[0],maxMs:sorted.at(-1)}];
    }));
    persist(); console.log(JSON.stringify({completed:report.completed,storageHttpRequests:usedStorageRequests,summary:report.summary}));
}
