#!/usr/bin/env python3
"""Cache Instagram profile pictures in Supabase Storage."""

from __future__ import annotations

import argparse
import json
import math
import os
import sys
import urllib.request
from dataclasses import dataclass
from pathlib import Path
from typing import Any

from apify_client import ApifyClient
from supabase import Client, create_client


ACTOR_ID = "apify/instagram-profile-scraper"
BUCKET = "shop-profile-images"
DEFAULT_CACHE = Path(".scratch/instagram-profile-sync-results.json")
MAX_IMAGE_BYTES = 10 * 1024 * 1024
ALLOWED_MIME_TYPES = {"image/jpeg", "image/png", "image/webp"}
EXTENSIONS = {"image/jpeg": ".jpg", "image/png": ".png", "image/webp": ".webp"}
ESTIMATED_PROFILE_CHARGE_USD = 0.003
MINIMUM_CHARGE_LIMIT_USD = 0.01
MAXIMUM_CHARGE_LIMIT_USD = 1.25


@dataclass(frozen=True)
class Source:
    shop_id: str
    username: str


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    selection = parser.add_mutually_exclusive_group()
    selection.add_argument("--limit", type=int, default=3)
    selection.add_argument("--all", action="store_true")
    parser.add_argument("--username", action="append", default=[])
    parser.add_argument("--apply", action="store_true")
    parser.add_argument("--cache", type=Path, default=DEFAULT_CACHE)
    return parser.parse_args()


def load_supabase_url() -> str:
    configured_url = os.getenv("SUPABASE_URL")
    if configured_url:
        return configured_url
    properties = Path("local.properties")
    if properties.exists():
        for line in properties.read_text().splitlines():
            if line.startswith("supabase.url="):
                return line.split("=", 1)[1].strip()
    raise RuntimeError("SUPABASE_URL or local.properties supabase.url is required")


def create_supabase_client() -> Client:
    service_role_key = os.environ["SUPABASE_SERVICE_ROLE_KEY"]
    return create_client(load_supabase_url(), service_role_key)


def username_from_url(url: str) -> str | None:
    path = url.split("?", 1)[0].rstrip("/")
    username = path.rsplit("/", 1)[-1].lstrip("@")
    return username or None


def select_sources(
    rows: list[dict[str, Any]], usernames: list[str], limit: int | None
) -> list[Source]:
    sources = []
    for row in rows:
        username = username_from_url(row["instagram_url"])
        if username:
            sources.append(Source(shop_id=str(row["shop_id"]), username=username))
    if not usernames:
        return sources if limit is None else sources[:limit]
    source_by_username = {source.username.lower(): source for source in sources}
    return [source_by_username[username.lower()] for username in usernames if username.lower() in source_by_username]


def fetch_sources(client: Client, limit: int | None, usernames: list[str]) -> list[Source]:
    query = (
        client.table("instagram_sources")
        .select("shop_id,instagram_url")
        .eq("is_active", True)
        .order("created_at")
    )
    if not usernames and limit is not None:
        query = query.limit(limit)
    return select_sources(query.execute().data, usernames, limit)


def charge_limit_usd(profile_count: int) -> float:
    estimated_cents = math.ceil(profile_count * ESTIMATED_PROFILE_CHARGE_USD * 100)
    estimated_usd = estimated_cents / 100
    bounded_usd = max(MINIMUM_CHARGE_LIMIT_USD, estimated_usd)
    return min(bounded_usd, MAXIMUM_CHARGE_LIMIT_USD)


def fetch_profiles(sources: list[Source]) -> list[dict[str, Any]]:
    usernames = [source.username for source in sources]
    apify_client = ApifyClient(os.environ["APIFY_TOKEN"])
    run = apify_client.actor(ACTOR_ID).call(
        run_input={"usernames": usernames},
        max_items=len(usernames),
        max_total_charge_usd=charge_limit_usd(len(usernames)),
    )
    if not run:
        raise RuntimeError("Apify Actor did not return a run")
    items = list(
        apify_client.dataset(run.default_dataset_id).iterate_items()
    )
    source_by_username = {source.username.lower(): source for source in sources}
    results = []
    for item in items:
        username = str(item.get("username", "")).lower()
        source = source_by_username.get(username)
        if not source:
            continue
        image_url = item.get("profilePicUrlHD") or item.get("profilePicUrl")
        results.append({"shop_id": source.shop_id, "username": source.username, "image_url": image_url})
    return results


def save_cache(path: Path, profiles: list[dict[str, Any]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(profiles, ensure_ascii=False, indent=2) + "\n")
    path.chmod(0o600)


def load_cache(path: Path) -> list[dict[str, Any]]:
    return json.loads(path.read_text())


def download_image(url: str) -> tuple[bytes, str]:
    request = urllib.request.Request(url, headers={"User-Agent": "RamapProfileSync/1.0"})
    with urllib.request.urlopen(request, timeout=30) as response:
        content_type = response.headers.get_content_type().lower()
        if content_type not in ALLOWED_MIME_TYPES:
            raise ValueError(f"unsupported MIME type: {content_type}")
        content_length = response.headers.get("Content-Length")
        if content_length and int(content_length) > MAX_IMAGE_BYTES:
            raise ValueError("image exceeds maximum size")
        image = response.read(MAX_IMAGE_BYTES + 1)
    if len(image) > MAX_IMAGE_BYTES:
        raise ValueError("image exceeds maximum size")
    if not image:
        raise ValueError("empty image")
    return image, content_type


def apply_profiles(client: Client, profiles: list[dict[str, Any]]) -> tuple[int, int]:
    succeeded = 0
    failed = 0
    for profile in profiles:
        username = profile["username"]
        try:
            if not profile.get("image_url"):
                raise ValueError("profile image URL missing")
            image, content_type = download_image(profile["image_url"])
            path = f'{profile["shop_id"]}{EXTENSIONS[content_type]}'
            client.storage.from_(BUCKET).upload(
                path,
                image,
                {"content-type": content_type, "upsert": "true"},
            )
            client.table("shops").update({"instagram_profile_image_path": path}).eq(
                "id", profile["shop_id"]
            ).execute()
            succeeded += 1
            print(f"OK {username} {path}")
        except Exception as error:
            failed += 1
            print(f"FAIL {username} {type(error).__name__}", file=sys.stderr)
    return succeeded, failed


def main() -> int:
    args = parse_args()
    if not args.all and args.limit < 1:
        raise ValueError("--limit must be positive")
    limit = None if args.all else args.limit
    client = create_supabase_client()
    if args.apply:
        if not args.cache.exists():
            raise RuntimeError("cache is required for --apply; run dry-run first")
        profiles = load_cache(args.cache)
        if args.username:
            selected = {username.lower() for username in args.username}
            profiles = [profile for profile in profiles if profile["username"].lower() in selected]
        elif limit is not None:
            profiles = profiles[:limit]
        succeeded, failed = apply_profiles(client, profiles)
        print(f"SUMMARY success={succeeded} failure={failed}")
        return 1 if failed else 0

    sources = fetch_sources(client, limit, args.username)
    if args.username and len(sources) != len(args.username):
        found = {source.username.lower() for source in sources}
        missing = [username for username in args.username if username.lower() not in found]
        raise RuntimeError(f"active Instagram source not found: {', '.join(missing)}")
    profiles = fetch_profiles(sources)
    save_cache(args.cache, profiles)
    for profile in profiles:
        field = "profilePicUrlHD/profilePicUrl" if profile.get("image_url") else "missing"
        print(f'DRY-RUN {profile["username"]} {field}')
    print(f"SUMMARY fetched={len(profiles)} cache={args.cache}")
    return 0 if len(profiles) == len(sources) else 1


if __name__ == "__main__":
    raise SystemExit(main())
