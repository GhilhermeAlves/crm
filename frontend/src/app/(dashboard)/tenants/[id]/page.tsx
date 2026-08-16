"use client";

import { use } from "react";
import { PageTitle } from "@/components/common/PageTitle";
import { ErrorCard } from "@/components/common/ErrorCard";
import { SkeletonCard } from "@/components/feedback/SkeletonCard";
import { useTenant } from "@/features/tenants/hooks/useTenants";
import { TenantDetails } from "@/features/tenants/components/TenantDetails";

type Params = { id: string };

export default function TenantDetailPage({
  params,
}: {
  params: Promise<Params>;
}) {
  const { id } = use(params);
  const { data: tenant, isLoading, error } = useTenant(id);

  if (isLoading) {
    return (
      <div className="space-y-6">
        <PageTitle>Detalhes da Empresa</PageTitle>
        <div className="grid gap-6 lg:grid-cols-2">
          <SkeletonCard />
          <SkeletonCard />
          <SkeletonCard />
          <SkeletonCard />
        </div>
      </div>
    );
  }

  if (error || !tenant) {
    return (
      <div className="space-y-6">
        <PageTitle>Detalhes da Empresa</PageTitle>
        <ErrorCard message="Não foi possível carregar os dados da empresa." />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <PageTitle>{tenant.tradingName}</PageTitle>
      <TenantDetails tenant={tenant} />
    </div>
  );
}
