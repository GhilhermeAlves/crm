"use client";

import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import type { Dentition, ToothFace } from "../types/procedure.types";
import {
  DENTITION_LABELS,
  QUADRANTS,
  STATE_FILL,
  STATE_LABELS,
  type ToothSummary,
} from "../utils/teeth";

type OdontogramProps = {
  dentition: Dentition;
  onDentitionChange?: (dentition: Dentition) => void;
  summary: Map<number, ToothSummary>;
  selected: number[];
  onToothClick: (tooth: number) => void;
  showLegend?: boolean;
};

// Setores de um dente 40x40: centro = oclusal/incisal; bordas = V (cima), L (baixo), M/D (lados).
const FACE_PATHS: Record<ToothFace, string> = {
  V: "M0 0 L40 0 L30 10 L10 10 Z",
  L: "M0 40 L10 30 L30 30 L40 40 Z",
  M: "M0 0 L10 10 L10 30 L0 40 Z",
  D: "M40 0 L40 40 L30 30 L30 10 Z",
  O: "M10 10 L30 10 L30 30 L10 30 Z",
};

function Tooth({
  tooth,
  summary,
  selected,
  onClick,
}: {
  tooth: number;
  summary?: ToothSummary;
  selected: boolean;
  onClick: () => void;
}) {
  const label = summary ? `Dente ${tooth}, ${STATE_LABELS[summary.state]}` : `Dente ${tooth}`;
  return (
    <button
      type="button"
      aria-label={label}
      aria-pressed={selected}
      onClick={onClick}
      className={cn(
        "flex flex-col items-center gap-1 rounded-md p-1 hover:bg-muted",
        selected && "ring-2 ring-ring",
      )}
    >
      <svg viewBox="0 0 40 40" className="h-8 w-8" aria-hidden>
        {(Object.keys(FACE_PATHS) as ToothFace[]).map((face) => {
          const faceState = summary?.faces[face];
          return (
            <path
              key={face}
              d={FACE_PATHS[face]}
              className={cn("stroke-border", faceState ? STATE_FILL[faceState] : "fill-card")}
            />
          );
        })}
        {summary && Object.keys(summary.faces).length === 0 && (
          <circle cx="20" cy="20" r="6" className={STATE_FILL[summary.state]} />
        )}
      </svg>
      <span className="text-xs text-muted-foreground">{tooth}</span>
    </button>
  );
}

export function Odontogram({
  dentition,
  onDentitionChange,
  summary,
  selected,
  onToothClick,
  showLegend = true,
}: OdontogramProps) {
  const [upperRight, upperLeft, lowerLeft, lowerRight] = QUADRANTS[dentition];
  const row = (teeth: number[]) =>
    teeth.map((tooth) => (
      <Tooth
        key={tooth}
        tooth={tooth}
        summary={summary.get(tooth)}
        selected={selected.includes(tooth)}
        onClick={() => onToothClick(tooth)}
      />
    ));

  return (
    <div className="space-y-3">
      {onDentitionChange && (
        <div className="flex gap-2">
          {(Object.keys(DENTITION_LABELS) as Dentition[]).map((value) => (
            <Button
              key={value}
              type="button"
              size="sm"
              variant={value === dentition ? "default" : "outline"}
              onClick={() => onDentitionChange(value)}
            >
              {DENTITION_LABELS[value]}
            </Button>
          ))}
        </div>
      )}
      <div className="overflow-x-auto">
        <div className="inline-grid min-w-max grid-cols-2 gap-x-4 gap-y-2">
          <div className="flex justify-end border-b border-r pb-2 pr-2">{row(upperRight)}</div>
          <div className="flex border-b pb-2">{row(upperLeft)}</div>
          <div className="flex justify-end border-r pr-2">{row(lowerRight.slice().reverse())}</div>
          <div className="flex">{row(lowerLeft)}</div>
        </div>
      </div>
      {showLegend && (
        <div className="flex flex-wrap gap-4 text-xs text-muted-foreground">
          {(Object.keys(STATE_LABELS) as (keyof typeof STATE_LABELS)[]).map((state) => (
            <span key={state} className="flex items-center gap-1">
              <svg viewBox="0 0 10 10" className="h-3 w-3" aria-hidden>
                <circle cx="5" cy="5" r="5" className={STATE_FILL[state]} />
              </svg>
              {STATE_LABELS[state]}
            </span>
          ))}
        </div>
      )}
    </div>
  );
}
