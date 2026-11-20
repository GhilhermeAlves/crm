"use client";

import { useParams, useRouter } from "next/navigation";
import { PageTitle } from "@/components/common/PageTitle";
import { ErrorCard } from "@/components/common/ErrorCard";
import { SkeletonForm } from "@/components/feedback/SkeletonForm";
import { ROUTES } from "@/lib/constants";
import { useTenant, useUpdateTenant } from "@/features/identity/tenants/hooks/useTenants";
import { TenantForm } from "@/features/identity/tenants/components/TenantForm";
import type { CreateTenantRequest } from "@/features/identity/tenants/types/tenant.types";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import Link from "next/link";
import { Button } from "@/components/ui/button";

export default function EditTenantPage() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const { data: tenant, isLoading, error } = useTenant(id);
  const updateMutation = useUpdateTenant();
  const { roles } = useAuth();
  const isPlatformAdmin = roles.includes("SUPER_ADMIN");

  const handleSubmit = (data: CreateTenantRequest) => {
    updateMutation.mutate(
      { id, data },
      {
        onSuccess: () => {
          router.push(`${ROUTES.TENANTS}/${id}`);
        },
      },
    );
  };

  if (isLoading) {
    return (
      <div className="space-y-6">
        <PageTitle>Editar Empresa</PageTitle>
        <SkeletonForm fields={8} />
      </div>
    );
  }

  if (error || !tenant) {
    return (
      <div className="space-y-6">
        <PageTitle>Editar Empresa</PageTitle>
        <ErrorCard message="Não foi possível carregar os dados da empresa." />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <PageTitle>Editar — {tenant.tradingName}</PageTitle>
      <TenantForm
        initialData={tenant}
        onSubmit={handleSubmit}
        isLoading={updateMutation.isPending}
      />
      {isPlatformAdmin && (
        <Button asChild variant="outline">
          <Link href={`${ROUTES.COMPANY_MODULES}?company=${id}`}>Gerenciar módulos</Link>
        </Button>
      )}
    </div>
  );
}
