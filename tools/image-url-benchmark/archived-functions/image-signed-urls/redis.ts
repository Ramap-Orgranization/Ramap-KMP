import { REFRESH_MARGIN_MS, type SignedImage } from "./storage.ts";
import { ImageRequestMetrics } from "./metrics.ts";

const REDIS_TIMEOUT_MS = 1_000;
const LOCK_LIFETIME_SECONDS = 10;
const RELEASE_SCRIPT = "if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) end return 0";
const SAVE_SCRIPT = "if redis.call('GET', KEYS[1]) == ARGV[1] then redis.call('SET', KEYS[2], ARGV[2], 'PX', ARGV[3]); redis.call('DEL', KEYS[1]); return 1 end return 0";

export class RedisImageCache {
    private readonly endpoint: URL;
    private available = true;

    constructor(url: string, private readonly token: string, private readonly metrics: ImageRequestMetrics) {
        this.endpoint = new URL(url);
        if (this.endpoint.protocol !== "https:" || !this.endpoint.hostname.endsWith(".upstash.io") ||
            this.endpoint.username || this.endpoint.password || this.endpoint.port || this.endpoint.search ||
            this.endpoint.hash || this.endpoint.pathname !== "/") throw new Error("invalid_redis_configuration");
    }

    isAvailable(): boolean {
        return this.available;
    }

    disable(): void {
        this.available = false;
    }

    async find(key: string): Promise<unknown> {
        const value = await this.command(["GET", key]);
        if (value === null) return null;
        if (typeof value !== "string") throw new Error("invalid_redis_response");
        try {
            return JSON.parse(value);
        } catch {
            return null;
        }
    }

    async acquire(key: string, owner: string): Promise<boolean> {
        return await this.command(["SET", `${key}:lock`, owner, "NX", "EX", LOCK_LIFETIME_SECONDS]) === "OK";
    }

    async save(key: string, owner: string, signed: SignedImage): Promise<boolean> {
        const remainingMs = signed.expiresAt - Date.now() - REFRESH_MARGIN_MS;
        if (remainingMs <= 0) return false;
        // Only the current lock holder may publish a value or release the lock.
        return await this.command(["EVAL", SAVE_SCRIPT, 2, `${key}:lock`, key, owner, JSON.stringify(signed), remainingMs]) === 1;
    }

    async release(key: string, owner: string): Promise<void> {
        await this.command(["EVAL", RELEASE_SCRIPT, 1, `${key}:lock`, owner]);
    }

    private async command(command: (string | number)[]): Promise<unknown> {
        if (!this.available) throw new Error("redis_unavailable");
        return this.metrics.measure("redis", () => this.sendCommand(command));
    }

    private async sendCommand(command: (string | number)[]): Promise<unknown> {
        const response = await fetch(this.endpoint, {
            method: "POST",
            headers: { Authorization: `Bearer ${this.token}`, "Content-Type": "application/json" },
            body: JSON.stringify(command),
            signal: AbortSignal.timeout(REDIS_TIMEOUT_MS),
        });
        if (!response.ok) throw new Error("redis_request_failed");
        const body = await response.json();
        if (!body || body.error || !("result" in body)) throw new Error("redis_command_failed");
        return body.result;
    }
}
