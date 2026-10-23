"use client";

import { Copy, MessageCircle } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { whatsappUrl } from "@/lib/whatsapp";
import { useCreateRegistrationLink } from "../useCreateRegistrationLink";

type RegistrationLinkActionsProps = {
  companyId: string | null;
  contactId: string;
  /** Primeiro nome para a saudação da mensagem. */
  firstName: string;
  /** Celular do paciente; sem número utilizável só o "Copiar link" é mostrado. */
  phone?: string | null;
};

/** "Enviar link de cadastro" (WhatsApp) e "Copiar link" para o paciente selecionado. */
export function RegistrationLinkActions({
  companyId,
  contactId,
  firstName,
  phone,
}: RegistrationLinkActionsProps) {
  const createLink = useCreateRegistrationLink(companyId, contactId);
  const canSend = whatsappUrl(phone) !== null;

  const send = async () => {
    try {
      const { url } = await createLink.mutateAsync();
      const greeting = firstName ? `Olá, ${firstName}!` : "Olá!";
      const target = whatsappUrl(
        phone,
        `${greeting} Para agilizar seu atendimento, complete seu cadastro: ${url}`,
      );
      if (target) window.open(target, "_blank", "noopener");
    } catch {
      // o toast de erro vem do useMutationDefaults
    }
  };

  const copy = async () => {
    let url: string;
    try {
      ({ url } = await createLink.mutateAsync());
    } catch {
      return;
    }
    try {
      if (typeof navigator === "undefined" || !navigator.clipboard?.writeText) {
        throw new Error("clipboard unavailable");
      }
      await navigator.clipboard.writeText(url);
      toast.success("Link de cadastro copiado");
    } catch {
      toast.error("Não foi possível copiar o link");
    }
  };

  return (
    <div className="flex flex-wrap gap-2 pt-1">
      {canSend && (
        <Button
          type="button"
          variant="outline"
          size="sm"
          disabled={createLink.isPending}
          onClick={send}
        >
          <MessageCircle className="mr-1.5 h-4 w-4" />
          Enviar link de cadastro
        </Button>
      )}
      <Button
        type="button"
        variant="ghost"
        size="sm"
        disabled={createLink.isPending}
        onClick={copy}
      >
        <Copy className="mr-1.5 h-4 w-4" />
        Copiar link
      </Button>
    </div>
  );
}
