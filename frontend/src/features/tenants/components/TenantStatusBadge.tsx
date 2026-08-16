import { BadgeStatus } from "@/components/common/BadgeStatus";
import type { TenantStatus } from "../types/tenant.types";
import { tenantStatusLabels } from "../schemas/tenant.schema";

const statusVariantMap: Record<
  TenantStatus,
  "success" | "warning" | "info" | "danger"
> = {
  active: "success",
  onboarding: "info",
  suspended: "warning",
  inactive: "danger",
};

type TenantStatusBadgeProps = {
  status: TenantStatus;
};

export function TenantStatusBadge({ status }: TenantStatusBadgeProps) {
  return (
    <BadgeStatus variant={statusVariantMap[status]}>
      {tenantStatusLabels[status] || status}
    </BadgeStatus>
  );
}
