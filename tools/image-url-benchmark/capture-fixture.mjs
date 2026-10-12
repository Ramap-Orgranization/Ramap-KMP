#!/usr/bin/env node
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const [profileId, output] = process.argv.slice(2);
if (!profileId || !output) {
    console.log("node tools/image-url-benchmark/capture-fixture.mjs <public-profile-uuid> /private/fixture.json");
    process.exit(0);
}
if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(profileId)) throw new Error("Invalid profile UUID");
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");
const propertiesFile = path.join(root, "local.properties");
const properties = Object.fromEntries((fs.existsSync(propertiesFile) ? fs.readFileSync(propertiesFile, "utf8") : "").split(/\r?\n/).filter(line => !line.startsWith("#") && line.includes("=")).map(line => {
    const index = line.indexOf("=");
    return [line.slice(0, index).trim(), line.slice(index + 1).trim().replace(/\\:/g, ":")];
}));
const base = process.env.SUPABASE_URL ?? properties["supabase.url"];
const key = process.env.SUPABASE_ANON_KEY ?? properties["supabase.anon_key"];
let role;
try { role = JSON.parse(Buffer.from(key?.split(".")[1] ?? "", "base64url")).role; } catch { /* Reject below. */ }
if (!base || !key || role !== "anon") throw new Error("A project URL and legacy anon JWT are required; no privileged credentials");
const url = new URL("/rest/v1/rpc/fetch_profile_reviews", base);
if (url.protocol !== "https:" || !url.hostname.endsWith(".supabase.co")) throw new Error("Expected a Supabase HTTPS project URL");
const response = await fetch(url, {
    method: "POST", headers: { apikey: key, Authorization: `Bearer ${key}`, "Content-Type": "application/json" },
    body: JSON.stringify({ p_user_id: profileId, p_offset: 0 }), signal: AbortSignal.timeout(15_000),
});
if (!response.ok) throw new Error(`Public review read returned HTTP ${response.status}`);
const rows = await response.json();
if (!Array.isArray(rows) || rows.length === 0 || rows.length > 20) throw new Error("Expected a non-empty public review page");
const reviews = rows.map(row => ({
    avatar: row.avatar_path ? { bucket: "profile-avatars", path: row.avatar_path } : null,
    images: (row.image_paths ?? []).map(imagePath => ({ bucket: "shop-review-images", path: imagePath })),
}));
// Do not overwrite another fixture or publish paths, IDs, review text or signed URLs.
fs.writeFileSync(output, JSON.stringify({ reviews }), { mode: 0o600, flag: "wx" });
console.log(JSON.stringify({ reviews: reviews.length, avatarOccurrences: reviews.filter(row => row.avatar).length, reviewImageOccurrences: reviews.flatMap(row => row.images).length }));
