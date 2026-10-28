"use client";

import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { CheckCircle2, Loader2, QrCode, RefreshCw, Smartphone, Unplug } from "lucide-react";
import { toast } from "sonner";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import api from "@/lib/api";

type ConnectionState =
  "NOT_CONFIGURED" | "NOT_CREATED" | "CONNECTED" | "CONNECTING" | "DISCONNECTED";
type ConnectionStatus = {
  state: ConnectionState;
  instance: string | null;
  channelId: string | null;
};
type ConnectionQr = { state: ConnectionState; qrCode: string | null; pairingCode: string | null };

const BASE = "/omnichannel/whatsapp/connection";
const STATUS_KEY = ["whatsapp-connection"];

const STATE_LABEL: Record<ConnectionState, string> = {
  NOT_CONFIGURED: "Indisponível no servidor",
  NOT_CREATED: "Não conectado",
  CONNECTED: "Conectado",
  CONNECTING: "Aguardando leitura do QR code",
  DISCONNECTED: "Desconectado",
};

function errorMessage(error: unknown, fallback: string) {
  return (
    (error as { response?: { data?: { message?: string } } })?.response?.data?.message ?? fallback
  );
}

/** Conecta o WhatsApp da empresa lendo o QR code com o celular (Evolution). */
export function WhatsAppConnectionCard() {
  const queryClient = useQueryClient();
  const [qr, setQr] = useState<ConnectionQr | null>(null);

  const { data: status, isLoading } = useQuery({
    queryKey: STATUS_KEY,
    queryFn: async () => (await api.get<ConnectionStatus>(BASE)).data,
    // Enquanto o QR está na tela, confere a cada 3 s se o celular já leu.
    refetchInterval: qr ? 3000 : false,
  });

  const connect = useMutation({
    mutationFn: async () => (await api.post<ConnectionQr>(`${BASE}/connect`)).data,
    onSuccess: (result) => {
      if (result.state === "CONNECTED") {
        setQr(null);
        toast.success("WhatsApp já está conectado.");
      } else {
        setQr(result);
      }
      void queryClient.invalidateQueries({ queryKey: STATUS_KEY });
    },
    onError: (error) => toast.error(errorMessage(error, "Não foi possível gerar o QR code.")),
  });

  const disconnect = useMutation({
    mutationFn: async () => {
      await api.post(`${BASE}/disconnect`);
    },
    onSuccess: () => {
      setQr(null);
      toast.success("WhatsApp desconectado.");
      void queryClient.invalidateQueries({ queryKey: STATUS_KEY });
    },
    onError: (error) => toast.error(errorMessage(error, "Não foi possível desconectar.")),
  });

  // Celular leu o QR: some o código e mostra conectado.
  useEffect(() => {
    if (qr && status?.state === "CONNECTED") {
      setQr(null);
      toast.success("WhatsApp conectado!");
    }
  }, [qr, status?.state]);

  const state = status?.state;

  return (
    <Card>
      <CardHeader>
        <div className="flex flex-wrap items-center justify-between gap-2">
          <div className="space-y-1">
            <CardTitle className="flex items-center gap-2 text-base">
              <Smartphone className="h-4 w-4" /> WhatsApp da clínica
            </CardTitle>
            <CardDescription>
              Conecte o número da clínica para enviar formulários, lembretes e atender no Inbox.
            </CardDescription>
          </div>
          {state && (
            <Badge variant={state === "CONNECTED" ? "default" : "secondary"} className="gap-1">
              {state === "CONNECTED" && <CheckCircle2 className="h-3 w-3" />}
              {STATE_LABEL[state]}
            </Badge>
          )}
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        {isLoading ? (
          <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
        ) : state === "NOT_CONFIGURED" ? (
          <p className="text-sm text-muted-foreground">
            O servidor de WhatsApp (Evolution) não está configurado. Fale com o administrador do
            CRM.
          </p>
        ) : qr ? (
          <div className="grid gap-6 sm:grid-cols-[auto,1fr]">
            {qr.qrCode ? (
              // eslint-disable-next-line @next/next/no-img-element -- QR em data URL vindo da Evolution
              <img
                src={qr.qrCode}
                alt="QR code para conectar o WhatsApp"
                className="h-64 w-64 rounded-md border bg-white p-2"
              />
            ) : (
              <div className="flex h-64 w-64 items-center justify-center rounded-md border">
                <Loader2 className="h-6 w-6 animate-spin" />
              </div>
            )}
            <div className="space-y-3 text-sm">
              <p className="font-medium">No celular da clínica:</p>
              <ol className="list-decimal space-y-1 pl-5 text-muted-foreground">
                <li>Abra o WhatsApp.</li>
                <li>
                  Toque em <strong>Configurações</strong> (ou nos três pontinhos) →{" "}
                  <strong>Dispositivos conectados</strong>.
                </li>
                <li>
                  Toque em <strong>Conectar um dispositivo</strong> e aponte a câmera para o código.
                </li>
              </ol>
              {qr.pairingCode && (
                <p>
                  Ou use o código de pareamento: <strong>{qr.pairingCode}</strong>
                </p>
              )}
              <p className="text-xs text-muted-foreground">
                O código expira em cerca de 1 minuto. Esta tela atualiza sozinha quando conectar.
              </p>
              <Button
                variant="outline"
                size="sm"
                className="gap-1.5"
                disabled={connect.isPending}
                onClick={() => connect.mutate()}
              >
                <RefreshCw className="h-4 w-4" /> Gerar novo código
              </Button>
            </div>
          </div>
        ) : state === "CONNECTED" ? (
          <div className="flex flex-wrap items-center gap-3">
            <p className="text-sm text-muted-foreground">
              O WhatsApp está conectado e pronto para uso.
            </p>
            <Button
              variant="outline"
              size="sm"
              className="gap-1.5"
              disabled={disconnect.isPending}
              onClick={() => disconnect.mutate()}
            >
              <Unplug className="h-4 w-4" /> Desconectar
            </Button>
          </div>
        ) : (
          <Button className="gap-1.5" disabled={connect.isPending} onClick={() => connect.mutate()}>
            {connect.isPending ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <QrCode className="h-4 w-4" />
            )}
            Conectar com QR code
          </Button>
        )}
      </CardContent>
    </Card>
  );
}
