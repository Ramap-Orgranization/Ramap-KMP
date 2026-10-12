import { handleImageUrls } from "./handler.ts";

Deno.serve((request: Request) => handleImageUrls(request, {
    supabaseUrl: Deno.env.get("SUPABASE_URL") ?? "",
    anonKey: Deno.env.get("SUPABASE_ANON_KEY") ?? "",
    redisUrl: Deno.env.get("UPSTASH_REDIS_REST_URL"),
    redisToken: Deno.env.get("UPSTASH_REDIS_REST_TOKEN"),
}));
