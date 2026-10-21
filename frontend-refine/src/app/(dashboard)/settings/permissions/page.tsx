import { redirect } from "next/navigation";

import { ROUTES } from "@/lib/constants";

/** O catálogo de permissões aparece na matriz de /settings/roles (Perfis e Permissões). */
export default function PermissionsPage() {
  redirect(ROUTES.SETTINGS_ROLES);
}
