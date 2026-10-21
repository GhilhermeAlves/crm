import { redirect } from "next/navigation";

import { ROUTES } from "@/lib/constants";

/** Centralizado em /settings/company/users (Equipe, aba Ativos). */
export default function SettingsUsersPage() {
  redirect(`${ROUTES.SETTINGS_COMPANY_USERS}?tab=ativos`);
}
