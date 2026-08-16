"use client";

import { useAuth } from "@/features/auth/hooks/useAuth";
import { useAuthorization } from "@/features/auth/hooks/useAuthorization";
import {
  useMembers,
  useUpdateMemberRole,
  useRemoveMember,
} from "@/features/members/hooks/useMembers";
import { useRoles } from "@/features/rbac/hooks/useRoles";
import { InviteMemberDialog } from "@/features/members/components/InviteMemberDialog";
import { PageTitle } from "@/components/common/PageTitle";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Trash2 } from "lucide-react";
import { format } from "date-fns";
import { ptBR } from "date-fns/locale";

/**
 * Sprint 9 — Administração de usuários da EMPRESA ATIVA.
 *
 * Lista, altera perfil/role e remove/desativa membros. Gestão restrita à
 * própria empresa ativa (backend valida; UI espelha via permissions
 * membership:view / membership:manage). Não permite tocar em outra empresa.
 */
export default function SettingsUsersPage() {
  const { user } = useAuth();
  const { can } = useAuthorization();
  const companyId = user?.companyId ?? null;

  const membersQuery = useMembers(companyId);
  const rolesQuery = useRoles();
  const updateRole = useUpdateMemberRole(companyId ?? "");
  const removeMember = useRemoveMember(companyId ?? "");

  if (membersQuery.isLoading) {
    return <Skeleton className="h-64 w-full" />;
  }

  const members = membersQuery.data ?? [];
  const manageUsers = can("membership:manage");

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <PageTitle>Usuários</PageTitle>
          <p className="text-sm text-muted-foreground">Membros e papéis de acesso desta empresa.</p>
        </div>
        {manageUsers && <InviteMemberDialog />}
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Membros da empresa</CardTitle>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Nome</TableHead>
                <TableHead>E-mail</TableHead>
                <TableHead>Perfil / Papel</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Entrou em</TableHead>
                <TableHead className="text-right">Ações</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {members.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} className="h-24 text-center text-muted-foreground">
                    Nenhum membro encontrado.
                  </TableCell>
                </TableRow>
              ) : (
                members.map((member) => (
                  <TableRow key={member.userId}>
                    <TableCell className="font-medium">{member.name}</TableCell>
                    <TableCell>{member.email}</TableCell>
                    <TableCell>
                      {manageUsers ? (
                        <select
                          value={member.role}
                          onChange={(e) =>
                            updateRole.mutate({
                              userId: member.userId,
                              role: e.target.value,
                            })
                          }
                          className="rounded border bg-background px-2 py-1 text-sm"
                          disabled={updateRole.isPending}
                        >
                          {rolesQuery.data?.map((role) => (
                            <option key={role.id} value={role.name}>
                              {role.name}
                            </option>
                          ))}
                        </select>
                      ) : (
                        <Badge variant="secondary">{member.role}</Badge>
                      )}
                    </TableCell>
                    <TableCell>
                      <Badge variant={member.status === "ACTIVE" ? "default" : "secondary"}>
                        {member.status === "ACTIVE" ? "Ativo" : "Pendente"}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-muted-foreground">
                      {member.joinedAt
                        ? format(new Date(member.joinedAt), "dd/MM/yyyy", {
                            locale: ptBR,
                          })
                        : "-"}
                    </TableCell>
                    <TableCell className="text-right">
                      {manageUsers && (
                        <Button
                          variant="ghost"
                          size="icon"
                          title="Remover membro"
                          onClick={() => removeMember.mutate(member.userId)}
                          disabled={removeMember.isPending}
                        >
                          <Trash2 className="h-4 w-4 text-destructive" />
                        </Button>
                      )}
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </div>
  );
}
