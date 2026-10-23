"use client";

import { useState } from "react";
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

  const [manualUrl, setManualUrl] = useState<string | null>(null);

  const copyUrl = async (url: string): Promise<boolean> => {
    try {
      if (typeof navigator === "undefined" || !navigator.clipboard?.writeText) return false;
      await navigator.clipboard.writeText(url);
      return true;
    } catch {
      return false;
    }
  };

  const send = async () => {
    // Abre a aba no clique (síncrono) para o navegador não bloquear o pop-up.
    const win = window.open("about:blank", "_blank");
    try {
      const { url } = await createLink.mutateAsync();
      const greeting = firstName ? `Olá, ${firstName}!` : "Olá!";
      const target = whatsappUrl(
        phone,
        `${greeting} Para agilizar seu atendimento, complete seu cadastro: ${url}`,
      );
      if (win && target) {
        win.opener = null;
        win.location.href = target;
        return;
      }
      win?.close();
      if (await copyUrl(url)) {
        toast.success("Pop-up bloqueado: link copiado");
      } else {
        setManualUrl(url);
      }
    } catch {
      win?.close();
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
    if (await copyUrl(url)) {
      setManualUrl(null);
      toast.success("Link de cadastro copiado");
    } else {
      setManualUrl(url);
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
      {manualUrl && (
        <input
          readOnly
          aria-label="Link de cadastro"
          value={manualUrl}
          onFocus={(e) => e.currentTarget.select()}
          className="w-full rounded-md border border-input bg-background px-3 py-1.5 text-sm"
        />
      )}
    </div>
  );
}
