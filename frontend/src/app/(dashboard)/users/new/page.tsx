"use client";

import { useRouter } from "next/navigation";
import { useCreateUser } from "@/features/users/hooks/useUsers";
import { UserForm } from "@/features/users/components/UserForm";
import { PageTitle } from "@/components/common/PageTitle";
import type { CreateUserRequest, UpdateUserRequest } from "@/features/users/types/user.types";
import { ROUTES } from "@/lib/constants";

export default function NewUserPage() {
  const router = useRouter();
  const createUser = useCreateUser();

  const handleSubmit = (data: CreateUserRequest | UpdateUserRequest) => {
    createUser.mutate(data as CreateUserRequest, {
      onSuccess: () => router.push(ROUTES.USERS),
    });
  };

  return (
    <div className="space-y-6">
      <PageTitle>Novo Usuário</PageTitle>
      <UserForm
        mode="create"
        onSubmit={handleSubmit}
        onCancel={() => router.back()}
        isLoading={createUser.isPending}
      />
    </div>
  );
}
