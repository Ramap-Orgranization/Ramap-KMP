#!/usr/bin/env node
import fs from "node:fs";
const report = JSON.parse(fs.readFileSync(process.argv[2]));
if (!report.completed) throw new Error("Incomplete run; retain partial evidence");
const round = n => Math.round(n * 1000) / 1000;
const samples = report.samples.filter(sample => sample.round > 0);
const variants = ["native_batch", "redis_warm"];
const summary = Object.fromEntries(variants.map(variant => {
    const selected = samples.filter(sample => sample.variant === variant);
    const sorted = selected.map(sample => sample.wallMs).sort((a, b) => a - b);
    const percentile = p => {
        const position = (sorted.length - 1) * p;
        const low = Math.floor(position);
        return round(sorted[low] + (sorted[Math.ceil(position)] - sorted[low]) * (position - low));
    };
    return [variant, { n: selected.length, p50Ms: percentile(.5), p95Ms: percentile(.95),
        meanMs: round(sorted.reduce((a,b) => a+b,0)/sorted.length), errors: selected.reduce((sum,sample) => sum+sample.errors,0) }];
}));
const direct = samples.filter(s => s.variant === "native_batch");
const redis = samples.filter(s => s.variant === "redis_warm");
console.log(JSON.stringify({ ...summary,
    reductionPercent: round((1-summary.native_batch.p50Ms/summary.redis_warm.p50Ms)*100),
    p95ReductionPercent: round((1-summary.native_batch.p95Ms/summary.redis_warm.p95Ms)*100),
    directFasterPairs: direct.filter(a => a.wallMs < redis.find(b => b.round === a.round).wallMs).length,
    startedAt:report.startedAt, endedAt:report.updatedAt,
    storageHttpAttempts:report.samples.reduce((sum,s)=>sum+s.storageHttpRequests+(s.server?s.server.operations.authorize.requests+s.server.operations.sign.requests:0),0),
    redisRestRequests:report.samples.reduce((sum,s)=>sum+(s.server?.operations.redis.requests??0),0),
    preparation:report.samples.filter(s=>s.round===0).map(s=>({variant:s.variant,wallMs:s.wallMs,issued:s.server?.issued??22})),
},null,2));
