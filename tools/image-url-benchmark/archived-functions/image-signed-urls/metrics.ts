type Operation = "authorize" | "sign" | "redis";
type OperationMetrics = { requests: number; failures: number; durationSumMs: number };

/** Per-request counters; never include image paths, signed URLs, or credentials. */
export class ImageRequestMetrics {
    readonly requestId = crypto.randomUUID();
    private readonly startedAt = performance.now();
    private readonly operations: Record<Operation, OperationMetrics> = {
        authorize: { requests: 0, failures: 0, durationSumMs: 0 },
        sign: { requests: 0, failures: 0, durationSumMs: 0 },
        redis: { requests: 0, failures: 0, durationSumMs: 0 },
    };
    cacheHits = 0;
    issued = 0;
    cacheBypasses = 0;
    denied = 0;
    failed = 0;
    lockWaits = 0;

    async measure<T>(operation: Operation, action: () => Promise<T>): Promise<T> {
        const metric = this.operations[operation];
        metric.requests++;
        const start = performance.now();
        try {
            return await action();
        } catch (error) {
            metric.failures++;
            throw error;
        } finally {
            metric.durationSumMs += performance.now() - start;
        }
    }

    snapshot(requested: number, unique: number) {
        const operations = Object.fromEntries(Object.entries(this.operations).map(([name, metric]) =>
            [name, { ...metric, durationSumMs: round(metric.durationSumMs) }]));
        return {
            schemaVersion: 1,
            requestId: this.requestId,
            requested,
            unique,
            cacheHits: this.cacheHits,
            issued: this.issued,
            cacheBypasses: this.cacheBypasses,
            denied: this.denied,
            failed: this.failed,
            lockWaits: this.lockWaits,
            totalMs: round(performance.now() - this.startedAt),
            operations,
        };
    }
}

function round(value: number): number {
    return Math.round(value * 100) / 100;
}
