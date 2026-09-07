import Link from "next/link";
import type { ReactNode } from "react";

export interface NavItem {
  href: string;
  label: string;
}

export function Shell({
  app,
  userLabel,
  nav,
  children,
}: {
  app: string;
  userLabel: string;
  nav: NavItem[];
  children: ReactNode;
}) {
  return (
    <div className="min-h-screen bg-slate-100">
      <aside className="fixed inset-y-0 left-0 z-20 flex w-60 flex-col border-r border-slate-200 bg-white">
        <div className="px-5 py-5">
          <p className="text-xs font-semibold uppercase tracking-wider text-indigo-600">
            {app}
          </p>
          <h1 className="mt-1 text-lg font-bold text-slate-900">SwasthAI Dashboard</h1>
        </div>
        <nav className="flex-1 space-y-1 px-3">
          {nav.map((item) => (
            <Link
              key={item.href}
              href={item.href}
              className="block rounded-lg px-3 py-2 text-sm font-medium text-slate-600 hover:bg-slate-100 hover:text-slate-900"
            >
              {item.label}
            </Link>
          ))}
        </nav>
        <div className="border-t border-slate-200 px-5 py-4">
          <p className="text-sm font-medium text-slate-800">{userLabel}</p>
          <Link
            href="/api/auth/logout"
            className="mt-1 inline-block text-xs font-medium text-slate-400 hover:text-slate-600"
          >
            Sign out
          </Link>
        </div>
      </aside>
      <main className="pl-60">
        <div className="mx-auto max-w-5xl px-8 py-8">{children}</div>
      </main>
    </div>
  );
}