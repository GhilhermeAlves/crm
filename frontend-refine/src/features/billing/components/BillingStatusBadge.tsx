import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import { BILLING_STATUS_LABELS, type BillingStatus } from "../types";

const COLORS: Partial<Record<BillingStatus | "UNKNOWN", string>> = {
  ACTIVE: "bg-emerald-100 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-200",
  PAST_DUE: "bg-amber-100 text-amber-800 dark:bg-amber-950 dark:text-amber-200",
  BLOCKED: "bg-red-100 text-red-800 dark:bg-red-950 dark:text-red-200",
  EXEMPT: "bg-sky-100 text-sky-800 dark:bg-sky-950 dark:text-sky-200",
};

export function BillingStatusBadge({ status }: { status: BillingStatus | "UNKNOWN" }) {
  return (
    <Badge variant="secondary" className={cn("font-medium", COLORS[status])}>
      {BILLING_STATUS_LABELS[status]}
    </Badge>
  );
}
