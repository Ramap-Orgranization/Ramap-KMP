import assert from "node:assert/strict";
import test from "node:test";
import { handleImageUrls } from "./handler.ts";
import { ImageRequestMetrics } from "./metrics.ts";
import { RedisImageCache } from "./redis.ts";
import { StorageImageClient } from "./storage.ts";

const origin = "https://test.supabase.co";
const configuration = {
    supabaseUrl: origin, anonKey: "test-anon-key",
    redisUrl: "https://test.upstash.io", redisToken: "test-redis-token",
};
const image = {
    bucket: "profile-avatars",
    path: "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa/bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb.jpg",
};

// Network boundary fake shared by independent handler invocations, including Redis leases/TTL.
function fixture(t) {
    const values = new Map();
    const state = { allow: true, version: "v1", redisFailure: false, signFailure: false, signs: 0, signDelayMs: 0, authorizations: [], logs: [] };
    const find = (key) => {
        const entry = values.get(key);
        if (!entry || entry.expiresAt <= Date.now()) { values.delete(key); return null; }
        return entry.value;
    };
    const save = (key, value, ttlMs) => values.set(key, { value, expiresAt: Date.now() + ttlMs });
    t.mock.method(console, "info", (message) => state.logs.push(JSON.parse(message)));
    t.mock.method(globalThis, "fetch", async (input, options) => {
        const url = new URL(input);
        if (url.hostname === "test.upstash.io") {
            if (state.redisFailure) return new Response("unavailable", { status: 503 });
            assert.equal(options.headers.Authorization, "Bearer test-redis-token");
            const command = JSON.parse(options.body);
            let result;
            switch (command[0]) {
                case "GET": result = find(command[1]); break;
                case "SET":
                    result = find(command[1]) === null ? "OK" : null;
                    if (result) save(command[1], command[2], command[5] * 1000);
                    break;
                case "EVAL": {
                    const count = command[2];
                    const keys = command.slice(3, 3 + count);
                    const args = command.slice(3 + count);
                    result = find(keys[0]) === args[0] ? 1 : 0;
                    if (result) {
                        if (count === 2) save(keys[1], args[1], args[2]);
                        values.delete(keys[0]);
                    }
                    break;
                }
                default: assert.fail("Unexpected Redis command");
            }
            return Response.json({ result });
        }
        assert.equal(url.origin, origin, "No real network calls are allowed");
        assert.equal(options.headers.apikey, "test-anon-key");
        if (url.pathname.startsWith("/storage/v1/object/info/")) {
            state.authorizations.push(options.headers.Authorization);
            return state.allow
                ? Response.json({ id: "object-id", version: state.version, last_modified: "2026-10-11T00:00:00Z" })
                : new Response("denied", { status: 403 });
        }
        assert.ok(url.pathname.startsWith("/storage/v1/object/sign/"));
        assert.equal(JSON.parse(options.body).expiresIn, 300);
        state.signs++;
        if (state.signDelayMs) await new Promise((resolve) => setTimeout(resolve, state.signDelayMs));
        return state.signFailure ? new Response("failed", { status: 500 })
            : Response.json({ signedURL: `${url.pathname.slice("/storage/v1".length)}?token=test-${state.signs}` });
    });
    async function request(images = [image], authorization = "Bearer caller-a") {
        const response = await handleImageUrls(new Request(`${origin}/functions/v1/image-signed-urls`, {
            method: "POST", headers: { Authorization: authorization }, body: JSON.stringify({ images }),
        }), configuration);
        return { status: response.status, body: await response.json(), metrics: JSON.parse(response.headers.get("x-image-metrics")) };
    }
    return { state, values, find, save, request };
}

test("deduplicates a batch by bucket/path and reuses both image types with no signing on warm hits", async (t) => {
    const f = fixture(t);
    const images = [...Array(20).fill(image), { ...image, bucket: "shop-review-images" }];
    const first = await f.request(images);
    assert.equal(first.metrics.requested, 21);
    assert.equal(first.metrics.unique, 2);
    assert.equal(first.metrics.issued, 2);
    assert.equal(first.metrics.operations.sign.requests, 2);
    assert.equal(first.metrics.operations.redis.requests, 8, "No redundant release after successful atomic save");
    const next = await f.request(images);
    assert.equal(next.metrics.cacheHits, 2);
    assert.equal(next.metrics.operations.authorize.requests, 2);
    assert.equal(next.metrics.operations.sign.requests, 0);
    assert.equal(next.metrics.operations.redis.requests, 2);
    assert.deepEqual(next.body, first.body);
    assert.equal(f.state.signs, 2);
    for (const sensitive of [image.path, origin, "test-anon-key", "test-redis-token", "caller-a", "test-1"]) {
        assert.ok(!JSON.stringify([next.metrics, f.state.logs]).includes(sensitive));
    }
});

test("shares links across callers only after checking each caller's current permission", async (t) => {
    const f = fixture(t);
    const first = await f.request();
    const second = await f.request([image], "Bearer caller-b");
    assert.deepEqual(second.body, first.body);
    assert.equal(second.metrics.cacheHits, 1);
    assert.deepEqual(f.state.authorizations, ["Bearer caller-a", "Bearer caller-b"]);
    f.state.allow = false;
    const denied = await f.request([image], "Bearer caller-b");
    assert.equal(denied.body.images[0].url, null);
    assert.equal(denied.metrics.denied, 1);
    assert.equal(denied.metrics.operations.redis.requests, 0);
    assert.equal(denied.metrics.operations.sign.requests, 0);
});

test("eight simultaneous cold requests issue one signed URL and record lock waits", async (t) => {
    const f = fixture(t);
    f.state.signDelayMs = 25;
    const results = await Promise.all(Array.from({ length: 8 }, () => f.request()));
    assert.equal(f.state.signs, 1);
    assert.equal(new Set(results.map(r => r.body.images[0].url)).size, 1);
    assert.equal(results.reduce((sum, r) => sum + r.metrics.issued, 0), 1);
    assert.equal(results.reduce((sum, r) => sum + r.metrics.cacheHits, 0), 7);
    assert.equal(results.reduce((sum, r) => sum + r.metrics.lockWaits, 0), 7);
    assert.ok(results.every(r => r.metrics.failed === 0));
});

test("refreshes at the expiry margin and isolates overwritten object versions", async (t) => {
    const f = fixture(t);
    const first = await f.request();
    const expiry = first.body.images[0].expiresAt;
    t.mock.method(Date, "now", () => expiry - 60_000);
    const refreshed = await f.request();
    assert.equal(refreshed.metrics.issued, 1);
    assert.ok(refreshed.body.images[0].expiresAt > expiry);
    f.state.version = "v2";
    const replaced = await f.request();
    assert.equal(replaced.metrics.issued, 1);
    assert.equal(f.state.signs, 3);
});

test("Redis outage falls back to authorized signing and does not retry Redis for queued images", async (t) => {
    const f = fixture(t);
    f.state.redisFailure = true;
    const images = Array.from({ length: 16 }, (_, index) => ({ ...image, path: image.path.replace("bbbbbbbb-bbbb", `${String(index).padStart(8, "0")}-bbbb`) }));
    const result = await f.request(images);
    assert.equal(result.metrics.issued, 16);
    assert.equal(result.metrics.cacheBypasses, 16);
    assert.equal(result.metrics.operations.authorize.requests, 16);
    assert.equal(result.metrics.operations.sign.requests, 16);
    assert.ok(result.metrics.operations.redis.requests <= 8);
    assert.equal(result.metrics.operations.redis.failures, result.metrics.operations.redis.requests);
    assert.ok(result.body.images.every(x => x.url));
});

test("counts failed signing attempts separately from successful issuance and releases the lease", async (t) => {
    const f = fixture(t);
    f.state.signFailure = true;
    const failed = await f.request();
    assert.equal(failed.metrics.operations.sign.requests, 1);
    assert.equal(failed.metrics.operations.sign.failures, 1);
    assert.equal(failed.metrics.issued, 0);
    assert.equal(failed.metrics.failed, 1);
    assert.equal(failed.body.images[0].url, null);
    assert.ok([...f.values.keys()].every(key => !key.endsWith(":lock")));
    f.state.signFailure = false;
    assert.equal((await f.request()).metrics.issued, 1);
});

test("an expired lock holder cannot publish a value or delete a newer owner's lease", async (t) => {
    const f = fixture(t);
    const cache = new RedisImageCache(configuration.redisUrl, configuration.redisToken, new ImageRequestMetrics());
    assert.equal(await cache.acquire("cache-key", "old-owner"), true);
    f.save("cache-key:lock", "new-owner", 10_000);
    assert.equal(await cache.save("cache-key", "old-owner", { url: "test", expiresAt: Date.now() + 300_000 }), false);
    await cache.release("cache-key", "old-owner");
    assert.equal(f.find("cache-key:lock"), "new-owner");
    assert.equal(f.find("cache-key"), null);
});

test("rejects corrupt cached URLs, wrong images and links within the refresh margin", () => {
    const storage = new StorageImageClient(origin, "key", "Bearer caller", new ImageRequestMetrics());
    const valid = { url: `${origin}/storage/v1/object/sign/${image.bucket}/${image.path}?token=test`, expiresAt: Date.now() + 290_000 };
    assert.equal(storage.isValidSignedImage(valid, image), true);
    for (const url of [valid.url.replace(origin, "https://foreign.example"), valid.url.replace(image.bucket, "shop-review-images"), valid.url.replace("?token=test", ""), `${valid.url}#extra`]) {
        assert.equal(storage.isValidSignedImage({ ...valid, url }, image), false);
    }
    assert.equal(storage.isValidSignedImage({ ...valid, expiresAt: Date.now() + 60_000 }, image), false);
});

test("rejects malformed, oversized and unsupported image batches without network access", async (t) => {
    const f = fixture(t);
    for (const images of [[{ ...image, path: "../secret.jpg" }], [{ ...image, bucket: "other" }], Array(65).fill(image)]) {
        assert.equal((await f.request(images)).status, 400);
    }
    assert.equal(f.state.authorizations.length, 0);
    assert.equal(f.state.signs, 0);
});
