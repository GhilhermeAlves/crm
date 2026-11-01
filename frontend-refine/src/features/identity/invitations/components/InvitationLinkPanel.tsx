"use client";

import { Copy, MessageCircle } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { copyInvitationLink, openInvitationWhatsApp } from "../lib/invitation-share";
import { resolveInviteUrl } from "../lib/invite-url";
import type { InvitationLink } from "../types/invitation.types";

type InvitationLinkPanelProps = {
  link: Pick<InvitationLink, "url">;
  companyName?: string | null;
  onClose: () => void;
};

export function InvitationLinkPanel({ link, companyName, onClose }: InvitationLinkPanelProps) {
  const url = resolveInviteUrl(link.url);

  return (
    <div className="space-y-4">
      <div className="space-y-2">
        <Label htmlFor="invitation-link">Link do convite</Label>
        <Input
          id="invitation-link"
          readOnly
          value={url}
          onFocus={(event) => event.currentTarget.select()}
          className="font-mono text-xs"
        />
        <p className="text-xs text-muted-foreground">
          Este link não será exibido novamente. Copie ou envie agora. Gerar um novo link invalida
          este.
        </p>
      </div>
      <div className="flex flex-col gap-2 sm:flex-row">
        <Button
          type="button"
          variant="outline"
          className="sm:flex-1"
          onClick={() => copyInvitationLink(link.url)}
        >
          <Copy className="mr-2 h-4 w-4" />
          Copiar link
        </Button>
        <Button
          type="button"
          className="sm:flex-1"
          onClick={() => openInvitationWhatsApp(link.url, companyName)}
        >
          <MessageCircle className="mr-2 h-4 w-4" />
          Enviar por WhatsApp
        </Button>
      </div>
      <div className="flex justify-end">
        <Button type="button" variant="ghost" onClick={onClose}>
          Fechar
        </Button>
      </div>
    </div>
  );
}
