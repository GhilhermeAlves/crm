"use client";

import { useParams, useRouter } from "next/navigation";
import { useUser, useUpdateUser } from "@/features/users/hooks/useUsers";
import { UserForm } from "@/features/users/components/UserForm";
import { PageTitle } from "@/components/common/PageTitle";
import type { UpdateUserRequest } from "@/features/users/types/user.types";
import { ROUTES } from "@/lib/constants";
import { SkeletonForm } from "@/components/feedback/SkeletonForm";

export default function EditUserPage() {
  const params = useParams();
  const router = useRouter();
  const id = params.id as string;

  const { data: user, isLoading } = useUser(id);
  const updateUser = useUpdateUser();

  const handleSubmit = (data: any) => {
    updateUser.mutate({ id, data: data as UpdateUserRequest }, {
      onSuccess: () => router.push(`${ROUTES.USERS}/${id}`),
    });
  };

  if (isLoading) {
    return (
      <div className="space-y-6">
        <SkeletonForm />
      </div>
    );
  }

  if (!user) {
    return (
      <div className="text-center py-12">
        <p className="text-muted-foreground">Usuário não encontrado.</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <PageTitle>Editar Usuário</PageTitle>
      <UserForm
        user={user}
        mode="edit"
        onSubmit={handleSubmit}
        onCancel={() => router.back()}
        isLoading={updateUser.isPending}
      />
    </div>
  );
}
