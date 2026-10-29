"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { Great_Vibes } from "next/font/google";
import { Eraser, PenLine, Type } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

const cursive = Great_Vibes({ weight: "400", subsets: ["latin"] });

interface Props {
  /** PNG em data URL, ou null quando o quadro está vazio. */
  onChange: (dataUrl: string | null) => void;
}

/**
 * Assinatura: em tela de toque, quadro para desenhar com o dedo; com mouse, o
 * paciente digita o nome e ele vira cursiva. Os dois modos geram o mesmo PNG.
 */
export function SignaturePad({ onChange }: Props) {
  const [mode, setMode] = useState<"draw" | "type" | null>(null);

  useEffect(() => {
    setMode(window.matchMedia("(pointer: coarse)").matches ? "draw" : "type");
  }, []);

  if (!mode) return null;
  return (
    <div className="space-y-2">
      {mode === "draw" ? <DrawPad onChange={onChange} /> : <TypedSignature onChange={onChange} />}
      <Button
        type="button"
        variant="link"
        size="sm"
        className="h-auto gap-1.5 px-0"
        onClick={() => {
          onChange(null);
          setMode(mode === "draw" ? "type" : "draw");
        }}
      >
        {mode === "draw" ? <Type className="h-4 w-4" /> : <PenLine className="h-4 w-4" />}
        {mode === "draw" ? "Prefiro digitar meu nome" : "Prefiro desenhar a assinatura"}
      </Button>
    </div>
  );
}

/** Nome digitado em cursiva; ao sair do campo vira PNG para o envio. */
function TypedSignature({ onChange }: Props) {
  const [name, setName] = useState("");
  const [editing, setEditing] = useState(true);

  const finish = async () => {
    const value = name.trim();
    setEditing(false);
    if (!value) {
      onChange(null);
      return;
    }
    const family = cursive.style.fontFamily;
    await document.fonts.load(`64px ${family}`).catch(() => undefined);
    const canvas = document.createElement("canvas");
    const ctx = canvas.getContext("2d");
    if (!ctx) return;
    ctx.font = `64px ${family}`;
    canvas.width = Math.ceil(ctx.measureText(value).width) + 40;
    canvas.height = 110;
    ctx.fillStyle = "#ffffff";
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    ctx.font = `64px ${family}`;
    ctx.fillStyle = "#111827";
    ctx.textBaseline = "middle";
    ctx.fillText(value, 20, canvas.height / 2);
    onChange(canvas.toDataURL("image/png"));
  };

  return (
    <div className="space-y-1">
      {editing || !name.trim() ? (
        <Input
          autoComplete="name"
          placeholder="Digite seu nome completo"
          value={name}
          onChange={(e) => setName(e.target.value)}
          onFocus={() => setEditing(true)}
          onBlur={() => void finish()}
        />
      ) : (
        <button
          type="button"
          aria-label="Editar assinatura"
          onClick={() => setEditing(true)}
          className={`${cursive.className} block w-full rounded-md border-b-2 bg-white px-4 py-2 text-left text-4xl text-gray-900`}
        >
          {name}
        </button>
      )}
      <p className="text-xs text-muted-foreground">Seu nome vira a assinatura em letra cursiva.</p>
    </div>
  );
}

/** Quadro de assinatura com dedo, caneta ou mouse (Pointer Events). */
function DrawPad({ onChange }: Props) {
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
