import { Badge } from "@/components/ui/badge";
import type { TenantPlan } from "../types/tenant.types";
import { tenantPlanLabels } from "../schemas/tenant.schema";

const planVariantMap: Record<TenantPlan, "default" | "secondary" | "outline"> = {
  starter: "secondary",
  professional: "default",
  business: "outline",
  enterprise: "default",
};

type TenantPlanBadgeProps = {
  plan: TenantPlan;
};

export function TenantPlanBadge({ plan }: TenantPlanBadgeProps) {
  return (
    <Badge variant={planVariantMap[plan]}>
      {tenantPlanLabels[plan] || plan}
    </Badge>
  );
}
