"use client";

import { useState } from "react";
import Link from "next/link";
import { MoreHorizontal, Pencil, Trash2, Eye, Shield, ShieldOff } from "lucide-react";
import { DataTable } from "@/components/ui/data-table";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { RoleBadge } from "./RoleBadge";
import type { Role } from "../types/rbac.types";
import { DeleteRoleDialog } from "./DeleteRoleDialog";
import { useAuthorization } from "@/features/identity/auth/hooks/useAuthorization";

interface RoleTableProps {
  roles: Role[];
}

export function RoleTable({ roles }: RoleTableProps) {
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [selectedRole, setSelectedRole] = useState<Role | null>(null);
  const { can } = useAuthorization();
  const canManage = can("role:manage");

  const handleDelete = (role: Role) => {
    setSelectedRole(role);
    setDeleteDialogOpen(true);
  };

  return (
    <>
      <DataTable
        tableId="roles"
        data={roles ?? []}
        emptyState={{ title: "Nenhuma role encontrada." }}
        columns={[
          {
            id: "nome",
            header: "Nome",
            cell: (role) => (
              <Link href={`/settings/roles/${role.id}`} className="font-medium hover:underline">
                {role.name.replace(/_/g, " ")}
              </Link>
            ),
          },
          {
            id: "descricao",
            header: "Descrição",
            className: "max-w-[200px] truncate text-muted-foreground",
            cell: (role) => <>{role.description || "-"}</>,
          },
          {
            id: "permissoes",
            header: "Permissões",
            cell: (role) => (
              <span className="text-sm text-muted-foreground">
                {role.permissions.length} permissão(ões)
              </span>
            ),
          },
          {
            id: "tipo",
            header: "Tipo",
            cell: (role) => <RoleBadge name={role.name} isSystem={role.isSystem} />,
          },
          {
            id: "status",
            header: "Status",
            cell: (role) => (
              <>
                {role.isActive ? (
                  <span className="inline-flex items-center gap-1 text-sm text-green-600">
                    <Shield className="h-3 w-3" /> Ativo
                  </span>
                ) : (
                  <span className="inline-flex items-center gap-1 text-sm text-gray-500">
                    <ShieldOff className="h-3 w-3" /> Inativo
                  </span>
                )}
              </>
            ),
          },
          {
            id: "acoes",
            header: "",
            label: "Ações",
            hideable: false,
            headerClassName: "w-[50px]",
            cell: (role) => (
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button variant="ghost" size="icon" className="h-8 w-8">
                    <MoreHorizontal className="h-4 w-4" />
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end">
                  <DropdownMenuItem asChild>
                    <Link href={`/settings/roles/${role.id}`}>
                      <Eye className="mr-2 h-4 w-4" />
                      Visualizar
                    </Link>
                  </DropdownMenuItem>
                  {canManage && !role.isSystem && (
                    <>
                      <DropdownMenuItem asChild>
                        <Link href={`/settings/roles/${role.id}/edit`}>
                          <Pencil className="mr-2 h-4 w-4" />
                          Editar
                        </Link>
                      </DropdownMenuItem>
                      <DropdownMenuSeparator />
                      <DropdownMenuItem
                        onClick={() => handleDelete(role)}
                        className="text-destructive"
                      >
                        <Trash2 className="mr-2 h-4 w-4" />
                        Excluir
                      </DropdownMenuItem>
                    </>
                  )}
                </DropdownMenuContent>
              </DropdownMenu>
            ),
          },
        ]}
        keyExtractor={(role) => role.id}
      />

      {selectedRole && (
        <DeleteRoleDialog
          open={deleteDialogOpen}
          onOpenChange={setDeleteDialogOpen}
          role={selectedRole}
        />
      )}
    </>
  );
}
