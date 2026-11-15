"use client";

import { ListPagination } from "@/components/common/ListPagination";
import { DEFAULT_PAGE_SIZE } from "@/types/page";
import { useState, useCallback } from "react";
import { useRouter } from "next/navigation";
import {
  useUsers,
  useDeleteUser,
  useActivateUser,
  useDeactivateUser,
} from "@/features/identity/users/hooks/useUsers";
import { UserTable } from "@/features/identity/users/components/UserTable";
import { UserFilters } from "@/features/identity/users/components/UserFilters";
import { DeleteUserDialog } from "@/features/identity/users/components/DeleteUserDialog";
import { PageTitle } from "@/components/common/PageTitle";
import { Button } from "@/components/ui/button";
import { Plus } from "lucide-react";
import type { User, UserStatus } from "@/features/identity/users/types/user.types";
import { ROUTES } from "@/lib/constants";

export default function UsersPage() {
  const router = useRouter();
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState("all");
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);
  const [deleteUser, setDeleteUser] = useState<User | null>(null);

  const { data, isLoading, refetch } = useUsers({
    page,
    pageSize,
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

      {data && (
        <ListPagination
          page={page}
          pageSize={pageSize}
          totalPages={data.totalPages}
          totalElements={data.totalElements}
          onPageChange={setPage}
          onPageSizeChange={(size) => {
            setPageSize(size);
            setPage(0);
          }}
        />
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
