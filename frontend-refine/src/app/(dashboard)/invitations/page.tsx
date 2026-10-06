import { redirect } from "next/navigation";

import { ROUTES } from "@/lib/constants";

/** Centralizado em /settings/company/users (aba Convites pendentes). */
export default function InvitationsPage() {
  redirect(`${ROUTES.SETTINGS_COMPANY_USERS}?tab=convites`);
}
