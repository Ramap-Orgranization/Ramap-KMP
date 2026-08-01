# reverse-geocode

The mobile client invokes this authenticated Edge Function to reverse geocode a latitude and longitude. The Naver REST credentials are only read from the Edge Function environment and are never included in an app binary.

Before deployment, set these Supabase Edge Function secrets without committing their values:

```sh
supabase secrets set NAVER_MAP_NCP_KEY_ID=... NAVER_CLIENT_SECRET=...
```

Deploy the function with:

```sh
supabase functions deploy reverse-geocode
```
