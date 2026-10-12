// Identical native batch operation for Node (direct) and Deno (Edge).
// Returns links in memory only. The runner persists counters, never links or paths.
export async function batchSign(images, base, anonKey, authorization, fetcher = fetch) {
    const unique = [...new Map(images.map(image => [JSON.stringify([image.bucket, image.path]), image])).values()];
    const groups = [...new Set(unique.map(image => image.bucket))];
    const started = performance.now();
    const metrics = { requested: images.length, unique: unique.length, issued: 0, cacheHits: 0,
        failed: 0, cacheBypasses: 0, operations: { authorize: { requests: 0 }, sign: { requests: 0, failures: 0 }, redis: { requests: 0 } } };
    const results = (await Promise.all(groups.map(async bucket => {
        const selected = unique.filter(image => image.bucket === bucket);
        metrics.operations.sign.requests++;
        const expiresAt = Math.floor(Date.now() / 1000) * 1000 + 300_000;
        try {
            const response = await fetcher(`${base}/storage/v1/object/sign/${encodeURIComponent(bucket)}`, {
                method: "POST", headers: { apikey: anonKey, Authorization: authorization, "Content-Type": "application/json" },
                body: JSON.stringify({ paths: selected.map(image => image.path), expiresIn: 300 }),
                signal: AbortSignal.timeout(3000),
            });
            if (!response.ok) throw new Error("batch_http_error");
            const body = await response.json();
            if (!Array.isArray(body)) throw new Error("invalid_batch_response");
            return selected.map(image => {
                const matches = body.filter(value => value.path === image.path);
                const value = matches.length === 1 ? matches[0] : null;
                try {
                    if (!value || value.error || typeof value.signedURL !== "string" || !value.signedURL.startsWith("/object/sign/")) throw new Error();
                    const url = new URL(`/storage/v1${value.signedURL}`, base);
                    const expected = `/storage/v1/object/sign/${bucket}/${image.path.split("/").map(encodeURIComponent).join("/")}`;
                    if (url.origin !== new URL(base).origin || url.pathname !== expected || !url.searchParams.get("token")) throw new Error();
                    metrics.issued++;
                    return { ...image, url: url.href, expiresAt, error: null };
                } catch {
                    metrics.failed++;
                    return { ...image, url: null, expiresAt: null, error: "image_unavailable" };
                }
            });
        } catch {
            metrics.operations.sign.failures++;
            metrics.failed += selected.length;
            return selected.map(image => ({ ...image, url: null, expiresAt: null, error: "batch_unavailable" }));
        }
    }))).flat();
    metrics.totalMs = Math.round((performance.now() - started) * 100) / 100;
    return { images: results, metrics };
}
