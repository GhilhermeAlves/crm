"use client";

import { useState } from "react";
import { RefreshCw, Trash2 } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { SEGMENT_OPTIONS } from "@/features/identity/modules/types/modules.types";
import { InvitationLinkPanel } from "@/features/identity/invitations/components/InvitationLinkPanel";
import {
  useCompanyInvitations,
  useRegenerateCompanyInvitation,
  useRevokeCompanyInvitation,
} from "../hooks";
import type { CompanyInvitation, CompanyInvitationLink } from "../types";

const STATUS_LABEL: Record<string, string> = {
  PENDING: "Pendente",
  ACCEPTED: "Aceito",
  EXPIRED: "Expirado",
  REVOKED: "Revogado",
};

const SEGMENT_LABEL = new Map(SEGMENT_OPTIONS.map((option) => [option.value, option.label]));

/** Convites de criação de empresa enviados pelo CRM_ADMIN (tela de Empresas). */
export function CompanyInvitationsList() {
  const { data: invitations } = useCompanyInvitations(true);
  const regenerate = useRegenerateCompanyInvitation();
  const revoke = useRevokeCompanyInvitation();
  const [link, setLink] = useState<CompanyInvitationLink | null>(null);
  // Gerar link novo invalida o anterior (inclusive o do e-mail): sempre confirma antes.
  const [confirming, setConfirming] = useState<CompanyInvitation | null>(null);

  function regenerateLink(send: boolean) {
    if (!confirming) return;
    regenerate.mutate(
      { id: confirming.id, send },
      {
        onSuccess: (result) => {
          setConfirming(null);
          setLink(result);
        },
      },
    );
  }

  if (!invitations || invitations.length === 0) return null;

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">Convites de empresa</CardTitle>
      </CardHeader>
      <CardContent className="space-y-2">
        {invitations.map((invitation) => {
          const usable = invitation.status === "PENDING" || invitation.status === "EXPIRED";
          return (
            <div
              key={invitation.id}
              className="flex flex-col gap-2 rounded-md border p-3 sm:flex-row sm:items-center sm:justify-between"
            >
              <div className="min-w-0 space-y-1">
                <p className="break-all text-sm font-medium">{invitation.email}</p>
                <p className="text-xs text-muted-foreground">
                  {SEGMENT_LABEL.get(invitation.segment as never) ?? invitation.segment} ·{" "}
                  {invitation.plan} · até {invitation.maxUsers} usuários ·{" "}
                  {invitation.enabledModules.length} módulos
                </p>
              </div>
              <div className="flex items-center gap-2">
                <Badge variant={invitation.status === "PENDING" ? "default" : "secondary"}>
                  {STATUS_LABEL[invitation.status] ?? invitation.status}
                </Badge>
                {usable && (
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-8 w-8"
                    title="Gerar novo link (o link atual deixa de valer)"
                    aria-label="Gerar novo link"
                    disabled={regenerate.isPending}
                    onClick={() => setConfirming(invitation)}
                  >
                    <RefreshCw className="h-4 w-4" />
                  </Button>
                )}
                {invitation.status === "PENDING" && (
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-8 w-8 text-destructive"
                    title="Revogar convite"
                    disabled={revoke.isPending}
                    onClick={() => revoke.mutate(invitation.id)}
                  >
                    <Trash2 className="h-4 w-4" />
                  </Button>
                )}
              </div>
            </div>
          );
        })}
      </CardContent>

      <Dialog open={!!confirming} onOpenChange={(open) => !open && setConfirming(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Gerar novo link?</DialogTitle>
            <DialogDescription>
              O link atual de {confirming?.email} deixa de funcionar, inclusive o que já foi enviado
              por e-mail. Use só se o convidado perdeu o link ou ele expirou.
            </DialogDescription>
          </DialogHeader>
          <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
            <Button variant="ghost" onClick={() => setConfirming(null)}>
              Cancelar
            </Button>
            <Button
              variant="outline"
              disabled={regenerate.isPending}
              onClick={() => regenerateLink(false)}
            >
              Só gerar o link
            </Button>
            <Button disabled={regenerate.isPending} onClick={() => regenerateLink(true)}>
              Gerar e reenviar por e-mail
            </Button>
          </div>
        </DialogContent>
      </Dialog>

      <Dialog open={!!link} onOpenChange={(open) => !open && setLink(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Novo link do convite</DialogTitle>
            <DialogDescription>O link anterior deixou de valer.</DialogDescription>
          </DialogHeader>
          {link && (
            <InvitationLinkPanel
              link={link}
              companyName="sua nova empresa"
              onClose={() => setLink(null)}
            />
          )}
        </DialogContent>
      </Dialog>
    </Card>
  );
}
