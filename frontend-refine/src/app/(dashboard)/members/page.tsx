import { redirect } from "next/navigation";

import { ROUTES } from "@/lib/constants";

/** Centralizado em /settings/company/users (aba Ativos). */
export default function MembersPage() {
  redirect(`${ROUTES.SETTINGS_COMPANY_USERS}?tab=ativos`);
}
