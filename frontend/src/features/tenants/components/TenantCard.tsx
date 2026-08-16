import Link from "next/link";
import { Building2 } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { ROUTES } from "@/lib/constants";
import type { Tenant } from "../types/tenant.types";
import { TenantStatusBadge } from "./TenantStatusBadge";
import { TenantPlanBadge } from "./TenantPlanBadge";

type TenantCardProps = {
  tenant: Tenant;
};

export function TenantCard({ tenant }: TenantCardProps) {
  return (
    <Link href={`${ROUTES.TENANTS}/${tenant.id}`}>
      <Card className="transition-shadow hover:shadow-md">
        <CardContent className="p-4">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-muted">
              {tenant.logoUrl ? (
                <img
                  src={tenant.logoUrl}
                  alt={tenant.tradingName}
                  className="h-10 w-10 rounded-lg object-cover"
                />
              ) : (
                <Building2 className="h-5 w-5 text-muted-foreground" />
              )}
            </div>
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-medium">
                {tenant.tradingName}
              </p>
              <p className="truncate text-xs text-muted-foreground">
                {tenant.cnpj}
              </p>
            </div>
            <div className="flex flex-col items-end gap-1">
              <TenantStatusBadge status={tenant.status} />
              <TenantPlanBadge plan={tenant.plan} />
            </div>
          </div>
        </CardContent>
      </Card>
    </Link>
  );
}
