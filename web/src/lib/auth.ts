import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import type { Role, Session } from "./types";

const COOKIE = "swasthai.session";
const SESSION_MAX_AGE = 60 * 60 * 24 * 7; // 7 days

export function roleLabel(role: Role): string {
  return role === "admin" ? "Govt / SaaS Admin" : "User";
}

function encode(session: Session): string {
  return Buffer.from(JSON.stringify(session)).toString("base64");
}

function decode(value: string): Session | null {
  try {
    const parsed = JSON.parse(Buffer.from(value, "base64").toString("utf8")) as Session;
    if (parsed.role && parsed.username && parsed.token) return parsed;
    return null;
  } catch {
    return null;
  }
}

export async function getSession(): Promise<Session | null> {
  const store = await cookies();
  const value = store.get(COOKIE)?.value;
  return value ? decode(value) : null;
}

export async function setSession(session: Session): Promise<void> {
  const store = await cookies();
  store.set(COOKIE, encode(session), {
    httpOnly: true,
    sameSite: "lax",
    path: "/",
    maxAge: SESSION_MAX_AGE,
  });
}

export async function clearSession(): Promise<void> {
  const store = await cookies();
  store.delete(COOKIE);
}

/** Guard for the admin console. Redirects to /login when not authenticated. */
export async function requireAdmin(): Promise<Session> {
  const session = await getSession();
  if (!session || session.role !== "admin") redirect("/login");
  return session;
}