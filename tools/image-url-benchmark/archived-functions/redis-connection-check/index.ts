const REDIS_TIMEOUT_MS = 5_000;
const PROBE_LIFETIME_SECONDS = 15;
const PROBE_VALUE = "connected";

Deno.serve(async (request: Request) => {
    if (request.method !== "POST") {
        return response({ code: "method_not_allowed" }, 405, { Allow: "POST" });
    }
    const redisUrl = Deno.env.get("UPSTASH_REDIS_REST_URL");
    const redisToken = Deno.env.get("UPSTASH_REDIS_REST_TOKEN");
    if (!redisUrl || !redisToken) {
        return response({ configured: false, connected: false }, 503);
    }
    const connectionToken = request.headers.get("x-redis-connection-token")?.trim();
    if (connectionToken !== redisToken) {
        return response({ code: "connection_token_required" }, 403);
    }

    try {
        const endpoint = new URL(redisUrl);
        if (
            endpoint.protocol !== "https:" ||
            !endpoint.hostname.endsWith(".upstash.io") ||
            endpoint.username || endpoint.password || endpoint.port ||
            endpoint.search || endpoint.hash || endpoint.pathname !== "/"
        ) {
            return response({ code: "invalid_redis_configuration" }, 503);
        }

        const probeKey = `ramap:redis-connection-check:${crypto.randomUUID()}`;
        const result = await fetch(new URL("/pipeline", endpoint), {
            method: "POST",
            headers: {
                Authorization: `Bearer ${redisToken}`,
                "Content-Type": "application/json",
            },
            body: JSON.stringify([
                ["PING"],
                ["SET", probeKey, PROBE_VALUE, "EX", PROBE_LIFETIME_SECONDS],
                ["GET", probeKey],
            ]),
            signal: AbortSignal.timeout(REDIS_TIMEOUT_MS),
        });
        if (!result.ok) {
            return response({ configured: true, connected: false, code: "redis_request_failed" }, 503);
        }

        const commands: unknown = await result.json();
        const connected = Array.isArray(commands) && commands.length === 3 &&
            commands[0]?.result === "PONG" &&
            commands[1]?.result === "OK" &&
            commands[2]?.result === PROBE_VALUE &&
            commands.every((command) => command && typeof command === "object" && !command.error);
        return response({ configured: true, connected, readWrite: connected }, connected ? 200 : 503);
    } catch {
        return response({ configured: true, connected: false, code: "redis_connection_failed" }, 503);
    }
});

function response(body: Record<string, unknown>, status = 200, headers: HeadersInit = {}): Response {
    return Response.json(body, {
        status,
        headers: { "Cache-Control": "no-store", ...headers },
    });
}
