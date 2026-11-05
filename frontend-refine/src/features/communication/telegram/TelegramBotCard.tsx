"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { CheckCircle2, Loader2, Send } from "lucide-react";
import { toast } from "sonner";
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
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { errorMessage, telegramApi } from "./api";

/** Configuração do bot do Telegram da clínica e das pessoas vinculadas. */
export function TelegramBotCard({ companyId }: { companyId: string }) {
  const queryClient = useQueryClient();
  const configKey = ["telegram", companyId];
  const linksKey = ["telegram-links", companyId];

  const { data, isLoading } = useQuery({
    queryKey: configKey,
    queryFn: () => telegramApi.get(companyId),
    enabled: Boolean(companyId),
  });
  const { data: links } = useQuery({
    queryKey: linksKey,
    queryFn: () => telegramApi.links(companyId),
    enabled: Boolean(companyId) && Boolean(data?.configured),
  });

  const [token, setToken] = useState("");
  const [confirmDisconnect, setConfirmDisconnect] = useState(false);

  const refresh = () => {
    void queryClient.invalidateQueries({ queryKey: configKey });
    void queryClient.invalidateQueries({ queryKey: linksKey });
  };

  const save = useMutation({
    mutationFn: () => telegramApi.save(companyId, token.trim()),
    onSuccess: () => {
      setToken("");
      refresh();
      toast.success("Bot conectado.");
    },
    onError: (error) => toast.error(errorMessage(error, "Não foi possível conectar o bot")),
  });

  const remove = useMutation({
    mutationFn: () => telegramApi.remove(companyId),
    onSuccess: () => {
      setConfirmDisconnect(false);
      refresh();
      toast.success("Bot desconectado.");
    },
    onError: (error) => toast.error(errorMessage(error, "Não foi possível desconectar o bot")),
  });

  const revoke = useMutation({
    mutationFn: (userId: string) => telegramApi.revoke(companyId, userId),
    onSuccess: () => {
      refresh();
      toast.success("Vínculo revogado.");
    },
    onError: (error) => toast.error(errorMessage(error, "Não foi possível revogar o vínculo")),
  });

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Send className="h-5 w-5" /> Bot do Telegram
          {data?.configured && (
            <Badge variant="secondary" className="gap-1">
              <CheckCircle2 className="h-3 w-3" /> Conectado como @{data.botUsername}
            </Badge>
          )}
        </CardTitle>
        <CardDescription>
          Crie um bot no @BotFather, cole o token abaixo e cada pessoa vincula a própria conta pelo
          perfil.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        {isLoading ? (
          <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
        ) : data?.configured ? (
          <Button
            type="button"
            variant="outline"
            className="text-destructive"
            disabled={remove.isPending}
            onClick={() => setConfirmDisconnect(true)}
          >
            Desconectar
          </Button>
        ) : (
          <form
            className="space-y-4"
            onSubmit={(e) => {
              e.preventDefault();
              if (token.trim()) save.mutate();
            }}
          >
            <div className="space-y-1.5">
              <Label htmlFor="telegram-token">Token do bot (@BotFather)</Label>
              <Input
                id="telegram-token"
                type="password"
                autoComplete="new-password"
                value={token}
                onChange={(e) => setToken(e.target.value)}
              />
            </div>
            <Button type="submit" disabled={!token.trim() || save.isPending} className="gap-1.5">
              {save.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
              Conectar bot
            </Button>
          </form>
        )}

        {data?.configured && (
          <div className="space-y-2">
            <h3 className="text-sm font-medium">Pessoas vinculadas</h3>
            {links && links.length > 0 ? (
              <ul className="divide-y rounded-md border">
                {links.map((link) => (
                  <li
                    key={link.userId}
                    className="flex items-center justify-between gap-3 px-3 py-2 text-sm"
                  >
                    <div>
                      <p className="font-medium">{link.telegramName ?? "Sem nome"}</p>
                      <p className="text-xs text-muted-foreground">
                        Vinculado em {new Date(link.createdAt).toLocaleDateString("pt-BR")}
                      </p>
                    </div>
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      className="text-destructive"
                      disabled={revoke.isPending}
                      onClick={() => revoke.mutate(link.userId)}
                    >
                      Revogar
                    </Button>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="text-sm text-muted-foreground">Ninguém vinculou o Telegram ainda.</p>
            )}
          </div>
        )}
      </CardContent>

      <AlertDialog open={confirmDisconnect} onOpenChange={setConfirmDisconnect}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Desconectar o bot</AlertDialogTitle>
            <AlertDialogDescription>
              O Leo deixa de responder no Telegram. Deseja continuar?
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancelar</AlertDialogCancel>
            <AlertDialogAction onClick={() => remove.mutate()}>Desconectar</AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </Card>
  );
}
