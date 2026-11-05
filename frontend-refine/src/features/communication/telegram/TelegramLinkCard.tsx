"use client";

import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Loader2, Send } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { errorMessage, telegramApi, type LinkCode } from "./api";

const POLL_MS = 5000;
const POLL_WINDOW_MS = 10 * 60 * 1000;

/** Vínculo da conta do usuário com o Telegram, para conversar com o Leo. */
export function TelegramLinkCard({ companyId }: { companyId: string }) {
  const queryClient = useQueryClient();
  const meKey = ["telegram-me", companyId];
  const [code, setCode] = useState<LinkCode | null>(null);
  const [pollUntil, setPollUntil] = useState(0);

  const { data: me, isLoading } = useQuery({
    queryKey: meKey,
    queryFn: () => telegramApi.me(companyId),
    enabled: Boolean(companyId),
    refetchInterval: () => (Date.now() < pollUntil ? POLL_MS : false),
  });

  // Vinculou enquanto o código estava na tela.
  useEffect(() => {
    if (me && code) {
      setCode(null);
      setPollUntil(0);
      void queryClient.invalidateQueries({ queryKey: ["telegram-links", companyId] });
      toast.success("Telegram vinculado");
    }
  }, [me, code, queryClient, companyId]);

  // Fim da janela de 10 minutos: esconde o código vencido.
  useEffect(() => {
    if (!code) return;
    const timer = setTimeout(
      () => {
        setCode(null);
        setPollUntil(0);
      },
      Math.max(pollUntil - Date.now(), 0),
    );
    return () => clearTimeout(timer);
  }, [code, pollUntil]);

  const generate = useMutation({
    mutationFn: () => telegramApi.linkCode(companyId),
    onSuccess: (result) => {
      setCode(result);
      setPollUntil(Date.now() + POLL_WINDOW_MS);
    },
    onError: (error) => toast.error(errorMessage(error, "Não foi possível gerar o código")),
  });

  const unlink = useMutation({
    mutationFn: () => telegramApi.unlinkMe(companyId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: meKey });
      void queryClient.invalidateQueries({ queryKey: ["telegram-links", companyId] });
      toast.success("Telegram desconectado.");
    },
    onError: (error) => toast.error(errorMessage(error, "Não foi possível desconectar")),
  });

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Send className="h-5 w-5" /> Telegram
        </CardTitle>
        <CardDescription>Converse com o Leo pelo Telegram, por texto ou áudio.</CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        {isLoading ? (
          <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
        ) : me ? (
          <div className="flex flex-wrap items-center justify-between gap-3">
            <p className="text-sm">Vinculado ao Telegram de {me.telegramName ?? "sua conta"}</p>
            <Button
              type="button"
              variant="outline"
              className="gap-1.5"
              disabled={unlink.isPending}
              onClick={() => unlink.mutate()}
            >
              {unlink.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
              Desconectar
            </Button>
          </div>
        ) : code ? (
          <div className="space-y-2">
            <p className="font-mono text-4xl font-bold tracking-widest">{code.code}</p>
            <p className="text-sm">
              Envie <code className="rounded bg-muted px-1.5 py-0.5">/vincular {code.code}</code>{" "}
              para @{code.botUsername} em até {code.expiresInMinutes} minutos.
            </p>
            <a
              href={`https://t.me/${code.botUsername}`}
              target="_blank"
              rel="noreferrer"
              className="text-sm underline"
            >
              https://t.me/{code.botUsername}
            </a>
            <p className="flex items-center gap-2 text-xs text-muted-foreground">
              <Loader2 className="h-3 w-3 animate-spin" /> Aguardando o vínculo...
            </p>
          </div>
        ) : (
          <Button
            type="button"
            className="gap-1.5"
            disabled={generate.isPending}
            onClick={() => generate.mutate()}
          >
            {generate.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
            Conectar Telegram
          </Button>
        )}
      </CardContent>
    </Card>
  );
}
