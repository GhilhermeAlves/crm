"use client";

import { useAuth } from "@/features/auth/hooks/useAuth";
import { useInvitations, useRevokeInvitation } from "@/features/invitations/hooks/useInvitations";
import { CreateInvitationDialog } from "@/features/invitations/components/CreateInvitationDialog";
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
import { XCircle } from "lucide-react";
import { format } from "date-fns";
import { ptBR } from "date-fns/locale";

const STATUS_LABEL: Record<string, { label: string; variant: "default" | "secondary" | "destructive" | "outline" }> = {
  PENDING: { label: "Pendente", variant: "secondary" },
  ACCEPTED: { label: "Aceito", variant: "default" },
  REVOKED: { label: "Revogado", variant: "destructive" },
  EXPIRED: { label: "Expirado", variant: "outline" },
};

export default function InvitationsPage() {
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;

  const invitationsQuery = useInvitations(companyId);
  const revoke = useRevokeInvitation(companyId ?? "");

  if (invitationsQuery.isLoading) {
    return <Skeleton className="h-64 w-full" />;
  }

  const invitations = invitationsQuery.data ?? [];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <PageTitle>Convites</PageTitle>
          <p className="text-sm text-muted-foreground">
            Convite membros por e-mail e acompanhe o status de cada convite.
          </p>
        </div>
        {companyId && <CreateInvitationDialog companyId={companyId} />}
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Convites de empresa</CardTitle>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>E-mail</TableHead>
                <TableHead>Papel</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Expira em</TableHead>
                <TableHead className="text-right">Ações</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {invitations.length === 0 ? (
                <TableRow>
                  <TableCell
                    colSpan={5}
                    className="h-24 text-center text-muted-foreground"
                  >
                    Nenhum convite encontrado.
                  </TableCell>
                </TableRow>
              ) : (
                invitations.map((invitation) => {
                  const status = STATUS_LABEL[invitation.status] ?? {
                    label: invitation.status,
                    variant: "secondary",
                  };
                  return (
                    <TableRow key={invitation.id}>
                      <TableCell>{invitation.email}</TableCell>
                      <TableCell>
                        <Badge variant="outline">{invitation.role}</Badge>
                      </TableCell>
                      <TableCell>
                        <Badge variant={status.variant}>{status.label}</Badge>
                      </TableCell>
                      <TableCell>
                        {format(new Date(invitation.expiresAt), "dd/MM/yyyy HH:mm", {
                          locale: ptBR,
                        })}
                      </TableCell>
                      <TableCell className="text-right">
                        {invitation.status === "PENDING" && (
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => revoke.mutate(invitation.id)}
                            disabled={revoke.isPending}
                          >
                            <XCircle className="mr-1 h-4 w-4" /> Revogar
                          </Button>
                        )}
                      </TableCell>
                    </TableRow>
                  );
                })
              )}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </div>
  );
}