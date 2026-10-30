"use client";

import { useEffect, useState } from "react";

/**
 * Texto que aparece letra por letra, como se alguém estivesse digitando, com
 * cursor piscando no fim. Leitores de tela recebem o texto inteiro de uma vez;
 * com "reduzir movimento" ligado no sistema, aparece pronto.
 */
export function TypewriterText({
  text,
  speedMs = 55,
  startDelayMs = 250,
}: {
  text: string;
  speedMs?: number;
  startDelayMs?: number;
}) {
  const [count, setCount] = useState(0);

  useEffect(() => {
    const reduce =
      typeof window !== "undefined" &&
      window.matchMedia?.("(prefers-reduced-motion: reduce)").matches;
    if (reduce) {
      setCount(text.length);
      return;
    }
    setCount(0);
    let interval: ReturnType<typeof setInterval> | undefined;
    const start = setTimeout(() => {
      interval = setInterval(() => {
        setCount((current) => {
          if (current >= text.length) {
            if (interval) clearInterval(interval);
            return current;
          }
          return current + 1;
        });
      }, speedMs);
    }, startDelayMs);
    return () => {
      clearTimeout(start);
      if (interval) clearInterval(interval);
    };
  }, [text, speedMs, startDelayMs]);

  const done = count >= text.length;

  return (
    <>
      <span className="sr-only">{text}</span>
      <span aria-hidden="true">
        {text.slice(0, count)}
        <span
          className={
            done
              ? "ml-0.5 inline-block w-[2px] animate-pulse bg-current align-middle opacity-60"
              : "ml-0.5 inline-block w-[2px] bg-current align-middle"
          }
          style={{ height: "1em" }}
        />
      </span>
    </>
  );
}
