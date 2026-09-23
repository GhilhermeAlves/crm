"use client";

import { useParams, useRouter } from "next/navigation";
import { useUser } from "@/features/users/hooks/useUsers";
import { UserDetails } from "@/features/users/components/UserDetails";
import { PageTitle } from "@/components/common/PageTitle";
import { Button } from "@/components/ui/button";
import { Pencil } from "lucide-react";
import { ROUTES } from "@/lib/constants";
import { SkeletonCard } from "@/components/feedback/SkeletonCard";

export default function UserDetailPage() {
  const params = useParams();
  const router = useRouter();
  const id = params.id as string;

  const { data: user, isLoading } = useUser(id);

  if (isLoading) {
    return (
      <div className="space-y-6">
        <SkeletonCard />
      </div>
    );
  }

  if (!user) {
    return (
      <div className="py-12 text-center">
        <p className="text-muted-foreground">Usuário não encontrado.</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <PageTitle>{user.name}</PageTitle>
        <Button onClick={() => router.push(`${ROUTES.USERS}/${id}/edit`)}>
          <Pencil className="mr-2 h-4 w-4" />
          Editar
        </Button>
      </div>
      <UserDetails user={user} />
    </div>
  );
}
