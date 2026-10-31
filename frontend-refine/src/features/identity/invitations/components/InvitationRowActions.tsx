"use client";

import { useState } from "react";
import { format } from "date-fns";
import { ptBR } from "date-fns/locale";
import { Copy, Mail, MessageCircle, MoreHorizontal, XCircle } from "lucide-react";
import { toast } from "sonner";

import { usePermission } from "@/features/identity/auth/hooks/useAuthorization";
import {
  useRegenerateInvitation,
  useRevokeInvitation,
} from "@/features/identity/invitations/hooks/useInvitations";
import {
  isUsableInvitation,
  invitationStatusMeta,
} from "@/features/identity/invitations/lib/invitation-status";
import { getInvitationLink } from "@/features/identity/invitations/lib/invitation-link-cache";
import {
  copyInvitationLink,
  openInvitationWhatsApp,
} from "@/features/identity/invitations/lib/invitation-share";
import { roleLabel } from "@/features/identity/members/lib/labels";
import type { Invitation } from "@/features/identity/invitations/types/invitation.types";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";

type RegenerateMode = "send" | "copy";

type InvitationRowActionsProps = {
  companyId: string;
  companyName?: string | null;
  invitation: Invitation;
};

export function InvitationRowActions({
  companyId,
  companyName,
  invitation,
}: InvitationRowActionsProps) {
  const can = usePermission();
  const canInvite = can("user:invite");
  const canDelete = can("user:delete");

  const regenerate = useRegenerateInvitation(companyId);
  const revoke = useRevokeInvitation(companyId);

  const [mode, setMode] = useState<RegenerateMode | null>(null);
  const [confirmRevoke, setConfirmRevoke] = useState(false);
  const [noLinkOpen, setNoLinkOpen] = useState(false);
  const [whatsAppAfterCopy, setWhatsAppAfterCopy] = useState(false);

  const isExpired = invitation.status === "EXPIRED";
  const canRegenerate = canInvite && isUsableInvitation(invitation.status);
  const canRevoke = canDelete && invitation.status === "PENDING";

  if (!canRegenerate && !canRevoke) return null;

  function sendByWhatsApp() {
    const link = getInvitationLink(invitation.id);
    if (!link) {
      setNoLinkOpen(true);
      return;
    }
    openInvitationWhatsApp(link, companyName);
  }

  async function runRegenerate() {
    if (!mode) return;
    const send = mode === "send";
    const wasExpired = isExpired;

    try {
      const link = await regenerate.mutateAsync({ invitationId: invitation.id, send });
      setMode(null);

      if (send) {
        toast.success(`Convite reenviado para ${invitation.email}.`);
      } else {
        await copyInvitationLink(link.url);
        if (whatsAppAfterCopy) {
          setWhatsAppAfterCopy(false);
          openInvitationWhatsApp(link.url, companyName);
        }
      }

      if (wasExpired) {
        toast.warning(
          `O convite estava expirado. O novo link vale até ${format(
            new Date(link.invitation.expiresAt),
            "dd/MM/yyyy HH:mm",
            { locale: ptBR },
          )}.`,
        );
      }
    } catch {
      // O onError do hook já exibiu a mensagem; o dialog permanece aberto.
      setWhatsAppAfterCopy(false);
    }
  }

  return (
    <>
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button
            variant="ghost"
            size="icon"
            className="h-7 w-7"
            aria-label={`Ações do convite para ${invitation.email}`}
          >
            <MoreHorizontal className="h-4 w-4" />
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end">
          <DropdownMenuLabel className="text-xs text-muted-foreground">
            {invitation.inviteeName || invitation.email}
          </DropdownMenuLabel>
          <div className="px-2 pb-1.5">
            <Badge variant={invitationStatusMeta(invitation.status).variant}>
              {invitationStatusMeta(invitation.status).label}
            </Badge>
          </div>
          <DropdownMenuSeparator />
          {canRegenerate && (
            <>
              <DropdownMenuItem onSelect={() => setMode("copy")}>
                <Copy />
                Copiar link
              </DropdownMenuItem>
              <DropdownMenuItem onSelect={sendByWhatsApp}>
                <MessageCircle />
                Enviar por WhatsApp
              </DropdownMenuItem>
              <DropdownMenuItem onSelect={() => setMode("send")}>
                <Mail />
                Reenviar
              </DropdownMenuItem>
            </>
          )}
          {canRevoke && (
            <DropdownMenuItem
              onSelect={() => setConfirmRevoke(true)}
              className="text-destructive focus:text-destructive"
            >
              <XCircle />
              Revogar
            </DropdownMenuItem>
          )}
        </DropdownMenuContent>
      </DropdownMenu>

      <AlertDialog
        open={mode !== null}
        onOpenChange={(open) => {
          if (!open) {
            setMode(null);
            setWhatsAppAfterCopy(false);
          }
        }}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>
              {mode === "send" ? "Reenviar convite" : "Gerar novo link de convite"}
            </AlertDialogTitle>
            <AlertDialogDescription>
              Um novo link será gerado e o atual deixará de funcionar imediatamente.
              {mode === "send" ? (
                <>
                  {" "}
                  Um novo e-mail será enviado para <strong>{invitation.email}</strong> com o papel{" "}
                  <strong>{roleLabel(invitation.role)}</strong>.
                </>
              ) : (
                <> O novo link será copiado para a área de transferência.</>
              )}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancelar</AlertDialogCancel>
            <AlertDialogAction
              onClick={(event) => {
                event.preventDefault();
                runRegenerate();
              }}
              disabled={regenerate.isPending}
            >
              {regenerate.isPending
                ? "Gerando..."
                : mode === "send"
                  ? "Reenviar"
                  : "Gerar e copiar"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      <AlertDialog open={noLinkOpen} onOpenChange={setNoLinkOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Link indisponível</AlertDialogTitle>
            <AlertDialogDescription>
              O CRM não armazena o link do convite e ele não pode ser recuperado. Para enviar por
              WhatsApp, use <strong>Copiar link</strong> — isso gera um novo link e invalida o link
              enviado antes. Depois, envie o novo link pelo WhatsApp.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancelar</AlertDialogCancel>
            <AlertDialogAction
              onClick={(event) => {
                event.preventDefault();
                setNoLinkOpen(false);
                setWhatsAppAfterCopy(true);
                setMode("copy");
              }}
            >
              Gerar link e abrir WhatsApp
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      <AlertDialog open={confirmRevoke} onOpenChange={setConfirmRevoke}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Revogar convite</AlertDialogTitle>
            <AlertDialogDescription>
              O link deixará de funcionar e <strong>{invitation.email}</strong> não poderá mais
              aceitar este convite. Esta ação não pode ser desfeita.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancelar</AlertDialogCancel>
            <AlertDialogAction
              onClick={(event) => {
                event.preventDefault();
                revoke.mutate(invitation.id, { onSuccess: () => setConfirmRevoke(false) });
              }}
              disabled={revoke.isPending}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            >
              {revoke.isPending ? "Revogando..." : "Revogar"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  );
}
