"use client";

import { useRouter } from "next/navigation";
import { PageTitle } from "@/components/common/PageTitle";
import { ROUTES } from "@/lib/constants";
import { useCreateTenant } from "@/features/tenants/hooks/useTenants";
import { TenantForm } from "@/features/tenants/components/TenantForm";
import type { CreateTenantRequest } from "@/features/tenants/types/tenant.types";

export default function NewTenantPage() {
  const router = useRouter();
  const createMutation = useCreateTenant();

  const handleSubmit = (data: CreateTenantRequest) => {
    createMutation.mutate(data, {
      onSuccess: () => {
        router.push(ROUTES.TENANTS);
      },
    });
  };

  return (
    <div className="space-y-6">
      <PageTitle>Nova Empresa</PageTitle>
      <TenantForm
        onSubmit={handleSubmit}
        isLoading={createMutation.isPending}
      />
    </div>
  );
}
