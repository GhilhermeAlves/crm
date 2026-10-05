"use client";

import { PermissionList } from "@/features/identity/rbac/components/PermissionList";
import { usePermissions } from "@/features/identity/rbac/hooks/useRoles";

export default function PermissionsPage() {
  const { data: permissions, isLoading } = usePermissions();

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Permissões</h1>
        <p className="text-muted-foreground">
          Visualize todas as permissões disponíveis no sistema
        </p>
      </div>

      {isLoading ? (
        <div className="flex h-32 items-center justify-center">
          <p className="text-muted-foreground">Carregando permissões...</p>
        </div>
      ) : (
        <PermissionList permissions={permissions || []} />
      )}
    </div>
  );
}
