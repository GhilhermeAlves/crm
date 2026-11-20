import { redirect } from "next/navigation";

import { ROUTES } from "@/lib/constants";

/** A migração de dados é feita pelo importador real em /settings/import. */
export default function CompanyMigrationPage() {
  redirect(ROUTES.SETTINGS_IMPORT);
}
