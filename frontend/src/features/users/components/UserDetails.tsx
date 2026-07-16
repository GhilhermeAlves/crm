"use client";

import type { User } from "../types/user.types";
import { UserAvatar } from "./UserAvatar";
import { UserStatusBadge } from "./UserStatusBadge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

interface UserDetailsProps {
  user: User;
}

function InfoRow({ label, value }: { label: string; value: string | null | undefined }) {
  return (
    <div className="flex justify-between py-2 border-b last:border-b-0">
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
