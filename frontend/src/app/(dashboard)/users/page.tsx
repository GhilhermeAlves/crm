"use client";

import { useState, useCallback } from "react";
import { useRouter } from "next/navigation";
import {
  useUsers,
  useDeleteUser,
  useActivateUser,
  useDeactivateUser,
} from "@/features/users/hooks/useUsers";
import { UserTable } from "@/features/users/components/UserTable";
import { UserFilters } from "@/features/users/components/UserFilters";
import { DeleteUserDialog } from "@/features/users/components/DeleteUserDialog";
import { PageTitle } from "@/components/common/PageTitle";
import { Button } from "@/components/ui/button";
import { Plus } from "lucide-react";
import type { User, UserStatus } from "@/features/users/types/user.types";
import { ROUTES } from "@/lib/constants";

export default function UsersPage() {
  const router = useRouter();
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState("all");
  const [page, setPage] = useState(0);
  const [deleteUser, setDeleteUser] = useState<User | null>(null);

  const { data, isLoading, refetch } = useUsers({
    page,
    pageSize: 10,
    search: search || undefined,
    status: status !== "all" ? (status as UserStatus) : undefined,
    sortBy: "createdAt",
    sortDirection: "desc",
  });

  const deleteUserMutation = useDeleteUser();
  const activateUserMutation = useActivateUser();
  const deactivateUserMutation = useDeactivateUser();

  const handleDelete = useCallback(() => {
    if (deleteUser) {
      deleteUserMutation.mutate(deleteUser.id, {
        onSuccess: () => setDeleteUser(null),
      });
    }
  }, [deleteUser, deleteUserMutation]);

  const handleActivate = useCallback(
    (user: User) => activateUserMutation.mutate(user.id),
    [activateUserMutation],
  );

  const handleDeactivate = useCallback(
    (user: User) => deactivateUserMutation.mutate(user.id),
    [deactivateUserMutation],
  );

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <PageTitle>Usuários</PageTitle>
        <Button onClick={() => router.push(ROUTES.USERS_NEW)}>
          <Plus className="mr-2 h-4 w-4" />
          Novo Usuário
        </Button>
      </div>

      <UserFilters
        search={search}
        status={status}
        onSearchChange={(val) => {
          setSearch(val);
          setPage(0);
        }}
        onStatusChange={(val) => {
          setStatus(val);
          setPage(0);
        }}
        onRefresh={() => refetch()}
      />

      <UserTable
        users={data?.content || []}
        isLoading={isLoading}
        onDelete={setDeleteUser}
        onActivate={handleActivate}
        onDeactivate={handleDeactivate}
      />

      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between">
          <p className="text-sm text-muted-foreground">
            Mostrando {data.content.length} de {data.totalElements} usuários
          </p>
          <div className="flex gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0}
            >
              Anterior
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setPage((p) => Math.min(data.totalPages - 1, p + 1))}
              disabled={page >= data.totalPages - 1}
            >
              Próximo
            </Button>
          </div>
        </div>
      )}

      <DeleteUserDialog
        user={deleteUser}
        open={!!deleteUser}
        onOpenChange={(open) => !open && setDeleteUser(null)}
        onConfirm={handleDelete}
        isLoading={deleteUserMutation.isPending}
      />
    </div>
  );
}
