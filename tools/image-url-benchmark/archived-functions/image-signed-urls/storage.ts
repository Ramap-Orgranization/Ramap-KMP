import { ImageRequestMetrics } from "./metrics.ts";

export const SIGNED_URL_LIFETIME_SECONDS = 300;
export const REFRESH_MARGIN_MS = 60_000;
const STORAGE_TIMEOUT_MS = 3_000;

export type ImageRequest = { bucket: string; path: string };
export type SignedImage = { url: string; expiresAt: number };
export type ImageMetadata = { id: string; version: string; lastModified: string };

export class ImageAccessError extends Error {}

export class StorageImageClient {
    private readonly baseUrl: URL;

    constructor(url: string, private readonly anonKey: string, private readonly authorization: string,
        private readonly metrics: ImageRequestMetrics) {
        this.baseUrl = new URL(url);
    }

    async authorize(image: ImageRequest): Promise<ImageMetadata> {
        return this.metrics.measure("authorize", () => this.fetchMetadata(image));
    }

    private async fetchMetadata(image: ImageRequest): Promise<ImageMetadata> {
        const response = await this.request(`/object/info/${this.objectPath(image)}`);
        const metadata = await response.json();
        if (typeof metadata.id !== "string" || typeof metadata.version !== "string" ||
            typeof metadata.last_modified !== "string") {
            throw new Error("invalid_image_metadata");
        }
        return { id: metadata.id, version: metadata.version, lastModified: metadata.last_modified };
    }

    async sign(image: ImageRequest): Promise<SignedImage> {
        return this.metrics.measure("sign", () => this.issueSignedImage(image));
    }

    private async issueSignedImage(image: ImageRequest): Promise<SignedImage> {
        // Use the time before issuance, so network latency cannot extend the recorded expiry.
        const expiresAt = Math.floor(Date.now() / 1_000) * 1_000 + SIGNED_URL_LIFETIME_SECONDS * 1_000;
        const response = await this.request(`/object/sign/${this.objectPath(image)}`, {
            method: "POST",
            body: JSON.stringify({ expiresIn: SIGNED_URL_LIFETIME_SECONDS }),
        });
        const body = await response.json();
        if (typeof body.signedURL !== "string" || !body.signedURL.startsWith("/object/sign/")) {
            throw new Error("invalid_signed_image");
        }
        const signed = { url: new URL(`/storage/v1${body.signedURL}`, this.baseUrl).href, expiresAt };
        if (!this.isValidSignedImage(signed, image)) throw new Error("invalid_signed_image");
        return signed;
    }

    isValidSignedImage(value: unknown, image: ImageRequest): value is SignedImage {
        if (!value || typeof value !== "object") return false;
        const signed = value as SignedImage;
        if (typeof signed.url !== "string" || !Number.isSafeInteger(signed.expiresAt) ||
            signed.expiresAt <= Date.now() + REFRESH_MARGIN_MS ||
            signed.expiresAt > Date.now() + SIGNED_URL_LIFETIME_SECONDS * 1_000) return false;
        try {
            const url = new URL(signed.url);
            return url.origin === this.baseUrl.origin && !url.username && !url.password && !url.hash &&
                url.pathname === `/storage/v1/object/sign/${this.objectPath(image)}` &&
                Boolean(url.searchParams.get("token"));
        } catch {
            return false;
        }
    }

    async cacheKey(image: ImageRequest, metadata: ImageMetadata): Promise<string> {
        const identity = JSON.stringify([this.baseUrl.origin, image.bucket, image.path,
            metadata.id, metadata.version, metadata.lastModified, SIGNED_URL_LIFETIME_SECONDS]);
        const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(identity));
        const hash = Array.from(new Uint8Array(digest), (byte) => byte.toString(16).padStart(2, "0")).join("");
        return `ramap:signed-image-url:v1:{${hash}}`;
    }

    private objectPath(image: ImageRequest): string {
        return `${image.bucket}/${image.path.split("/").map(encodeURIComponent).join("/")}`;
    }

    private async request(path: string, options: RequestInit = {}): Promise<Response> {
        const response = await fetch(new URL(`/storage/v1${path}`, this.baseUrl), {
            ...options,
            headers: { apikey: this.anonKey, Authorization: this.authorization, "Content-Type": "application/json" },
            signal: AbortSignal.timeout(STORAGE_TIMEOUT_MS),
        });
        if ([400, 401, 403, 404].includes(response.status)) throw new ImageAccessError("image_unavailable");
        if (!response.ok) throw new Error("storage_request_failed");
        return response;
    }
}
