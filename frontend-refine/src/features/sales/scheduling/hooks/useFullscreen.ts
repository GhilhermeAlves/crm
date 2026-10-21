"use client";

import { useCallback, useEffect, useRef, useState } from "react";

/** Liga/desliga a tela cheia do navegador para o elemento referenciado. */
export function useFullscreen<T extends HTMLElement>() {
  const ref = useRef<T>(null);
  const [isFullscreen, setIsFullscreen] = useState(false);
  // Lido só no cliente para não divergir da renderização do servidor.
  const [supported, setSupported] = useState(false);

  useEffect(() => {
    setSupported(!!document.fullscreenEnabled);
    const sync = () => setIsFullscreen(document.fullscreenElement === ref.current);
    document.addEventListener("fullscreenchange", sync);
    return () => document.removeEventListener("fullscreenchange", sync);
  }, []);

  const toggle = useCallback(() => {
    if (document.fullscreenElement) void document.exitFullscreen();
    else void ref.current?.requestFullscreen?.();
  }, []);

  return { ref, isFullscreen, toggle, supported };
}
