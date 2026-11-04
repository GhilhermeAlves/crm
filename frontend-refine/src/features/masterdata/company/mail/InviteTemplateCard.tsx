"use client";

import { useEffect, useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  Check,
  ChevronLeft,
  ChevronRight,
  ImagePlus,
  Loader2,
  Palette,
  Send,
  Sparkles,
  Trash2,
} from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { Textarea } from "@/components/ui/textarea";
import api from "@/lib/api";
import { cn } from "@/lib/utils";

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

type InviteSuggestion = { title: string; message: string; footer: string };

const LEO_TONES = [
  { value: "ACOLHEDOR", label: "Acolhedor" },
  { value: "PROFISSIONAL", label: "Profissional" },
  { value: "DESCONTRAIDO", label: "Descontraído" },
] as const;

const DEFAULT_COLOR = "#0b5cff";
const LOGO_TYPES = ["image/png", "image/jpeg"];
const LOGO_MAX_BYTES = 500 * 1024;
const HEX_COLOR = /^#[0-9a-fA-F]{6}$/;
const MESSAGE_MAX = 1500;
const COLOR_SUGGESTIONS = ["#0b5cff", "#0f9d74", "#7c3aed", "#db2777", "#ea580c", "#111827"];

const STEPS = [
  { title: "Ativar", hint: "Use um modelo próprio nos convites" },
  { title: "Logo", hint: "A marca no topo do e-mail" },
  { title: "Cor", hint: "Botão, passos e rodapé" },
  { title: "Textos", hint: "Título, mensagem e rodapé" },
  { title: "Revisar", hint: "Salvar e enviar um teste" },
] as const;

function errorMessage(error: unknown, fallback: string) {
  return (
    (error as { response?: { data?: { message?: string } } })?.response?.data?.message ?? fallback
  );
}

/**
 * Assistente do modelo do convite: 5 passos com prévia ao vivo. O botão, o
 * passo a passo e o link do convite são sempre gerados pelo sistema.
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

  const [step, setStep] = useState(0);
  const [enabled, setEnabled] = useState(false);
  const [logo, setLogo] = useState<string | null>(null);
  const [color, setColor] = useState(DEFAULT_COLOR);
  const [title, setTitle] = useState("");
  const [message, setMessage] = useState("");
  const [footer, setFooter] = useState("");
  const [saved, setSaved] = useState(true);
  const [preview, setPreview] = useState<InvitePreview | null>(null);
  const [previewLoading, setPreviewLoading] = useState(false);
  const [leoTone, setLeoTone] = useState<string>("ACOLHEDOR");
  const [leoNotes, setLeoNotes] = useState("");

  const { data: leoStatus } = useQuery({
    queryKey: ["invite-template-leo", companyId],
    queryFn: async () =>
      (await api.get<{ available: boolean }>(`${base}/invite-template/leo`)).data,
    enabled: Boolean(companyId) && Boolean(data?.configured),
    staleTime: 5 * 60 * 1000,
  });

  useEffect(() => {
    const t = data?.inviteTemplate;
    if (!t) return;
    setEnabled(t.enabled);
    setLogo(t.logo);
    setColor(t.color ?? DEFAULT_COLOR);
    setTitle(t.title ?? "");
    setMessage(t.message ?? "");
    setFooter(t.footer ?? "");
    setSaved(true);
  }, [data]);

  // Qualquer alteração marca o modelo como não salvo.
  const edit =
    <T,>(setter: (value: T) => void) =>
    (value: T) => {
      setter(value);
      setSaved(false);
    };

  const colorValid = HEX_COLOR.test(color);

  // Prévia ao vivo: refaz 600 ms depois da última alteração.
  useEffect(() => {
    if (!data?.configured) return;
    const body: InviteTemplate = {
      enabled,
      logo,
      color: colorValid ? color : null,
      title: title || null,
      message: message || null,
      footer: footer || null,
    };
    let cancelled = false;
    const timer = setTimeout(async () => {
      setPreviewLoading(true);
      try {
        const result = (await api.post<InvitePreview>(`${base}/invite-template/preview`, body))
          .data;
        if (!cancelled) setPreview(result);
      } catch {
        // A prévia é auxiliar: erros de validação aparecem ao salvar.
      } finally {
        if (!cancelled) setPreviewLoading(false);
      }
    }, 600);
    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [base, data?.configured, enabled, logo, color, colorValid, title, message, footer]);

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
    reader.onload = () => edit(setLogo)(String(reader.result));
    reader.readAsDataURL(file);
  };

  const save = useMutation({
    mutationFn: async () =>
      (
        await api.put(`${base}/invite-template`, {
          enabled,
          logo,
          color: colorValid ? color : null,
          title: title || null,
          message: message || null,
          footer: footer || null,
        })
      ).data,
    onSuccess: () => {
      setSaved(true);
      void queryClient.invalidateQueries({ queryKey: key });
      toast.success(
        enabled
          ? "Modelo salvo. Os próximos convites já saem com ele."
          : "Modelo salvo, mas desligado: os convites seguem com o modelo padrão.",
      );
    },
    onError: (error) => toast.error(errorMessage(error, "Não foi possível salvar o modelo")),
  });

  const suggest = useMutation({
    mutationFn: async () =>
      (
        await api.post<InviteSuggestion>(`${base}/invite-template/suggest`, {
          tone: leoTone,
          notes: leoNotes || null,
        })
      ).data,
    onSuccess: (s) => {
      if (s.title) edit(setTitle)(s.title);
      if (s.message) edit(setMessage)(s.message);
      if (s.footer) edit(setFooter)(s.footer);
      toast.success("O Leo preencheu os textos. Revise e ajuste como quiser.");
    },
    onError: (error) => toast.error(errorMessage(error, "O Leo não conseguiu sugerir agora")),
  });

  const test = useMutation({
    mutationFn: async () => api.post(`${base}/invite-template/test`, {}),
    onSuccess: () => toast.success(`Convite de teste enviado para ${data?.email}.`),
    onError: (error) => toast.error(errorMessage(error, "O envio de teste falhou")),
  });

  if (!data?.configured) {
    return null;
  }

  const last = step === STEPS.length - 1;
  const canAdvance = step !== 2 || colorValid;

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Palette className="h-5 w-5" /> Modelo do convite
        </CardTitle>
        <CardDescription>
          Siga os passos para personalizar o e-mail que convida pessoas para o CRM. A prévia ao lado
          mostra o resultado na hora.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        <ol className="flex flex-wrap gap-2" aria-label="Passos do modelo do convite">
          {STEPS.map((s, index) => (
            <li key={s.title}>
              <button
                type="button"
                onClick={() => setStep(index)}
                aria-current={index === step ? "step" : undefined}
                className={cn(
                  "flex items-center gap-2 rounded-full border px-3 py-1.5 text-sm transition-colors",
                  index === step
                    ? "border-primary bg-primary text-primary-foreground"
                    : index < step
                      ? "border-primary/40 text-primary"
                      : "text-muted-foreground hover:bg-muted",
                )}
              >
                <span className="flex h-5 w-5 items-center justify-center rounded-full bg-background/20 text-xs font-semibold">
                  {index < step ? <Check className="h-3 w-3" /> : index + 1}
                </span>
                {s.title}
              </button>
            </li>
          ))}
        </ol>

        <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_minmax(0,1.1fr)]">
          <div className="space-y-5">
            <div>
              <p className="text-sm font-semibold">
                Passo {step + 1} de {STEPS.length}: {STEPS[step].title}
              </p>
              <p className="text-xs text-muted-foreground">{STEPS[step].hint}</p>
            </div>

            {step === 0 && (
              <div className="space-y-4">
                <p className="text-sm text-muted-foreground">
                  Com o modelo ligado, todo convite enviado pela sua conta de e-mail sai com o seu
                  logo, a sua cor e o seu texto. O botão <strong>Criar minha conta</strong>, os 4
                  passos e o link do convite são incluídos automaticamente.
                </p>
                <div className="flex items-start justify-between gap-4 rounded-md border p-3">
                  <div className="space-y-0.5">
                    <Label htmlFor="invite-enabled">Usar modelo personalizado</Label>
                    <p className="text-xs text-muted-foreground">
                      Desligado, os convites usam o modelo padrão.
                    </p>
                  </div>
                  <Switch
                    id="invite-enabled"
                    checked={enabled}
                    onCheckedChange={edit(setEnabled)}
                  />
                </div>
                {!enabled && (
                  <p className="rounded-md bg-muted p-3 text-xs text-muted-foreground">
                    Você pode montar o modelo agora e ligar depois. A prévia mostra o modelo padrão
                    enquanto ele estiver desligado.
                  </p>
                )}
              </div>
            )}

            {step === 1 && (
              <div className="space-y-3">
                <div className="flex h-24 items-center justify-center rounded-md border bg-white px-3">
                  {logo ? (
                    // eslint-disable-next-line @next/next/no-img-element
                    <img src={logo} alt="Logo do convite" className="max-h-16 w-auto" />
                  ) : (
                    <span className="text-xs text-muted-foreground">Nenhum logo enviado</span>
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
                <div className="flex flex-wrap gap-2">
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
                      onClick={() => edit(setLogo)(null)}
                    >
                      <Trash2 className="h-4 w-4" /> Remover
                    </Button>
                  )}
                </div>
                <p className="text-xs text-muted-foreground">
                  PNG ou JPG até 500 KB. Prefira logo com fundo claro ou transparente: o topo do
                  e-mail é branco. Sem logo, o nome do remetente aparece no lugar.
                </p>
              </div>
            )}

            {step === 2 && (
              <div className="space-y-3">
                <div
                  className="flex flex-wrap gap-2"
                  role="radiogroup"
                  aria-label="Cores sugeridas"
                >
                  {COLOR_SUGGESTIONS.map((c) => (
                    <button
                      key={c}
                      type="button"
                      role="radio"
                      aria-checked={color.toLowerCase() === c}
                      aria-label={`Cor ${c}`}
                      onClick={() => edit(setColor)(c)}
                      className={cn(
                        "h-9 w-9 rounded-full border-2 transition-transform hover:scale-110",
                        color.toLowerCase() === c ? "border-foreground" : "border-transparent",
                      )}
                      style={{ backgroundColor: c }}
                    />
                  ))}
                </div>
                <div className="flex items-center gap-2">
                  <input
                    id="invite-color"
                    type="color"
                    aria-label="Escolher outra cor"
                    className="h-9 w-12 cursor-pointer rounded-md border bg-transparent p-1"
                    value={colorValid ? color : DEFAULT_COLOR}
                    onChange={(e) => edit(setColor)(e.target.value)}
                  />
                  <Input
                    aria-label="Cor principal em hexadecimal"
                    className="w-32 font-mono"
                    maxLength={7}
                    value={color}
                    onChange={(e) => edit(setColor)(e.target.value)}
                  />
                </div>
                {!colorValid && (
                  <p className="text-xs text-destructive">Use o formato #RRGGBB, ex.: #0b5cff.</p>
                )}
                <p className="text-xs text-muted-foreground">
                  Escolha uma cor escura o bastante para o texto branco do botão ficar legível.
                </p>
              </div>
            )}

            {step === 3 && (
              <div className="space-y-4">
                {leoStatus?.available && (
                  <div className="space-y-3 rounded-md border border-primary/30 bg-primary/5 p-3">
                    <div className="flex items-center gap-2 text-sm font-medium">
                      <Sparkles className="h-4 w-4 text-primary" /> Pedir ao Leo
                    </div>
                    <p className="text-xs text-muted-foreground">
                      O Leo escreve o título, a mensagem e o rodapé para você. Depois é só revisar.
                    </p>
                    <div
                      className="flex flex-wrap gap-2"
                      role="radiogroup"
                      aria-label="Tom do texto"
                    >
                      {LEO_TONES.map((t) => (
                        <Button
                          key={t.value}
                          type="button"
                          size="sm"
                          role="radio"
                          aria-checked={leoTone === t.value}
                          variant={leoTone === t.value ? "default" : "outline"}
                          className="h-7 text-xs"
                          onClick={() => setLeoTone(t.value)}
                        >
                          {t.label}
                        </Button>
                      ))}
                    </div>
                    <Input
                      aria-label="Conte ao Leo sobre a sua empresa"
                      maxLength={500}
                      placeholder="Opcional: conte sobre a empresa (ex.: clínica de estética em Uberlândia)"
                      value={leoNotes}
                      onChange={(e) => setLeoNotes(e.target.value)}
                    />
                    <Button
                      type="button"
                      size="sm"
                      className="gap-1.5"
                      disabled={suggest.isPending}
                      onClick={() => suggest.mutate()}
                    >
                      {suggest.isPending ? (
                        <Loader2 className="h-4 w-4 animate-spin" />
                      ) : (
                        <Sparkles className="h-4 w-4" />
                      )}
                      {title || message || footer ? "Sugerir de novo" : "Sugerir textos"}
                    </Button>
                  </div>
                )}
                <div className="space-y-1.5">
                  <Label htmlFor="invite-title">Título</Label>
                  <Input
                    id="invite-title"
                    maxLength={150}
                    placeholder="Seja muito bem-vindo(a)!"
                    value={title}
                    onChange={(e) => edit(setTitle)(e.target.value)}
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
                    onChange={(e) => edit(setMessage)(e.target.value)}
                  />
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="text-xs text-muted-foreground">Inserir:</span>
                    {["{empresa}", "{perfil}"].map((tag) => (
                      <Button
                        key={tag}
                        type="button"
                        size="sm"
                        variant="secondary"
                        className="h-7 font-mono text-xs"
                        onClick={() => edit(setMessage)(`${message}${message ? " " : ""}${tag}`)}
                      >
                        {tag}
                      </Button>
                    ))}
                    <span className="ml-auto text-xs text-muted-foreground">
                      {message.length}/{MESSAGE_MAX}
                    </span>
                  </div>
                  <p className="text-xs text-muted-foreground">
                    {"{empresa}"} vira o nome da empresa e {"{perfil}"} o perfil de acesso. Em
                    branco, usa o texto padrão.
                  </p>
                </div>
                <div className="space-y-1.5">
                  <Label htmlFor="invite-footer">Rodapé</Label>
                  <Input
                    id="invite-footer"
                    maxLength={200}
                    placeholder="Ex.: Rua das Flores, 100 • (34) 99999-0000"
                    value={footer}
                    onChange={(e) => edit(setFooter)(e.target.value)}
                  />
                </div>
              </div>
            )}

            {step === 4 && (
              <div className="space-y-4">
                <ul className="space-y-1.5 text-sm">
                  <li>
                    Modelo: <strong>{enabled ? "ligado" : "desligado (usa o padrão)"}</strong>
                  </li>
                  <li>
                    Logo: <strong>{logo ? "enviado" : "sem logo"}</strong>
                  </li>
                  <li className="flex items-center gap-2">
                    Cor:
                    <span
                      className="inline-block h-4 w-4 rounded-full border"
                      style={{ backgroundColor: colorValid ? color : DEFAULT_COLOR }}
                    />
                    <strong className="font-mono">{colorValid ? color : DEFAULT_COLOR}</strong>
                  </li>
                  <li>
                    Textos:{" "}
                    <strong>{title || message || footer ? "personalizados" : "padrão"}</strong>
                  </li>
                </ul>
                <div className="flex flex-wrap gap-2">
                  <Button
                    type="button"
                    disabled={save.isPending || !colorValid}
                    className="gap-1.5"
                    onClick={() => save.mutate()}
                  >
                    {save.isPending ? (
                      <Loader2 className="h-4 w-4 animate-spin" />
                    ) : (
                      <Check className="h-4 w-4" />
                    )}
                    Salvar modelo
                  </Button>
                  <Button
                    type="button"
                    variant="outline"
                    className="gap-1.5"
                    disabled={test.isPending || !saved}
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
                  {saved
                    ? `O teste vai para ${data.email} com o modelo salvo.`
                    : "Salve o modelo para liberar o convite de teste."}
                </p>
              </div>
            )}

            <div className="flex justify-between gap-2 border-t pt-4">
              <Button
                type="button"
                variant="ghost"
                className="gap-1"
                disabled={step === 0}
                onClick={() => setStep((s) => s - 1)}
              >
                <ChevronLeft className="h-4 w-4" /> Voltar
              </Button>
              {!last && (
                <Button
                  type="button"
                  className="gap-1"
                  disabled={!canAdvance}
                  onClick={() => setStep((s) => s + 1)}
                >
                  Próximo <ChevronRight className="h-4 w-4" />
                </Button>
              )}
            </div>
          </div>

          <div className="space-y-2">
            <div className="flex items-center justify-between text-xs text-muted-foreground">
              <span className="truncate">
                Assunto: <strong>{preview?.subject ?? "..."}</strong>
              </span>
              {previewLoading && <Loader2 className="h-3.5 w-3.5 animate-spin" />}
            </div>
            <iframe
              title="Prévia ao vivo do e-mail de convite"
              sandbox=""
              srcDoc={preview?.html ?? ""}
              className="h-[560px] w-full rounded-md border bg-white"
            />
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
