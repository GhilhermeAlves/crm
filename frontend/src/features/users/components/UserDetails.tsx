"use client";

import { useState } from "react";
import type { User } from "../types/user.types";
import { UserAvatar } from "./UserAvatar";
import { UserStatusBadge } from "./UserStatusBadge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  useRoles,
  useUserRoles,
  useAssignRoleToUser,
  useRemoveRoleFromUser,
} from "@/features/rbac/hooks/useRoles";

interface UserDetailsProps {
  user: User;
}

function InfoRow({ label, value }: { label: string; value: string | null | undefined }) {
  return (
    <div className="flex justify-between border-b py-2 last:border-b-0">
      <span className="text-sm text-muted-foreground">{label}</span>
      <span className="text-sm font-medium">{value || "—"}</span>
    </div>
  );
}

function formatDate(dateStr: string | null): string {
  if (!dateStr) return "—";
  return new Date(dateStr).toLocaleDateString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function UserDetails({ user }: UserDetailsProps) {
  const rolesQuery = useRoles();
  const userRolesQuery = useUserRoles(user.id);
  const assignRole = useAssignRoleToUser();
  const removeRole = useRemoveRoleFromUser();
  const [selectedRoleId, setSelectedRoleId] = useState("");

  const availableRoles = (rolesQuery.data ?? []).filter(
    (r) => !userRolesQuery.data?.some((ur) => ur.id === r.id),
  );

  const handleAssign = () => {
    if (!selectedRoleId) return;
    assignRole.mutate(
      { userId: user.id, data: { roleId: selectedRoleId } },
      { onSuccess: () => setSelectedRoleId("") },
    );
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <Card>
        <CardContent className="pt-6">
          <div className="flex items-center gap-4">
            <UserAvatar
              firstName={user.firstName}
              lastName={user.lastName}
              avatarUrl={user.avatarUrl}
              size="lg"
            />
            <div>
              <h2 className="text-xl font-bold">{user.name}</h2>
              <p className="text-muted-foreground">{user.email}</p>
              <div className="mt-1">
                <UserStatusBadge status={user.status} />
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Personal Data */}
      <Card>
        <CardHeader>
          <CardTitle>Dados Pessoais</CardTitle>
        </CardHeader>
        <CardContent>
          <InfoRow label="Nome" value={user.firstName} />
          <InfoRow label="Sobrenome" value={user.lastName} />
          <InfoRow label="Email" value={user.email} />
          <InfoRow label="Telefone" value={user.phone} />
          <InfoRow label="Idioma" value={user.language} />
          <InfoRow label="Fuso Horário" value={user.timezone} />
        </CardContent>
      </Card>

      {/* Professional Data */}
      <Card>
        <CardHeader>
          <CardTitle>Dados Profissionais</CardTitle>
        </CardHeader>
        <CardContent>
          <InfoRow label="Cargo" value={user.jobTitle} />
          <InfoRow label="Departamento" value={user.department} />
        </CardContent>
      </Card>

      {/* Papéis */}
      <Card>
        <CardHeader>
          <CardTitle>Papéis</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex flex-wrap gap-2">
            {userRolesQuery.data?.length ? (
              userRolesQuery.data.map((role) => (
                <div key={role.id} className="flex items-center gap-1 rounded-md border px-2 py-1">
                  <Badge variant="secondary">{role.name.replace(/_/g, " ")}</Badge>
                  <Button
                    variant="ghost"
                    size="sm"
                    className="h-5 w-5 p-0 text-muted-foreground"
                    disabled={removeRole.isPending}
                    onClick={() => removeRole.mutate({ userId: user.id, roleId: role.id })}
                  >
                    ✕
                  </Button>
                </div>
              ))
            ) : (
              <p className="text-sm text-muted-foreground">Nenhum papel atribuído.</p>
            )}
          </div>

          {availableRoles.length > 0 && (
            <div className="flex items-center gap-2">
              <Select value={selectedRoleId} onValueChange={setSelectedRoleId}>
                <SelectTrigger className="w-56">
                  <SelectValue placeholder="Selecionar papel..." />
                </SelectTrigger>
                <SelectContent>
                  {availableRoles.map((r) => (
                    <SelectItem key={r.id} value={r.id}>
                      {r.name.replace(/_/g, " ")}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <Button disabled={!selectedRoleId || assignRole.isPending} onClick={handleAssign}>
                {assignRole.isPending ? "Atribuindo..." : "Atribuir"}
              </Button>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Metadata */}
      <Card>
        <CardHeader>
          <CardTitle>Metadados</CardTitle>
        </CardHeader>
        <CardContent>
          <InfoRow label="Criado em" value={formatDate(user.createdAt)} />
          <InfoRow label="Atualizado em" value={formatDate(user.updatedAt)} />
          <InfoRow label="Último Login" value={formatDate(user.lastLoginAt)} />
          <InfoRow label="Observações" value={user.notes} />
        </CardContent>
      </Card>
    </div>
  );
}
