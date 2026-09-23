"use client";

import { RoleForm } from "@/features/rbac/components/RoleForm";
import { useAuthorization } from "@/features/auth/hooks/useAuthorization";

export default function NewRolePage() {
  const { can } = useAuthorization();

  if (!can("role:manage")) {
    return (
      <div className="py-12 text-center">
        <p className="text-muted-foreground">Sem permissão para criar roles.</p>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-2xl">
      <RoleForm mode="create" />
    </div>
  );
}
