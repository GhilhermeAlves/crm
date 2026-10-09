"use client";

import { useEffect, useRef } from "react";

/**
 * Fundo animado da página inicial: um "funil" de fios brancos finos que desce
 * do topo, estreita num pescoço e se abre num piso elíptico, girando devagar.
 * Canvas 2D próprio (sem libs). Respeita prefers-reduced-motion (desenha um
 * quadro estático) e pausa quando a aba fica oculta.
 */
const STRANDS = 260;
const SEGMENTS = 110;
const SPEED = 0.06; // rad/s

export function VortexBackground({ className }: { className?: string }) {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    const ctx = canvas?.getContext("2d");
    if (!canvas || !ctx) return;

    const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    const strands = Array.from({ length: STRANDS }, (_, i) => ({
      angle: (i / STRANDS) * Math.PI * 2,
      dir: i % 2 === 0 ? 1 : -1, // duas famílias em sentidos opostos → trama cruzada
      alpha: 0.05 + Math.random() * 0.1,
    }));
    const stars = Array.from({ length: 70 }, () => ({
      x: Math.random(),
      y: Math.random() * 0.7,
      a: 0.15 + Math.random() * 0.45,
    }));

    let width = 0;
    let height = 0;
    let frame = 0;
    let running = true;

    const resize = () => {
      const dpr = Math.min(window.devicePixelRatio || 1, 2);
      width = canvas.clientWidth;
      height = canvas.clientHeight;
      canvas.width = Math.round(width * dpr);
      canvas.height = Math.round(height * dpr);
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    };

    const draw = (time: number) => {
      ctx.globalCompositeOperation = "source-over";
      ctx.fillStyle = "#000";
      ctx.fillRect(0, 0, width, height);

      for (const s of stars) {
        ctx.fillStyle = `rgba(255,255,255,${s.a})`;
        ctx.fillRect(s.x * width, s.y * height, 1, 1);
      }

      const cx = width / 2;
      const topY = -height * 0.08;
      const neckY = height * 0.68;
      const floorY = height * 1.02;
      const topR = Math.max(width * 0.34, 260);
      const neckR = Math.max(width * 0.045, 28);
      const floorR = Math.max(width * 0.75, 420);
      const spin = time * SPEED;

      ctx.globalCompositeOperation = "lighter";
      ctx.lineWidth = 0.6;
      for (const s of strands) {
        ctx.beginPath();
        for (let k = 0; k <= SEGMENTS; k++) {
          const t = k / SEGMENTS;
          const y = topY + (floorY - topY) * t;
          let r: number;
          let squash: number;
          let twist: number;
          if (y < neckY) {
            const u = (neckY - y) / (neckY - topY); // 1 no topo → 0 no pescoço
            r = neckR + (topR - neckR) * Math.pow(u, 1.7);
            squash = 0.1;
            twist = (1 - u) * 2.4;
          } else {
            const u = (y - neckY) / (floorY - neckY); // 0 no pescoço → 1 no piso
            r = neckR + (floorR - neckR) * Math.pow(u, 2.4);
            squash = 0.16;
            twist = 2.4 + u * 1.8;
          }
          const phi = s.angle + s.dir * (twist + spin);
          const x = cx + r * Math.cos(phi);
          const py = y + r * Math.sin(phi) * squash;
          if (k === 0) ctx.moveTo(x, py);
          else ctx.lineTo(x, py);
        }
        ctx.strokeStyle = `rgba(255,255,255,${s.alpha})`;
        ctx.stroke();
      }

      // vinheta para o texto respirar e as bordas sumirem no preto
      ctx.globalCompositeOperation = "source-over";
      const vignette = ctx.createRadialGradient(
        cx,
        height * 0.45,
        height * 0.1,
        cx,
        height * 0.5,
        width * 0.75,
      );
      vignette.addColorStop(0, "rgba(0,0,0,0)");
      vignette.addColorStop(1, "rgba(0,0,0,0.85)");
      ctx.fillStyle = vignette;
      ctx.fillRect(0, 0, width, height);
    };

    const loop = (now: number) => {
      if (!running) return;
      draw(now / 1000);
      frame = requestAnimationFrame(loop);
    };

    const onVisibility = () => {
      if (reduceMotion) return;
      running = !document.hidden;
      cancelAnimationFrame(frame);
      if (running) frame = requestAnimationFrame(loop);
    };

    const observer = new ResizeObserver(() => {
      resize();
      if (reduceMotion) draw(0);
    });
    observer.observe(canvas);
    resize();

    if (reduceMotion) draw(0);
    else frame = requestAnimationFrame(loop);
    document.addEventListener("visibilitychange", onVisibility);

    return () => {
      running = false;
      cancelAnimationFrame(frame);
      observer.disconnect();
      document.removeEventListener("visibilitychange", onVisibility);
    };
  }, []);

  return <canvas ref={canvasRef} aria-hidden="true" className={className} />;
}
