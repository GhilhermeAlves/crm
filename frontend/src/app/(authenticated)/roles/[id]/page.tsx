"use client";

import { use } from "react";
import { RoleDetails } from "@/features/rbac/components/RoleDetails";
import { useRole } from "@/features/rbac/hooks/useRoles";

export default function RoleDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const { data: role, isLoading } = useRole(id);

  if (isLoading) {
    return (
      <div className="flex h-32 items-center justify-center">
        <p className="text-muted-foreground">Carregando role...</p>
      </div>
    );
  }

  if (!role) {
    return (
      <div className="py-12 text-center">
        <p className="text-muted-foreground">Role não encontrada.</p>
      </div>
    );
  }

  return <RoleDetails role={role} />;
}
