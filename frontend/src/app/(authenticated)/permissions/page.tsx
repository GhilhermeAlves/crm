"use client";

import { KeyRound } from "lucide-react";
import { PermissionList } from "@/features/rbac/components/PermissionList";
import { usePermissions } from "@/features/rbac/hooks/useRoles";

export default function PermissionsPage() {
  const { data: permissions, isLoading } = usePermissions();

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold flex items-center gap-2">
          <KeyRound className="h-6 w-6" />
          Permissões
        </h1>
        <p className="text-muted-foreground">
          Visualize todas as permissões disponíveis no sistema
        </p>
      </div>

      {isLoading ? (
        <div className="flex items-center justify-center h-32">
          <p className="text-muted-foreground">Carregando permissões...</p>
        </div>
      ) : (
        <PermissionList permissions={permissions || []} />
      )}
    </div>
  );
}
