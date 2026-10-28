"use client";

import { useState } from "react";
import { format, parseISO } from "date-fns";
import { Copy, ExternalLink, Loader2, MessageCircle, Send, Tablet } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import type { AnamnesisModelSummary } from "@/features/masterdata/anamnesis/types/anamnesis.types";
import type { Contact } from "@/features/masterdata/contacts/types/contact.types";
import {
  type CreatedFormRequest,
  type FormRequestMode,
  useCreateFormRequest,
  useWhatsAppConnected,
} from "../hooks/usePatientFormRequests";

interface Props {
  companyId: string;
  patient: Contact;
  models: AnamnesisModelSummary[];
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

/** O backend devolve a URL com a origem configurada; sem ela, usa a do navegador. */
function absoluteUrl(url: string) {
  return url.startsWith("http") ? url : `${window.location.origin}${url}`;
}

/**
 * Envia um formulário (modelo de anamnese) ao paciente: gera o link com token
 * ou abre o modo recepção para o paciente responder no tablet da clínica.
 */
export function SendFormDialog({ companyId, patient, models, open, onOpenChange }: Props) {
  const defaultModel = models.find((m) => m.isDefault) ?? models[0];
  const [modelId, setModelId] = useState<string | undefined>(undefined);
  const [created, setCreated] = useState<CreatedFormRequest | null>(null);
  const create = useCreateFormRequest(companyId, patient.id);
  const whatsAppConnected = useWhatsAppConnected();
  const hasPhone = Boolean(patient.mobile || patient.phone);
  const effectiveModel = modelId ?? defaultModel?.id;

  const close = (value: boolean) => {
    if (!value) {
      setCreated(null);
      setModelId(undefined);
    }
    onOpenChange(value);
  };

  const generate = (mode: FormRequestMode, sendWhatsApp = false) => {
    if (!effectiveModel) return;
    // A aba precisa abrir no clique (antes da resposta), senão o navegador bloqueia.
    const tab = mode === "RECEPTION" ? window.open("about:blank", "_blank") : null;
    create.mutate(
      { modelId: effectiveModel, mode, sendWhatsApp },
      {
        onSuccess: (result) => {
          if (mode === "RECEPTION") {
            const url = `${absoluteUrl(result.url)}?modo=recepcao`;
            if (tab) tab.location.href = url;
            else window.open(url, "_blank");
            close(false);
          } else {
            setCreated(result);
            if (sendWhatsApp) {
              if (result.whatsAppSent)
                toast.success(`Link enviado pelo WhatsApp para ${patient.firstName}.`);
              else
                toast.error(
                  "Não foi possível enviar pelo WhatsApp. Copie o link e envie manualmente.",
                );
            }
          }
        },
        onError: () => tab?.close(),
      },
    );
  };

  const link = created ? absoluteUrl(created.url) : "";
  const phone = (patient.mobile || patient.phone || "").replace(/\D/g, "");
  const whatsappText = encodeURIComponent(
    `Olá, ${patient.firstName}! Por favor, responda o formulário antes da sua consulta: ${link}`,
  );
  const whatsappUrl = phone
    ? `https://wa.me/${phone.length <= 11 ? `55${phone}` : phone}?text=${whatsappText}`
    : `https://wa.me/?text=${whatsappText}`;

  const copy = async () => {
    try {
      await navigator.clipboard.writeText(link);
      toast.success("Link copiado.");
    } catch {
      toast.error("Não foi possível copiar. Selecione o link e copie manualmente.");
    }
  };

  return (
    <Dialog open={open} onOpenChange={close}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>Formulário para o paciente</DialogTitle>
          <DialogDescription>
            {patient.firstName} responde sem login e assina no fim. A resposta aparece nesta aba e
            no sino de notificações.
          </DialogDescription>
        </DialogHeader>

        {created ? (
          <div className="space-y-3">
            <Label htmlFor="form-link">Link do formulário</Label>
            <div className="flex gap-2">
              <Input id="form-link" readOnly value={link} onFocus={(e) => e.target.select()} />
              <Button type="button" variant="outline" size="icon" onClick={() => void copy()}>
                <Copy className="h-4 w-4" />
                <span className="sr-only">Copiar link</span>
              </Button>
            </div>
            <p className="text-xs text-muted-foreground">
              Vale até {format(parseISO(created.expiresAt), "dd/MM/yyyy 'às' HH:mm")} e só pode ser
              respondido uma vez.
            </p>
            <div className="flex flex-wrap gap-2">
              <Button asChild className="gap-1.5">
                <a href={whatsappUrl} target="_blank" rel="noreferrer">
                  <MessageCircle className="h-4 w-4" /> Enviar pelo WhatsApp
                </a>
              </Button>
              <Button asChild variant="outline" className="gap-1.5">
                <a href={link} target="_blank" rel="noreferrer">
                  <ExternalLink className="h-4 w-4" /> Abrir
                </a>
              </Button>
            </div>
          </div>
        ) : (
          <div className="space-y-2">
            <Label htmlFor="form-model">Modelo</Label>
            <Select value={effectiveModel} onValueChange={setModelId}>
              <SelectTrigger id="form-model">
                <SelectValue placeholder="Selecione um modelo" />
              </SelectTrigger>
              <SelectContent>
                {models.map((model) => (
                  <SelectItem key={model.id} value={model.id}>
                    {model.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <p className="text-xs text-muted-foreground">
              Seções &quot;do profissional&quot; não aparecem para o paciente; você completa depois.
            </p>
          </div>
        )}

        <DialogFooter className="gap-2 sm:gap-0">
          {created ? (
            <Button variant="outline" onClick={() => close(false)}>
              Fechar
            </Button>
          ) : (
            <>
              <Button
                variant="outline"
                className="gap-1.5"
                disabled={!effectiveModel || create.isPending}
                onClick={() => generate("RECEPTION")}
              >
                <Tablet className="h-4 w-4" /> Modo recepção (tablet)
              </Button>
              {whatsAppConnected && hasPhone && (
                <Button
                  variant="outline"
                  className="gap-1.5"
                  disabled={!effectiveModel || create.isPending}
                  onClick={() => generate("LINK", true)}
                >
                  <MessageCircle className="h-4 w-4" /> Enviar pelo WhatsApp da clínica
                </Button>
              )}
              <Button
                className="gap-1.5"
                disabled={!effectiveModel || create.isPending}
                onClick={() => generate("LINK")}
              >
                {create.isPending ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <Send className="h-4 w-4" />
                )}
                Gerar link
              </Button>
            </>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
