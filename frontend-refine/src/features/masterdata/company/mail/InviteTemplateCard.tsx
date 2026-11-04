"use client";

import { useEffect, useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Eye, ImagePlus, Loader2, Palette, Send, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { Textarea } from "@/components/ui/textarea";
import api from "@/lib/api";

export type InviteTemplate = {
  enabled: boolean;
  logo: string | null;
  color: string | null;
  title: string | null;
  message: string | null;
  footer: string | null;
};

type MailSettingsWithTemplate = {
  configured: boolean;
  email: string | null;
  inviteTemplate: InviteTemplate | null;
};

type InvitePreview = { subject: string; html: string };

const DEFAULT_COLOR = "#0b5cff";
const LOGO_TYPES = ["image/png", "image/jpeg"];
const LOGO_MAX_BYTES = 500 * 1024;
const HEX_COLOR = /^#[0-9a-fA-F]{6}$/;
const MESSAGE_MAX = 1500;

function errorMessage(error: unknown, fallback: string) {
  return (
    (error as { response?: { data?: { message?: string } } })?.response?.data?.message ?? fallback
  );
}

/**
 * Modelo do e-mail de convite da empresa. O passo a passo e o link do convite
 * são sempre gerados pelo sistema; aqui a empresa ajusta a aparência e o texto.
 */
export function InviteTemplateCard({ companyId }: { companyId: string }) {
  const queryClient = useQueryClient();
  const key = ["company-mail-settings", companyId];
  const base = `/companies/${companyId}/mail-settings`;
  const logoInput = useRef<HTMLInputElement>(null);

  const { data } = useQuery({
    queryKey: key,
    queryFn: async () => (await api.get<MailSettingsWithTemplate>(base)).data,
    enabled: Boolean(companyId),
  });

  const [enabled, setEnabled] = useState(false);
  const [logo, setLogo] = useState<string | null>(null);
  const [color, setColor] = useState(DEFAULT_COLOR);
  const [title, setTitle] = useState("");
  const [message, setMessage] = useState("");
  const [footer, setFooter] = useState("");
  const [preview, setPreview] = useState<InvitePreview | null>(null);

  useEffect(() => {
    const t = data?.inviteTemplate;
    if (!t) return;
    setEnabled(t.enabled);
    setLogo(t.logo);
    setColor(t.color ?? DEFAULT_COLOR);
    setTitle(t.title ?? "");
    setMessage(t.message ?? "");
    setFooter(t.footer ?? "");
  }, [data]);

  const body = (): InviteTemplate => ({
    enabled,
    logo,
    color: HEX_COLOR.test(color) ? color : null,
    title: title || null,
    message: message || null,
    footer: footer || null,
  });

  const handleLogoFile = (file: File | undefined) => {
    if (!file) return;
    if (!LOGO_TYPES.includes(file.type)) {
      toast.error("Formato não aceito. Use PNG ou JPG.");
      return;
    }
    if (file.size > LOGO_MAX_BYTES) {
      toast.error("O logo deve ter até 500 KB.");
      return;
    }
    const reader = new FileReader();
    reader.onload = () => setLogo(String(reader.result));
    reader.readAsDataURL(file);
  };

  const save = useMutation({
    mutationFn: async () => (await api.put(`${base}/invite-template`, body())).data,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: key });
      toast.success(
        enabled
          ? "Modelo salvo. Os próximos convites já saem com ele."
          : "Modelo salvo. Ative-o para usar nos convites.",
      );
    },
    onError: (error) => toast.error(errorMessage(error, "Não foi possível salvar o modelo")),
  });

  const showPreview = useMutation({
    mutationFn: async () =>
      (await api.post<InvitePreview>(`${base}/invite-template/preview`, body())).data,
    onSuccess: (result) => setPreview(result),
    onError: (error) => toast.error(errorMessage(error, "Não foi possível gerar a prévia")),
  });

  const test = useMutation({
    mutationFn: async () => api.post(`${base}/invite-template/test`, {}),
    onSuccess: () => toast.success(`Convite de teste enviado para ${data?.email}.`),
    onError: (error) => toast.error(errorMessage(error, "O envio de teste falhou")),
  });

  if (!data?.configured) {
    return null;
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Palette className="h-5 w-5" /> Modelo do convite
        </CardTitle>
        <CardDescription>
          Aparência e texto do e-mail que convida pessoas para o CRM. O botão, o passo a passo e o
          link do convite são incluídos automaticamente.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <form
          className="space-y-5"
          onSubmit={(e) => {
            e.preventDefault();
            save.mutate();
          }}
        >
          <div className="flex items-start justify-between gap-4 rounded-md border p-3">
            <div className="space-y-0.5">
              <Label htmlFor="invite-enabled">Usar modelo personalizado</Label>
              <p className="text-xs text-muted-foreground">
                Desligado, os convites usam o modelo padrão.
              </p>
            </div>
            <Switch id="invite-enabled" checked={enabled} onCheckedChange={setEnabled} />
          </div>

          <div className="space-y-1.5">
            <Label>Logo</Label>
            <div className="flex flex-wrap items-center gap-3">
              <div className="flex h-16 min-w-32 items-center justify-center rounded-md border bg-white px-3">
                {logo ? (
                  // eslint-disable-next-line @next/next/no-img-element
                  <img src={logo} alt="Logo do convite" className="max-h-12 w-auto" />
                ) : (
                  <span className="text-xs text-muted-foreground">Sem logo</span>
                )}
              </div>
              <input
                ref={logoInput}
                type="file"
                accept={LOGO_TYPES.join(",")}
                className="hidden"
                onChange={(e) => {
                  handleLogoFile(e.target.files?.[0]);
                  e.target.value = "";
                }}
              />
              <Button
                type="button"
                variant="outline"
                className="gap-1.5"
                onClick={() => logoInput.current?.click()}
              >
                <ImagePlus className="h-4 w-4" /> {logo ? "Trocar logo" : "Enviar logo"}
              </Button>
              {logo && (
                <Button
                  type="button"
                  variant="ghost"
                  className="gap-1.5 text-destructive"
                  onClick={() => setLogo(null)}
                >
                  <Trash2 className="h-4 w-4" /> Remover
                </Button>
              )}
            </div>
            <p className="text-xs text-muted-foreground">
              PNG ou JPG até 500 KB. Prefira logo com fundo claro ou transparente: o topo do e-mail
              é branco.
            </p>
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="invite-color">Cor principal</Label>
            <div className="flex items-center gap-2">
              <input
                id="invite-color"
                type="color"
                aria-label="Escolher cor principal"
                className="h-9 w-12 cursor-pointer rounded-md border bg-transparent p-1"
                value={HEX_COLOR.test(color) ? color : DEFAULT_COLOR}
                onChange={(e) => setColor(e.target.value)}
              />
              <Input
                aria-label="Cor principal em hexadecimal"
                className="w-32 font-mono"
                maxLength={7}
                value={color}
                onChange={(e) => setColor(e.target.value)}
              />
            </div>
            <p className="text-xs text-muted-foreground">Usada no botão, nos passos e no rodapé.</p>
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="invite-title">Título</Label>
            <Input
              id="invite-title"
              maxLength={150}
              placeholder="Seja muito bem-vindo(a)!"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
            />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="invite-message">Mensagem de boas-vindas</Label>
            <Textarea
              id="invite-message"
              rows={5}
              maxLength={MESSAGE_MAX}
              placeholder="Que alegria ter você com a gente! Você foi convidado(a) para fazer parte da equipe de {empresa} como {perfil}."
              value={message}
              onChange={(e) => setMessage(e.target.value)}
            />
            <p className="text-xs text-muted-foreground">
              Use <code>{"{empresa}"}</code> e <code>{"{perfil}"}</code> para inserir o nome da
              empresa e o perfil de acesso. Em branco, usa o texto padrão. {message.length}/
              {MESSAGE_MAX}
            </p>
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="invite-footer">Rodapé</Label>
            <Input
              id="invite-footer"
              maxLength={200}
              placeholder="Ex.: Rua das Flores, 100 • (34) 99999-0000"
              value={footer}
              onChange={(e) => setFooter(e.target.value)}
            />
            <p className="text-xs text-muted-foreground">
              Aparece abaixo do nome do remetente, no fim do e-mail.
            </p>
          </div>

          <div className="flex flex-wrap gap-2">
            <Button type="submit" disabled={save.isPending} className="gap-1.5">
              {save.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
              Salvar modelo
            </Button>
            <Button
              type="button"
              variant="outline"
              className="gap-1.5"
              disabled={showPreview.isPending}
              onClick={() => showPreview.mutate()}
            >
              {showPreview.isPending ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <Eye className="h-4 w-4" />
              )}
              Ver prévia
            </Button>
            <Button
              type="button"
              variant="outline"
              className="gap-1.5"
              disabled={test.isPending}
              onClick={() => test.mutate()}
            >
              {test.isPending ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <Send className="h-4 w-4" />
              )}
              Enviar convite de teste
            </Button>
          </div>
          <p className="text-xs text-muted-foreground">
            O convite de teste usa o modelo salvo. Salve antes de testar.
          </p>
        </form>
      </CardContent>

      <Dialog open={preview !== null} onOpenChange={(open) => !open && setPreview(null)}>
        <DialogContent className="max-w-3xl">
          <DialogHeader>
            <DialogTitle>Prévia do convite</DialogTitle>
            <DialogDescription>Assunto: {preview?.subject}</DialogDescription>
          </DialogHeader>
          <iframe
            title="Prévia do e-mail de convite"
            sandbox=""
            srcDoc={preview?.html ?? ""}
            className="h-[70vh] w-full rounded-md border bg-white"
          />
        </DialogContent>
      </Dialog>
    </Card>
  );
}
