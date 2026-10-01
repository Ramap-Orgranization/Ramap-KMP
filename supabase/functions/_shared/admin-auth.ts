const ADMIN_TIMEOUT_MS = 3_000;

export async function isAdministrator(request: Request): Promise<boolean> {
  const token = request.headers.get("authorization")?.match(/^Bearer\s+(.+)$/i)?.[1]?.trim();
  const url = Deno.env.get("SUPABASE_URL");
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY");
  if (!token || !url || !anonKey) return false;

  const response = await fetch(`${url}/auth/v1/user`, {
    headers: { apikey: anonKey, Authorization: `Bearer ${token}` },
    signal: AbortSignal.timeout(ADMIN_TIMEOUT_MS),
  }).catch(() => null);
  const user = response?.ok ? await response.json() as { email?: unknown } : null;
  return isAdministratorEmail(user?.email);
}

export function isAdministratorEmail(value: unknown): boolean {
  if (typeof value !== "string" || !value.trim()) return false;

  const primaryEmail = Deno.env.get("ADMIN_EMAIL") ?? "uni070@naver.com";
  const additionalEmails = (Deno.env.get("ADMIN_ADDITIONAL_EMAILS") ?? "").split(",");
  const email = value.trim().toLowerCase();
  return [primaryEmail, ...additionalEmails].some((candidate) =>
    candidate.trim().toLowerCase() === email
  );
}
