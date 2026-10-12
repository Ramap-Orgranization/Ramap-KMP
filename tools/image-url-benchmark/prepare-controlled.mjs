#!/usr/bin/env node
import fs from "node:fs";
import path from "node:path";
import { createHash, randomBytes, randomUUID } from "node:crypto";
import { fileURLToPath } from "node:url";

// Produces a PRIVATE deployment bundle; never commit its entrypoint or config.
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");
const [fixturePath, outputDirectory] = process.argv.slice(2);
if (!fixturePath || !outputDirectory) throw new Error("Usage: prepare-controlled.mjs /private/fixture.json /private/new-directory");
fs.mkdirSync(outputDirectory, { mode: 0o700 }); // Refuse overwriting an earlier experiment.
const hash = value => createHash("sha256").update(value).digest("hex");
const fixtureText = fs.readFileSync(fixturePath, "utf8");
const { reviews } = JSON.parse(fixtureText);
const images = [...new Map(reviews.flatMap(r => [...r.images, ...(r.avatar ? [r.avatar] : [])]).map(i => [JSON.stringify([i.bucket, i.path]), i])).values()];
if (images.length !== 22) throw new Error("This protocol expects the reviewed 22-image fixture");
const secret = randomBytes(32).toString("hex");
const namespace = `ramap:image-url-benchmark:${randomUUID()}`;
const expiresAt = Date.now() + 2 * 60 * 60 * 1000;
const functionName = `image-url-benchmark-${randomBytes(4).toString("hex")}`;
const files = [];
const originalHashes = {};
function replaceExactly(source, oldText, newText) {
    if (source.split(oldText).length !== 2) throw new Error("Production source changed; review isolation patch");
    return source.replace(oldText, newText);
}
for (const name of ["handler.ts", "storage.ts", "redis.ts", "metrics.ts"]) {
    let content = fs.readFileSync(path.join(root, "tools/image-url-benchmark/archived-functions/image-signed-urls", name), "utf8");
    originalHashes[name] = hash(content);
    if (name === "handler.ts") {
        content = replaceExactly(content, "redisToken?: string }", "redisToken?: string; cacheNamespace: string }");
        content = replaceExactly(content, "authorization, counts);", "authorization, counts, configuration.cacheNamespace);");
    }
    if (name === "storage.ts") {
        content = replaceExactly(content, "private readonly metrics: ImageRequestMetrics)", "private readonly metrics: ImageRequestMetrics, private readonly namespace: string)");
        content = replaceExactly(content, "`ramap:signed-image-url:v1:{${hash}}`", "`${this.namespace}:{${hash}}`");
    }
    files.push({ name, content });
}
files.push({ name: "batch-sign.mjs", content: fs.readFileSync(path.join(root, "tools/image-url-benchmark/batch-sign.mjs"), "utf8") });
const allowlist = images.map(i => hash(JSON.stringify([i.bucket, i.path])));
files.push({ name: "index.ts", content: `import { handleImageUrls } from "./handler.ts";
import { batchSign } from "./batch-sign.mjs";
const allowed = new Set(${JSON.stringify(allowlist)});
const hash = async (text: string) => Array.from(new Uint8Array(await crypto.subtle.digest("SHA-256", new TextEncoder().encode(text))), n => n.toString(16).padStart(2,"0")).join("");
Deno.serve(async (request: Request) => {
    const start = performance.now();
    if (request.method !== "POST" || Date.now() > ${expiresAt} || await hash(request.headers.get("x-benchmark-token") ?? "") !== ${JSON.stringify(hash(secret))}) return new Response(null, {status:403});
    const text = await request.text();
    if (text.length > 20_000) return new Response(null, {status:413});
    let input;
    try { input = JSON.parse(text); } catch { return new Response(null, {status:400}); }
    if (!Array.isArray(input.images) || !input.images.length || input.images.length > 22 || !["batch","cached","uncached"].includes(input.mode) || !Number.isInteger(input.slot) || input.slot < 0 || input.slot > 100) return new Response(null, {status:400});
    for (const image of input.images) if (!allowed.has(await hash(JSON.stringify([image.bucket,image.path])))) return new Response(null,{status:403});
    const authorization = request.headers.get("authorization") ?? "";
    const config = { supabaseUrl: Deno.env.get("SUPABASE_URL")!, anonKey: Deno.env.get("SUPABASE_ANON_KEY")!, cacheNamespace: ${JSON.stringify(namespace)} + ":" + input.slot,
        ...(input.mode === "cached" ? {redisUrl:Deno.env.get("UPSTASH_REDIS_REST_URL"),redisToken:Deno.env.get("UPSTASH_REDIS_REST_TOKEN")} : {}) };
    let response;
    if (input.mode === "batch") {
        const result = await batchSign(input.images, config.supabaseUrl, config.anonKey, authorization);
        const metrics = {...result.metrics,requestId:crypto.randomUUID()};
        console.info(JSON.stringify({event:"image_url_batch_benchmark",...metrics}));
        response = new Response(JSON.stringify({images:result.images}),{headers:{"Content-Type":"application/json","X-Image-Metrics":JSON.stringify(metrics)}});
    } else response = await handleImageUrls(new Request(request.url,{method:"POST",headers:request.headers,body:JSON.stringify({images:input.images})}), config);
    response.headers.set("X-Benchmark-Server-Ms", String(performance.now()-start));
    response.headers.set("Cache-Control","no-store");
    return response;
});
` });
const metadata = { functionName, expiresAt, namespace, fixtureSha256: hash(fixtureText), originalHashes,
    bundleSha256: hash(JSON.stringify(files)), generatedAt: new Date().toISOString() };
fs.writeFileSync(path.join(outputDirectory, "bundle.json"), JSON.stringify(files), { mode: 0o600 });
fs.writeFileSync(path.join(outputDirectory, "config.json"), JSON.stringify({ ...metadata, secret }), { mode: 0o600 });
fs.writeFileSync(path.join(outputDirectory, "metadata.json"), JSON.stringify(metadata, null, 2), { mode: 0o600 });
for (const file of files) fs.writeFileSync(path.join(outputDirectory, file.name), file.content, { mode: 0o600 });
console.log(JSON.stringify({ functionName, expiresAt, imageCount: images.length, bundleSha256: metadata.bundleSha256 }));
