"use client";

import Link from "next/link";
import { Plus, Shield } from "lucide-react";
import { Button } from "@/components/ui/button";
import { RoleTable } from "@/features/rbac/components/RoleTable";
import { useRoles } from "@/features/rbac/hooks/useRoles";
import { useAuthorization } from "@/features/auth/hooks/useAuthorization";

export default function RolesPage() {
  const { data: roles, isLoading } = useRoles();
  const { can } = useAuthorization();
  const canManage = can("role:manage");

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="flex items-center gap-2 text-2xl font-bold">
            <Shield className="h-6 w-6" />
            Roles
          </h1>
          <p className="text-muted-foreground">Gerencie as roles e permissões do sistema</p>
        </div>
        {canManage && (
          <Button asChild>
            <Link href="/roles/new">
              <Plus className="mr-2 h-4 w-4" />
              Nova Role
            </Link>
          </Button>
        )}
      </div>

      {isLoading ? (
        <div className="flex h-32 items-center justify-center">
          <p className="text-muted-foreground">Carregando roles...</p>
        </div>
      ) : (
        <RoleTable roles={roles || []} />
      )}
    </div>
  );
}
