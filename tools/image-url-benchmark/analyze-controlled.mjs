#!/usr/bin/env node
import fs from "node:fs";
const report=JSON.parse(fs.readFileSync(process.argv[2]));
if (!report.completed) throw new Error("Comparison did not complete; inspect retained partial report");
const mean=values=>values.reduce((a,b)=>a+b,0)/values.length;
const percentile=(values,p)=>{const sorted=[...values].sort((a,b)=>a-b);const position=(sorted.length-1)*p;const low=Math.floor(position);return sorted[low]+(sorted[Math.ceil(position)]-sorted[low])*(position-low)};
const round=value=>Math.round(value*100)/100;
let seed=20261012;
function random(){seed=(1664525*seed+1013904223)>>>0;return seed/4294967296}
function paired(a,b){
    const first=report.samples.filter(s=>s.variant===a);
    const pairs=first.map(left=>[left,report.samples.find(s=>s.variant===b&&s.round===left.round)]).filter(([,right])=>right);
    const differences=pairs.map(([left,right])=>right.wallMs-left.wallMs);
    const bootstrap=Array.from({length:10000},()=>mean(differences.map(()=>differences[Math.floor(random()*differences.length)])));
    return {first:a,second:b,n:pairs.length,firstFaster:pairs.filter(([a,b])=>a.wallMs<b.wallMs).length,
        pairsWithErrors:pairs.filter(([a,b])=>a.errors||b.errors).length,
        meanSecondMinusFirstMs:round(mean(differences)),medianSecondMinusFirstMs:round(percentile(differences,.5)),
        bootstrapMeanDifference95PercentIntervalMs:[round(percentile(bootstrap,.025)),round(percentile(bootstrap,.975))]};
}
const all=[...report.preparation,...report.samples].filter(s=>s.server);
const operationCounts=Object.fromEntries(["authorize","sign","redis"].map(name=>[name,all.reduce((sum,s)=>sum+(s.server.operations[name].requests??0),0)]));
console.log(JSON.stringify({
    startedAt:report.startedAt,finishedAt:report.finishedAt,summary:report.summary,
    paired:[paired("direct_batch","edge_batch"),paired("direct_batch","redis_warm"),paired("edge_batch","redis_warm"),paired("redis_warm","edge_uncached_control")],
    counts:{measuredPages:report.samples.length,preparationPages:all.length-report.samples.length,edgeInvocations:all.filter(s=>s.variant!=="direct_batch").length+1,
        operationCounts,issued:all.reduce((sum,s)=>sum+s.server.issued,0),cacheHits:all.reduce((sum,s)=>sum+s.server.cacheHits,0),imageErrors:all.reduce((sum,s)=>sum+s.errors,0)},
    perVariant:Object.fromEntries(Object.keys(report.summary).map(variant=>{const samples=report.samples.filter(s=>s.variant===variant);return [variant,{
        meanIssued:mean(samples.map(s=>s.server.issued)),meanCacheHits:mean(samples.map(s=>s.server.cacheHits)),
        meanStorageHttpRequests:mean(samples.map(s=>s.storageHttpRequests)),meanRedisRestRequests:mean(samples.map(s=>s.server.operations.redis.requests)),
        instrumentedWorkP50Ms:round(percentile(samples.map(s=>s.server.totalMs),.5)),timingScope:variant==="direct_batch"?"client batch helper":"Edge handler",regions:[...new Set(samples.map(s=>s.edgeRegion).filter(Boolean))]
    }]})),
    caveat:"Paired bootstrap resamples sequential rounds from one run; it does not cover other clients, times, workloads or traffic. Response percentiles include failures if any."
},null,2));
