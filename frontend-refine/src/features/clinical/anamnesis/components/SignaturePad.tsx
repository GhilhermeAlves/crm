"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { Eraser } from "lucide-react";
import { Button } from "@/components/ui/button";

interface Props {
  /** PNG em data URL, ou null quando o quadro está vazio. */
  onChange: (dataUrl: string | null) => void;
}

/** Quadro de assinatura com dedo, caneta ou mouse (Pointer Events). */
export function SignaturePad({ onChange }: Props) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const drawing = useRef(false);
  const last = useRef<{ x: number; y: number } | null>(null);
  const [empty, setEmpty] = useState(true);

  const setup = useCallback(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    // Resolução real = tamanho na tela × densidade de pixels (traço nítido no celular).
    const ratio = window.devicePixelRatio || 1;
    const { width, height } = canvas.getBoundingClientRect();
    canvas.width = Math.round(width * ratio);
    canvas.height = Math.round(height * ratio);
    const ctx = canvas.getContext("2d");
    if (!ctx) return;
    ctx.scale(ratio, ratio);
    ctx.lineWidth = 2.5;
    ctx.lineCap = "round";
    ctx.lineJoin = "round";
    ctx.strokeStyle = "#111827";
    setEmpty(true);
    onChange(null);
  }, [onChange]);

  useEffect(() => {
    setup();
  }, [setup]);

  const point = (event: React.PointerEvent<HTMLCanvasElement>) => {
    const rect = event.currentTarget.getBoundingClientRect();
    return { x: event.clientX - rect.left, y: event.clientY - rect.top };
  };

  const start = (event: React.PointerEvent<HTMLCanvasElement>) => {
    event.currentTarget.setPointerCapture(event.pointerId);
    drawing.current = true;
    last.current = point(event);
  };

  const move = (event: React.PointerEvent<HTMLCanvasElement>) => {
    if (!drawing.current || !last.current) return;
    const ctx = canvasRef.current?.getContext("2d");
    if (!ctx) return;
    const next = point(event);
    ctx.beginPath();
    ctx.moveTo(last.current.x, last.current.y);
    ctx.lineTo(next.x, next.y);
    ctx.stroke();
    last.current = next;
    if (empty) setEmpty(false);
  };

  const end = () => {
    if (!drawing.current) return;
    drawing.current = false;
    last.current = null;
    if (!empty && canvasRef.current) onChange(canvasRef.current.toDataURL("image/png"));
  };

  return (
    <div className="space-y-2">
      <div className="relative">
        <canvas
          ref={canvasRef}
          aria-label="Quadro de assinatura"
          className="h-40 w-full touch-none rounded-md border-2 border-dashed bg-white"
          onPointerDown={start}
          onPointerMove={move}
          onPointerUp={end}
          onPointerLeave={end}
          onPointerCancel={end}
        />
        {empty && (
          <span className="pointer-events-none absolute inset-0 flex items-center justify-center text-sm text-muted-foreground">
            Assine aqui com o dedo
          </span>
        )}
      </div>
      <Button type="button" variant="ghost" size="sm" className="gap-1.5" onClick={setup}>
        <Eraser className="h-4 w-4" /> Limpar assinatura
      </Button>
    </div>
  );
}
