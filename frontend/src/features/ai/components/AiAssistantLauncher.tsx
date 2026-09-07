"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { Maximize2, Sparkles, X } from "lucide-react";
import { usePathname, useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from "@/components/ui/tooltip";
import { useLocalStorage } from "@/hooks/useLocalStorage";
import { ROUTES } from "@/lib/constants";
import { useAiPermissions } from "../hooks/useAi";
import { AiChatAssistant } from "./AiChatAssistant";

const BUTTON_SIZE = 56;
const MARGIN = 16;
const POSITION_KEY = "ai-assistant-float-position";

type Pos = { x: number; y: number } | null;

/**
 * Launcher flutuante do assistente Léo (AI-04). Um botão fixo arrastável que
 * abre o painel com o chat em modo {@code embedded}. Escondido na rota
 * {@code ROUTES.ASSISTANT} (a tela cheia do assistente é assumida lá) e sem
 * permissão {@code ai:chat}. A posição é persistida via {@code useLocalStorage}.
 */
export function AiAssistantLauncher() {
  const { canChat } = useAiPermissions();
  const pathname = usePathname();
  const router = useRouter();

  const [pos, setPos] = useLocalStorage<Pos>(POSITION_KEY, null);
  const [open, setOpen] = useState(false);
  const buttonRef = useRef<HTMLButtonElement>(null);
  const dragStart = useRef<{
    pointerId: number;
    clientX: number;
    clientY: number;
    originLeft: number;
    originTop: number;
  } | null>(null);
  const moved = useRef(false);

  useEffect(() => {
    if (!open) {
      return;
    }
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setOpen(false);
      }
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [open]);

  const isVisible = canChat && pathname !== ROUTES.ASSISTANT;
  if (!isVisible) {
    return null;
  }

  const toggleOpen = () => {
    if (moved.current) {
      return;
    }
    setOpen((value) => !value);
  };

  const onPointerDown = (event: React.PointerEvent<HTMLButtonElement>) => {
    const el = buttonRef.current;
    if (!el) {
      return;
    }
    const rect = el.getBoundingClientRect();
    dragStart.current = {
      pointerId: event.pointerId,
      clientX: event.clientX,
      clientY: event.clientY,
      originLeft: rect.left,
      originTop: rect.top,
    };
    moved.current = false;
    el.setPointerCapture(event.pointerId);
  };

  const onPointerMove = (event: React.PointerEvent<HTMLButtonElement>) => {
    if (!dragStart.current || event.pointerId !== dragStart.current.pointerId) {
      return;
    }
    const el = buttonRef.current;
    if (!el) {
      return;
    }
    const dx = event.clientX - dragStart.current.clientX;
    const dy = event.clientY - dragStart.current.clientY;
    if (Math.abs(dx) > 5 || Math.abs(dy) > 5) {
      moved.current = true;
    }
    if (moved.current) {
      const left = dragStart.current.originLeft + dx;
      const top = dragStart.current.originTop + dy;
      const maxX = window.innerWidth - BUTTON_SIZE - MARGIN;
      const maxY = window.innerHeight - BUTTON_SIZE - MARGIN;
      el.style.left = `${Math.max(MARGIN, Math.min(left, maxX))}px`;
      el.style.top = `${Math.max(MARGIN, Math.min(top, maxY))}px`;
    }
  };

  const onPointerUp = (event: React.PointerEvent<HTMLButtonElement>) => {
    const el = buttonRef.current;
    if (!el) {
      return;
    }
    if (event.pointerId === dragStart.current?.pointerId) {
      if (moved.current) {
        setPos({ x: parseInt(el.style.left, 10), y: parseInt(el.style.top, 10) });
      }
      el.releasePointerCapture(event.pointerId);
      dragStart.current = null;
    }
  };

  const positionStyle: React.CSSProperties = pos
    ? { left: pos.x, top: pos.y }
    : { right: MARGIN, bottom: MARGIN };

  return (
    <>
      <button
        ref={buttonRef}
        type="button"
        className="fixed z-50 flex h-14 w-14 touch-none select-none items-center justify-center rounded-full bg-primary text-primary-foreground shadow-lg shadow-primary/30 transition-colors hover:bg-primary/90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
        style={positionStyle}
        onPointerDown={onPointerDown}
        onPointerMove={onPointerMove}
        onPointerUp={onPointerUp}
        onPointerCancel={onPointerUp}
        onClick={toggleOpen}
        aria-label={open ? "Fechar assistente Léo" : "Abrir assistente Léo"}
        aria-expanded={open}
        aria-haspopup="dialog"
      >
        <Sparkles className="h-6 w-6 animate-pulse" />
      </button>
      {open && (
        <div className="fixed bottom-24 right-4 z-50 flex max-h-[min(80vh,560px)] w-[min(400px,calc(100vw-32px))] flex-col overflow-hidden rounded-xl border bg-card shadow-2xl">
          <div className="flex items-center justify-between border-b px-4 py-2">
            <span className="text-sm font-semibold">Léo · Assistente IA</span>
            <div className="flex items-center gap-1">
              <TooltipProvider delayDuration={200}>
                <Tooltip>
                  <TooltipTrigger asChild>
                    <Button
                      variant="ghost"
                      size="icon"
                      className="h-8 w-8"
                      onClick={() => {
                        setOpen(false);
                        router.push(ROUTES.ASSISTANT);
                      }}
                      aria-label="Abrir em tela cheia"
                    >
                      <Maximize2 className="h-4 w-4" />
                    </Button>
                  </TooltipTrigger>
                  <TooltipContent side="bottom">Abrir em tela cheia</TooltipContent>
                </Tooltip>
              </TooltipProvider>
              <Button
                variant="ghost"
                size="icon"
                className="h-8 w-8"
                onClick={() => setOpen(false)}
                aria-label="Fechar assistente Léo"
              >
                <X className="h-4 w-4" />
              </Button>
            </div>
          </div>
          <div className="min-h-0 flex-1">
            <AiChatAssistant embedded />
          </div>
        </div>
      )}
    </>
  );
}
