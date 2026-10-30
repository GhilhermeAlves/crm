"use client";

import { useEffect, useState } from "react";
import { Loader2, Maximize } from "lucide-react";
import { LiveViewService } from "../services/live-view.service";

const POLL_MS = 100;
const RETRY_MS = 1000;

/**
 * Tela cheia no tablet do paciente. Busca o quadro mais recente em sequência
 * (o próximo pedido só sai depois do anterior) e mantém o último visível.
 */
export function LiveViewScreen({ token }: { token: string }) {
  const [src, setSrc] = useState<string | null>(null);
  const [ended, setEnded] = useState(false);

  useEffect(() => {
    let cancelled = false;
    let seq = 0;
    let current: string | null = null;
    let timer: ReturnType<typeof setTimeout> | undefined;

    const poll = async () => {
      let delay = POLL_MS;
      try {
        const result = await LiveViewService.frame(token, seq);
        if (cancelled) return;
        if (result.status === "ended") {
          setEnded(true);
          return;
        }
        if (result.status === "frame") {
          seq = result.seq;
          const url = URL.createObjectURL(result.blob);
          setSrc(url);
          if (current) URL.revokeObjectURL(current);
          current = url;
        }
      } catch {
        delay = RETRY_MS;
      }
      if (!cancelled) timer = setTimeout(() => void poll(), delay);
    };
    void poll();

    return () => {
      cancelled = true;
      if (timer) clearTimeout(timer);
      if (current) URL.revokeObjectURL(current);
    };
  }, [token]);

  const fullscreen = () => {
    void document.documentElement.requestFullscreen?.().catch(() => undefined);
  };

  return (
    <div className="fixed inset-0 flex items-center justify-center bg-black text-white">
      {ended ? (
        <p className="px-6 text-center text-lg">
          A transmissão foi encerrada. Peça um novo QR code à clínica.
        </p>
      ) : src ? (
        // eslint-disable-next-line @next/next/no-img-element -- quadro ao vivo (blob:)
        <img
          src={src}
          alt="Imagem ao vivo do tricoscópio"
          className="h-full w-full object-contain"
        />
      ) : (
        <p className="flex items-center gap-2 text-lg">
          <Loader2 className="h-5 w-5 animate-spin" /> Aguardando a câmera da clínica…
        </p>
      )}
      {!ended && (
        <button
          type="button"
          onClick={fullscreen}
          className="absolute bottom-4 right-4 rounded-full bg-white/15 p-3 backdrop-blur"
          aria-label="Tela cheia"
        >
          <Maximize className="h-5 w-5" />
        </button>
      )}
    </div>
  );
}
