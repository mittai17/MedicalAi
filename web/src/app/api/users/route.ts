import { NextResponse } from "next/server";
import { ApiError, api } from "@/lib/api";
import { getSession } from "@/lib/auth";

export async function POST(req: Request) {
  const session = await getSession();
  if (!session) {
    return NextResponse.json({ error: "Not authenticated" }, { status: 401 });
  }

  try {
    const body = (await req.json()) as {
      username?: string;
      password?: string;
      email?: string;
      role?: string;
    };
    const username = body.username?.trim() ?? "";
    const password = body.password ?? "";
    const role = body.role === "health_worker" ? "health_worker" : "citizen";

    if (!username || !password) {
      return NextResponse.json({ error: "Username and password are required" }, { status: 400 });
    }

    const result = await api.register(username, password, role, body.email?.trim());
    return NextResponse.json({ ok: true, user: result.user });
  } catch (err) {
    if (err instanceof ApiError) {
      return NextResponse.json({ error: err.message }, { status: err.status });
    }
    const message = err instanceof Error ? err.message : "Could not create user";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}