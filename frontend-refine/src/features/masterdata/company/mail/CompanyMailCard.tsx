"use client";

import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { CheckCircle2, Eye, EyeOff, Loader2, Mail, Send, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import api from "@/lib/api";

type MailSettings = {
  configured: boolean;
  email: string | null;
  senderName: string | null;
  host: string | null;
  port: number | null;
  hasPassword: boolean;
  updatedAt: string | null;
};

/** Provedores em que servidor e porta são deduzidos pelo backend. */
const KNOWN_DOMAINS = [
  "gmail.com",
  "googlemail.com",
  "outlook.com",
  "hotmail.com",
  "live.com",
  "yahoo.com",
  "yahoo.com.br",
  "icloud.com",
  "uol.com.br",
  "bol.com.br",
];

function errorMessage(error: unknown, fallback: string) {
  return (
    (error as { response?: { data?: { message?: string } } })?.response?.data?.message ?? fallback
  );
}

/** E-mail da própria clínica para enviar formulários aos pacientes. */
export function CompanyMailCard({ companyId }: { companyId: string }) {
  const queryClient = useQueryClient();
  const key = ["company-mail-settings", companyId];
  const base = `/companies/${companyId}/mail-settings`;

  const { data, isLoading } = useQuery({
    queryKey: key,
    queryFn: async () => (await api.get<MailSettings>(base)).data,
    enabled: Boolean(companyId),
  });

  const [email, setEmail] = useState("");
  const [senderName, setSenderName] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [host, setHost] = useState("");
  const [port, setPort] = useState("");

  useEffect(() => {
    if (!data) return;
    setEmail(data.email ?? "");
    setSenderName(data.senderName ?? "");
    setHost(data.host ?? "");
    setPort(data.port ? String(data.port) : "");
  }, [data]);

  const domain = email.split("@")[1]?.toLowerCase() ?? "";
  const known = KNOWN_DOMAINS.includes(domain);
  const isGmail = domain === "gmail.com" || domain === "googlemail.com";

  const refresh = () => {
    void queryClient.invalidateQueries({ queryKey: key });
    void queryClient.invalidateQueries({ queryKey: ["form-request-options", companyId] });
  };

  const save = useMutation({
    mutationFn: async () =>
      (
        await api.put<MailSettings>(base, {
          email,
          senderName: senderName || null,
          password: password || null,
          // Provedor conhecido: o servidor deduz host e porta pelo e-mail.
          host: known ? null : host || null,
          port: known ? null : port ? Number(port) : null,
        })
      ).data,
    onSuccess: () => {
      setPassword("");
      setShowPassword(false);
      refresh();
      toast.success("E-mail salvo. Envie um teste para conferir.");
    },
    onError: (error) => toast.error(errorMessage(error, "Não foi possível salvar o e-mail")),
  });

  const test = useMutation({
    mutationFn: async () => api.post(`${base}/test`, {}),
    onSuccess: () => toast.success(`E-mail de teste enviado para ${data?.email}.`),
    onError: (error) => toast.error(errorMessage(error, "O e-mail de teste falhou")),
  });

  const remove = useMutation({
    mutationFn: async () => api.delete(base),
    onSuccess: () => {
      setEmail("");
      setSenderName("");
      setPassword("");
      setHost("");
      setPort("");
      refresh();
      toast.success("E-mail removido.");
    },
    onError: (error) => toast.error(errorMessage(error, "Não foi possível remover")),
  });

  const canSave = Boolean(email) && (Boolean(password) || Boolean(data?.hasPassword));

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Mail className="h-5 w-5" /> E-mail de envio
          {data?.configured && (
            <Badge variant="secondary" className="gap-1">
              <CheckCircle2 className="h-3 w-3" /> Configurado
            </Badge>
          )}
        </CardTitle>
        <CardDescription>
          Os formulários enviados aos pacientes saem deste e-mail, e as respostas do paciente chegam
          nele.
        </CardDescription>
      </CardHeader>
      <CardContent>
        {isLoading ? (
          <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
        ) : (
          <form
            className="space-y-4"
            onSubmit={(e) => {
              e.preventDefault();
              if (canSave) save.mutate();
            }}
          >
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-1.5">
                <Label htmlFor="mail-email">E-mail</Label>
                <Input
                  id="mail-email"
                  type="email"
                  autoComplete="off"
                  placeholder="contato@suaclinica.com.br"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="mail-password">Senha</Label>
                <div className="relative">
                  <Input
                    id="mail-password"
                    type={showPassword ? "text" : "password"}
                    autoComplete="new-password"
                    className="pr-10"
                    placeholder={data?.hasPassword ? "•••••••• (deixe em branco para manter)" : ""}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                  />
                  <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    className="absolute right-0 top-0 h-full px-3 hover:bg-transparent"
                    onClick={() => setShowPassword((v) => !v)}
                    aria-label={showPassword ? "Ocultar senha" : "Mostrar senha"}
                  >
                    {showPassword ? (
                      <EyeOff className="h-4 w-4 text-muted-foreground" />
                    ) : (
                      <Eye className="h-4 w-4 text-muted-foreground" />
                    )}
                  </Button>
                </div>
              </div>
            </div>

            {isGmail && (
              <p className="rounded-md bg-muted p-3 text-xs text-muted-foreground">
                No Gmail use uma <strong>senha de app</strong>, não a senha normal da conta. Ative a
                verificação em duas etapas e gere a senha em{" "}
                <a
                  href="https://myaccount.google.com/apppasswords"
                  target="_blank"
                  rel="noreferrer"
                  className="underline"
                >
                  myaccount.google.com/apppasswords
                </a>
                .
              </p>
            )}

            <div className="space-y-1.5">
              <Label htmlFor="mail-sender">Nome do remetente</Label>
              <Input
                id="mail-sender"
                placeholder="Nome da clínica (padrão)"
                value={senderName}
                onChange={(e) => setSenderName(e.target.value)}
              />
            </div>

            {email.includes("@") && !known && (
              <div className="grid gap-4 sm:grid-cols-[1fr_120px]">
                <div className="space-y-1.5">
                  <Label htmlFor="mail-host">Servidor SMTP</Label>
                  <Input
                    id="mail-host"
                    placeholder="smtp.seuprovedor.com.br"
                    value={host}
                    onChange={(e) => setHost(e.target.value)}
                  />
                </div>
                <div className="space-y-1.5">
                  <Label htmlFor="mail-port">Porta</Label>
                  <Input
                    id="mail-port"
                    inputMode="numeric"
                    placeholder="587"
                    value={port}
                    onChange={(e) => setPort(e.target.value.replace(/\D/g, ""))}
                  />
                </div>
              </div>
            )}

            <div className="flex flex-wrap gap-2">
              <Button type="submit" disabled={!canSave || save.isPending} className="gap-1.5">
                {save.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
                Salvar
              </Button>
              {data?.configured && (
                <>
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
                    Enviar e-mail de teste
                  </Button>
                  <Button
                    type="button"
                    variant="ghost"
                    className="gap-1.5 text-destructive"
                    disabled={remove.isPending}
                    onClick={() => remove.mutate()}
                  >
                    <Trash2 className="h-4 w-4" /> Remover
                  </Button>
                </>
              )}
            </div>
          </form>
        )}
      </CardContent>
    </Card>
  );
}
