import { RedisImageCache } from "./redis.ts";
import { ImageRequestMetrics } from "./metrics.ts";
import { ImageAccessError, type ImageRequest, type SignedImage, StorageImageClient } from "./storage.ts";

const MAX_IMAGES = 64;
const MAX_BODY_BYTES = 20_000;
const MAX_CONCURRENT_IMAGES = 8;
const LOCK_WAIT_MS = 5_500;
const POLL_INTERVAL_MS = 150;
const IMAGE_PATH = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\.(jpg|png)$/i;
const BUCKETS = new Set(["profile-avatars", "shop-review-images"]);
const CORS_HEADERS = {
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-client-info",
    "Access-Control-Allow-Methods": "POST, OPTIONS",
    "Access-Control-Expose-Headers": "x-image-metrics, server-timing",
    "Cache-Control": "no-store",
};

type ImageResult = ImageRequest & {
    url: string | null;
    expiresAt: number | null;
    error: string | null;
};
export type ImageServerConfiguration = { supabaseUrl: string; anonKey: string; redisUrl?: string; redisToken?: string };

export async function handleImageUrls(request: Request, configuration: ImageServerConfiguration): Promise<Response> {
    if (request.method === "OPTIONS") return reply({}, 200);
    if (request.method !== "POST") return reply({ code: "method_not_allowed" }, 405);
    const authorization = request.headers.get("authorization");
    if (!authorization?.match(/^Bearer\s+\S+$/i)) return reply({ code: "authentication_required" }, 401);
    if (!configuration.supabaseUrl || !configuration.anonKey) return reply({ code: "server_not_configured" }, 503);

    const counts = new ImageRequestMetrics();
    const images = await parseImages(request);
    if (images === null) return reply({ code: "invalid_images" }, 400);
    const uniqueImages = Array.from(new Map(images.map((image) => [JSON.stringify(image), image])).values());
    const storage = new StorageImageClient(configuration.supabaseUrl, configuration.anonKey, authorization, counts);
    const cache = createCache(configuration, counts);
    const results = await mapImages(uniqueImages, async (image) => {
        try {
            // This uses the caller's JWT and the existing Storage SELECT policies, even on cache hits.
            const metadata = await storage.authorize(image);
            const key = await storage.cacheKey(image, metadata);
            const signed = await resolveImage(image, key, storage, cache, counts);
            return { ...image, ...signed, error: null };
        } catch (error) {
            const denied = error instanceof ImageAccessError;
            if (denied) counts.denied++; else counts.failed++;
            return { ...image, url: null, expiresAt: null, error: denied ? "image_unavailable" : "image_temporarily_unavailable" };
        }
    });
    const metrics = counts.snapshot(images.length, uniqueImages.length);
    console.info(JSON.stringify({ event: "image_signed_urls", ...metrics }));
    return reply({ images: results }, 200, {
        "X-Image-Metrics": JSON.stringify(metrics),
        "Server-Timing": `image-urls;dur=${metrics.totalMs}`,
    });
}

async function resolveImage(image: ImageRequest, key: string, storage: StorageImageClient,
    cache: RedisImageCache | null, counts: ImageRequestMetrics): Promise<SignedImage> {
    if (!cache || !cache.isAvailable()) return issueWithoutCache(image, storage, counts);
    const owner = crypto.randomUUID();
    let acquired = false;
    try {
        const cached = await cache.find(key);
        if (storage.isValidSignedImage(cached, image)) {
            counts.cacheHits++;
            return cached;
        }
        acquired = await cache.acquire(key, owner);
        if (!acquired) return await waitForImage(image, key, storage, cache, counts);
        // A different instance may have populated the cache just before this lock was acquired.
        const shared = await cache.find(key);
        if (storage.isValidSignedImage(shared, image)) {
            counts.cacheHits++;
            await releaseQuietly(cache, key, owner);
            return shared;
        }
    } catch (error) {
        if (error instanceof LockWaitExpired) throw error;
        cache.disable();
        return issueWithoutCache(image, storage, counts);
    }

    let saved = false;
    try {
        const signed = await storage.sign(image);
        counts.issued++;
        try {
            saved = await cache.save(key, owner, signed);
            if (!saved) counts.cacheBypasses++;
        } catch {
            cache.disable();
            counts.cacheBypasses++;
        }
        return signed;
    } finally {
        // A successful save already releases the lease atomically.
        if (acquired && !saved) await releaseQuietly(cache, key, owner);
    }
}

async function waitForImage(image: ImageRequest, key: string, storage: StorageImageClient,
    cache: RedisImageCache, counts: ImageRequestMetrics): Promise<SignedImage> {
    counts.lockWaits++;
    const deadline = Date.now() + LOCK_WAIT_MS;
    while (Date.now() < deadline) {
        await new Promise((resolve) => setTimeout(resolve, POLL_INTERVAL_MS));
        const cached = await cache.find(key);
        if (storage.isValidSignedImage(cached, image)) {
            counts.cacheHits++;
            return cached;
        }
    }
    // Do not create another signed URL while a healthy Redis lock is held.
    throw new LockWaitExpired();
}

class LockWaitExpired extends Error {}

async function issueWithoutCache(image: ImageRequest, storage: StorageImageClient, counts: ImageRequestMetrics): Promise<SignedImage> {
    counts.cacheBypasses++;
    const signed = await storage.sign(image);
    counts.issued++;
    return signed;
}

async function releaseQuietly(cache: RedisImageCache, key: string, owner: string): Promise<void> {
    if (!cache.isAvailable()) return;
    try {
        await cache.release(key, owner);
    } catch {
        // The lease expires automatically; never delete a lock owned by another request.
    }
}

function createCache(configuration: ImageServerConfiguration, counts: ImageRequestMetrics): RedisImageCache | null {
    if (!configuration.redisUrl || !configuration.redisToken) return null;
    try {
        return new RedisImageCache(configuration.redisUrl, configuration.redisToken, counts);
    } catch {
        return null;
    }
}

async function parseImages(request: Request): Promise<ImageRequest[] | null> {
    try {
        const reader = request.body?.getReader();
        if (!reader) return null;
        const chunks: Uint8Array[] = [];
        let size = 0;
        while (true) {
            const { done, value } = await reader.read();
            if (done) break;
            size += value.byteLength;
            if (size > MAX_BODY_BYTES) {
                await reader.cancel();
                return null;
            }
            chunks.push(value);
        }
        const bytes = new Uint8Array(size);
        let offset = 0;
        for (const chunk of chunks) { bytes.set(chunk, offset); offset += chunk.byteLength; }
        const body = JSON.parse(new TextDecoder().decode(bytes));
        if (!body || !Array.isArray(body.images) || body.images.length > MAX_IMAGES) return null;
        const images: ImageRequest[] = [];
        for (const image of body.images) {
            if (!image || !BUCKETS.has(image.bucket) || typeof image.path !== "string" || !IMAGE_PATH.test(image.path)) return null;
            images.push({ bucket: image.bucket, path: image.path });
        }
        return images;
    } catch {
        return null;
    }
}

async function mapImages(images: ImageRequest[], action: (image: ImageRequest) => Promise<ImageResult>): Promise<ImageResult[]> {
    const results: ImageResult[] = new Array(images.length);
    let next = 0;
    await Promise.all(Array.from({ length: Math.min(MAX_CONCURRENT_IMAGES, images.length) }, async () => {
        while (next < images.length) {
            const index = next++;
            results[index] = await action(images[index]);
        }
    }));
    return results;
}

function reply(body: unknown, status = 200, headers: Record<string, string> = {}): Response {
    return Response.json(body, { status, headers: { ...CORS_HEADERS, ...headers } });
}
