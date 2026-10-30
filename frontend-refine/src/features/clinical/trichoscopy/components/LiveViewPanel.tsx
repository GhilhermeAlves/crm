"use client";

import { useEffect, useRef, useState, type RefObject } from "react";
import QRCode from "qrcode";
import { Copy, Loader2, MonitorSmartphone, X } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { LiveViewService } from "../services/live-view.service";

/** Largura do quadro enviado ao tablet: leve o bastante para ~8 quadros/s. */
const FRAME_WIDTH = 1280;
const FRAME_INTERVAL_MS = 120;
const JPEG_QUALITY = 0.72;

type Props = {
  companyId: string;
  /** Desenha no canvas o que aparece na tela (zoom e espelho). */
  draw: (ctx: CanvasRenderingContext2D, width: number, height: number) => boolean;
  videoRef: RefObject<HTMLVideoElement | null>;
};

/**
 * Tela do paciente: transmite a câmera para um tablet que abre o QR/link.
 * Enquanto ativo, envia um quadro por vez (o próximo só depois do anterior).
 */
export function LiveViewPanel({ companyId, draw, videoRef }: Props) {
  const [publishToken, setPublishToken] = useState<string | null>(null);
  const [link, setLink] = useState<string | null>(null);
  const [qr, setQr] = useState<string | null>(null);
  const [opening, setOpening] = useState(false);
  const drawRef = useRef(draw);
  drawRef.current = draw;

  const start = async () => {
    setOpening(true);
    try {
      const tokens = await LiveViewService.open(companyId);
      const url = `${window.location.origin}/t/${tokens.viewToken}`;
      setLink(url);
      setQr(await QRCode.toDataURL(url, { width: 220, margin: 1 }));
      setPublishToken(tokens.publishToken);
    } catch {
      toast.error("Não foi possível abrir a tela do paciente.");
    } finally {
      setOpening(false);
    }
  };

  const stop = () => {
    if (publishToken) LiveViewService.close(publishToken);
    setPublishToken(null);
    setLink(null);
    setQr(null);
  };

  useEffect(() => {
    if (!publishToken) return;
    let cancelled = false;
    let timer: ReturnType<typeof setTimeout> | undefined;
    const canvas = document.createElement("canvas");
    const ctx = canvas.getContext("2d");

    const tick = async () => {
      const video = videoRef.current;
      if (cancelled) return;
      if (ctx && video && video.videoWidth > 0) {
        const width = Math.min(FRAME_WIDTH, video.videoWidth);
        const height = Math.round((video.videoHeight / video.videoWidth) * width);
        canvas.width = width;
        canvas.height = height;
        if (drawRef.current(ctx, width, height)) {
          const blob = await new Promise<Blob | null>((resolve) =>
            canvas.toBlob(resolve, "image/jpeg", JPEG_QUALITY),
          );
          if (blob && !cancelled) {
            const alive = await LiveViewService.push(publishToken, blob).catch(() => true);
            if (!alive) {
              toast.info("A tela do paciente foi encerrada.");
              setPublishToken(null);
              setLink(null);
              setQr(null);
              return;
            }
          }
        }
      }
      if (!cancelled) timer = setTimeout(() => void tick(), FRAME_INTERVAL_MS);
    };
    void tick();

    return () => {
      cancelled = true;
      if (timer) clearTimeout(timer);
    };
  }, [publishToken, videoRef]);

  // Fecha a sessão ao desmontar (diálogo fechado).
  const tokenRef = useRef<string | null>(null);
  tokenRef.current = publishToken;
  useEffect(
    () => () => {
      if (tokenRef.current) LiveViewService.close(tokenRef.current);
    },
    [],
  );

  if (!publishToken) {
    return (
      <Button
        type="button"
        variant="outline"
        size="sm"
        onClick={() => void start()}
        disabled={opening}
      >
        {opening ? (
          <Loader2 className="mr-2 h-4 w-4 animate-spin" />
        ) : (
          <MonitorSmartphone className="mr-2 h-4 w-4" />
        )}
        Tela do paciente
      </Button>
    );
  }

  return (
    <div className="flex w-full flex-wrap items-center gap-4 rounded-lg border p-3">
      {qr && (
        // eslint-disable-next-line @next/next/no-img-element -- QR gerado localmente (data URL)
        <img
          src={qr}
          alt="QR code da tela do paciente"
          className="h-32 w-32 rounded bg-white p-1"
        />
      )}
      <div className="min-w-0 flex-1 space-y-2">
        <p className="text-sm font-medium">
          <span className="mr-2 inline-block h-2 w-2 animate-pulse rounded-full bg-red-500" />
          Transmitindo para a tela do paciente
        </p>
        <p className="text-xs text-muted-foreground">
          Aponte a câmera do tablet para o QR code ou abra o link nele. O link vale só enquanto esta
          tela estiver aberta.
        </p>
        <div className="flex flex-wrap gap-2">
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => {
              if (link)
                void navigator.clipboard
                  ?.writeText(link)
                  .then(() => toast.success("Link copiado."));
            }}
          >
            <Copy className="mr-2 h-4 w-4" /> Copiar link
          </Button>
          <Button type="button" variant="outline" size="sm" onClick={stop}>
            <X className="mr-2 h-4 w-4" /> Encerrar
          </Button>
        </div>
      </div>
    </div>
  );
}
