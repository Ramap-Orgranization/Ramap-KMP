import assert from "node:assert/strict";
import test from "node:test";

import { isAdministrator, isAdministratorEmail } from "./admin-auth.ts";

const values = {
  ADMIN_EMAIL: "primary@example.com",
  ADMIN_ADDITIONAL_EMAILS: " second@example.com, third@example.com ",
  SUPABASE_URL: "https://example.supabase.co",
  SUPABASE_ANON_KEY: "test-anon-key",
};

globalThis.Deno = { env: { get: (name) => values[name] } };

test("accepts the primary and additional administrator emails", () => {
  assert.equal(isAdministratorEmail("primary@example.com"), true);
  assert.equal(isAdministratorEmail("THIRD@EXAMPLE.COM"), true);
  assert.equal(isAdministratorEmail("second@example.com"), true);
  assert.equal(isAdministratorEmail("other@example.com"), false);
  assert.equal(isAdministratorEmail(""), false);
});

test("requires a verified user response before granting administrator access", async () => {
  const previousFetch = globalThis.fetch;
  try {
    globalThis.fetch = async () => Response.json({ email: "third@example.com" });
    assert.equal(await isAdministrator(new Request("https://example.com", {
      headers: { Authorization: "Bearer user-token" },
    })), true);

    globalThis.fetch = async () => Response.json({ email: "other@example.com" });
    assert.equal(await isAdministrator(new Request("https://example.com", {
      headers: { Authorization: "Bearer user-token" },
    })), false);

    globalThis.fetch = async () => new Response(null, { status: 401 });
    assert.equal(await isAdministrator(new Request("https://example.com", {
      headers: { Authorization: "Bearer user-token" },
    })), false);

    assert.equal(await isAdministrator(new Request("https://example.com")), false);
  } finally {
    globalThis.fetch = previousFetch;
  }
});
