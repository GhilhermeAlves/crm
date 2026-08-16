"use client";

import { useState } from "react";
import Link from "next/link";
import {
  MoreHorizontal,
  Pencil,
  Trash2,
  Eye,
  Shield,
  ShieldOff,
} from "lucide-react";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
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

interface RoleTableProps {
  roles: Role[];
}

export function RoleTable({ roles }: RoleTableProps) {
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [selectedRole, setSelectedRole] = useState<Role | null>(null);

  const handleDelete = (role: Role) => {
    setSelectedRole(role);
    setDeleteDialogOpen(true);
  };

  return (
    <>
      <div className="rounded-md border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Nome</TableHead>
              <TableHead>Descrição</TableHead>
              <TableHead>Permissões</TableHead>
              <TableHead>Tipo</TableHead>
              <TableHead>Status</TableHead>
              <TableHead className="w-[50px]"></TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {roles.length === 0 ? (
              <TableRow>
                <TableCell
                  colSpan={6}
                  className="h-24 text-center text-muted-foreground"
                >
                  Nenhuma role encontrada.
                </TableCell>
              </TableRow>
            ) : (
              roles.map((role) => (
                <TableRow key={role.id}>
                  <TableCell>
                    <Link
                      href={`/roles/${role.id}`}
                      className="font-medium hover:underline"
                    >
                      {role.name.replace(/_/g, " ")}
                    </Link>
                  </TableCell>
                  <TableCell className="text-muted-foreground max-w-[200px] truncate">
                    {role.description || "-"}
                  </TableCell>
                  <TableCell>
                    <span className="text-sm text-muted-foreground">
                      {role.permissions.length} permissão(ões)
                    </span>
                  </TableCell>
                  <TableCell>
                    <RoleBadge name={role.name} isSystem={role.isSystem} />
                  </TableCell>
                  <TableCell>
                    {role.isActive ? (
                      <span className="inline-flex items-center gap-1 text-sm text-green-600">
                        <Shield className="h-3 w-3" /> Ativo
                      </span>
                    ) : (
                      <span className="inline-flex items-center gap-1 text-sm text-gray-500">
                        <ShieldOff className="h-3 w-3" /> Inativo
                      </span>
                    )}
                  </TableCell>
                  <TableCell>
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button variant="ghost" size="icon" className="h-8 w-8">
                          <MoreHorizontal className="h-4 w-4" />
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end">
                        <DropdownMenuItem asChild>
                          <Link href={`/roles/${role.id}`}>
                            <Eye className="mr-2 h-4 w-4" />
                            Visualizar
                          </Link>
                        </DropdownMenuItem>
                        {!role.isSystem && (
                          <>
                            <DropdownMenuItem asChild>
                              <Link href={`/roles/${role.id}/edit`}>
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
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

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
