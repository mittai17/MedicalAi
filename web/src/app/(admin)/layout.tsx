import { requireAdmin } from "@/lib/auth";
import { roleLabel } from "@/lib/auth";
import { Shell } from "@/components/shell";

export default async function AdminLayout({ children }: { children: React.ReactNode }) {
  const session = await requireAdmin();

  return (
    <Shell
      app="Govt / SaaS Admin"
      userLabel={`${roleLabel("admin")} · ${session.username}`}
      nav={[
        { href: "/overview", label: "Overview" },
        { href: "/citizens", label: "Citizens" },
        { href: "/workers", label: "Health Workers" },
        { href: "/patients", label: "Patients" },
        { href: "/screenings", label: "Screenings" },
        { href: "/vitals", label: "Vitals" },
        { href: "/referrals", label: "Referrals" },
        { href: "/analytics", label: "Analytics" },
      ]}
    >
      {children}
    </Shell>
  );
}