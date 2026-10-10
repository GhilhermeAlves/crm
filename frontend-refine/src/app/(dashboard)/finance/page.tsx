import { redirect } from "next/navigation";
import { ROUTES } from "@/lib/constants";

export default function FinancePage() {
  redirect(ROUTES.FINANCE_CONTROL);
}
