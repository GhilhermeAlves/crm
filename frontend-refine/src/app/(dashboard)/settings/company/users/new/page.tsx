import { redirect } from "next/navigation";

import { ROUTES } from "@/lib/constants";

/** O fluxo legado "create user" (POST /users/invite) foi unificado na página central. */
export default function NewUsersPage() {
  redirect(ROUTES.SETTINGS_COMPANY_USERS);
}
