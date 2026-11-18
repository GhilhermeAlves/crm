"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { MoreHorizontal, Eye, Pencil, Trash2, UserCheck, UserX, Mail } from "lucide-react";
import type { User } from "../types/user.types";
import { UserAvatar } from "./UserAvatar";
import { UserStatusBadge } from "./UserStatusBadge";
import { DataTable } from "@/components/ui/data-table";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { ROUTES } from "@/lib/constants";

interface UserTableProps {
  users: User[];
  isLoading?: boolean;
  onDelete?: (user: User) => void;
  onActivate?: (user: User) => void;
  onDeactivate?: (user: User) => void;
}

function formatDate(dateStr: string | null): string {
  if (!dateStr) return "—";
  return new Date(dateStr).toLocaleDateString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });
}

function formatDateTime(dateStr: string | null): string {
  if (!dateStr) return "Nunca";
  return new Date(dateStr).toLocaleDateString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function UserTable({
  users,
  isLoading,
  onDelete,
  onActivate,
  onDeactivate,
}: UserTableProps) {
  const router = useRouter();

  if (isLoading) {
    return (
      <div className="space-y-3">
        {Array.from({ length: 5 }).map((_, i) => (
          <div key={i} className="h-16 animate-pulse rounded bg-muted" />
        ))}
      </div>
    );
  }

  if (users.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-12 text-muted-foreground">
        <Mail className="mb-4 h-12 w-12 opacity-50" />
        <p className="text-lg font-medium">Nenhum usuário encontrado</p>
        <p className="text-sm">Cadastre ou convide novos usuários para começar.</p>
      </div>
    );
  }

  return (
    <DataTable
      tableId="users"
      data={users ?? []}
      columns={[
        {
          id: "usuario",
          header: "Usuário",
          headerClassName: "w-[300px]",
          cell: (user) => (
            <div className="flex items-center gap-3">
              <UserAvatar
                firstName={user.firstName}
                lastName={user.lastName}
                avatarUrl={user.avatarUrl}
                size="sm"
              />
              <div>
                <p className="font-medium">{user.name}</p>
                <p className="text-sm text-muted-foreground">{user.email}</p>
              </div>
            </div>
          ),
        },
        {
          id: "cargo",
          header: "Cargo",
          className: "text-sm",
          cell: (user) => <>{user.jobTitle || "—"}</>,
        },
        {
          id: "empresa",
          header: "Empresa",
          className: "text-sm text-muted-foreground",
          cell: (user) => <>—</>,
        },
        {
          id: "status",
          header: "Status",
          cell: (user) => <UserStatusBadge status={user.status} />,
        },
        {
          id: "ultimo-login",
          header: "Último Login",
          className: "text-sm text-muted-foreground",
          sortValue: (user) => user.lastLoginAt,
          cell: (user) => <>{formatDateTime(user.lastLoginAt)}</>,
        },
        {
          id: "criado-em",
          header: "Criado em",
          className: "text-sm text-muted-foreground",
          sortValue: (user) => user.createdAt,
          cell: (user) => <>{formatDate(user.createdAt)}</>,
        },
        {
          id: "acoes",
          header: "",
          label: "Ações",
          hideable: false,
          headerClassName: "w-[60px]",
          cell: (user) => (
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="ghost" size="icon" className="h-8 w-8">
                  <MoreHorizontal className="h-4 w-4" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end">
                <DropdownMenuItem onClick={() => router.push(`${ROUTES.USERS}/${user.id}`)}>
                  <Eye className="mr-2 h-4 w-4" />
                  Visualizar
                </DropdownMenuItem>
                <DropdownMenuItem onClick={() => router.push(`${ROUTES.USERS}/${user.id}/edit`)}>
                  <Pencil className="mr-2 h-4 w-4" />
                  Editar
                </DropdownMenuItem>
                <DropdownMenuSeparator />
                {user.status === "active" ? (
                  <DropdownMenuItem
                    onClick={() => onDeactivate?.(user)}
                    className="text-destructive"
                  >
                    <UserX className="mr-2 h-4 w-4" />
                    Desativar
                  </DropdownMenuItem>
                ) : (
                  <DropdownMenuItem onClick={() => onActivate?.(user)} className="text-green-600">
                    <UserCheck className="mr-2 h-4 w-4" />
                    Ativar
                  </DropdownMenuItem>
                )}
                <DropdownMenuSeparator />
                <DropdownMenuItem onClick={() => onDelete?.(user)} className="text-destructive">
                  <Trash2 className="mr-2 h-4 w-4" />
                  Excluir
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          ),
        },
      ]}
      keyExtractor={(user) => user.id}
    />
  );
}
