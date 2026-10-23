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
import { FACE_SECTOR_PATHS, TOOTH_PATHS, faceLayout, isUpper, toothKind } from "./toothShapes";

type OdontogramProps = {
  dentition: Dentition;
  onDentitionChange?: (dentition: Dentition) => void;
  summary: Map<number, ToothSummary>;
  selected: number[];
  onToothClick: (tooth: number) => void;
  showLegend?: boolean;
};

function ToothDrawing({
  tooth,
  dentition,
  summary,
}: {
  tooth: number;
  dentition: Dentition;
  summary?: ToothSummary;
}) {
  const { root, crown } = TOOTH_PATHS[toothKind(tooth, dentition)];
  // Procedimento sem faces (ex.: canal, extração) pinta o dente inteiro.
  const wholeTooth = summary && Object.keys(summary.faces).length === 0;
  const fill = wholeTooth ? STATE_FILL[summary.state] : "fill-card";
  return (
    <svg viewBox="0 0 40 80" className="h-14 w-7" aria-hidden>
      <g transform={isUpper(tooth) ? undefined : "matrix(1 0 0 -1 0 80)"}>
        <path d={root} className={cn("stroke-muted-foreground", fill)} strokeWidth={1.5} />
        <path d={crown} className={cn("stroke-muted-foreground", fill)} strokeWidth={1.5} />
      </g>
    </svg>
  );
}

function FaceDiagram({ tooth, summary }: { tooth: number; summary?: ToothSummary }) {
  const layout = faceLayout(tooth);
  const fillFor = (face: ToothFace) => {
    const state = summary?.faces[face];
    return state ? STATE_FILL[state] : "fill-card";
  };
  return (
    <svg viewBox="0 0 40 40" className="h-7 w-7" aria-hidden>
      {(Object.keys(FACE_SECTOR_PATHS) as (keyof typeof FACE_SECTOR_PATHS)[]).map((position) => (
        <path
          key={position}
          d={FACE_SECTOR_PATHS[position]}
          className={cn("stroke-muted-foreground", fillFor(layout[position]))}
          strokeWidth={1}
        />
      ))}
      <circle
        cx="20"
        cy="20"
        r="8"
        className={cn("stroke-muted-foreground", fillFor(layout.center))}
        strokeWidth={1}
      />
    </svg>
  );
}

function Tooth({
  tooth,
  dentition,
  summary,
  selected,
  onClick,
}: {
  tooth: number;
  dentition: Dentition;
  summary?: ToothSummary;
  selected: boolean;
  onClick: () => void;
}) {
  const label = summary ? `Dente ${tooth}, ${STATE_LABELS[summary.state]}` : `Dente ${tooth}`;
  const upper = isUpper(tooth);
  const number = <span className="text-xs text-muted-foreground">{tooth}</span>;
  return (
    <button
      type="button"
      aria-label={label}
      aria-pressed={selected}
      onClick={onClick}
      className={cn(
        "flex flex-col items-center gap-1 rounded-md px-0.5 py-1 hover:bg-muted",
        selected && "ring-2 ring-ring",
      )}
    >
      {upper ? (
        <>
          {number}
          <ToothDrawing tooth={tooth} dentition={dentition} summary={summary} />
          <FaceDiagram tooth={tooth} summary={summary} />
        </>
      ) : (
        <>
          <FaceDiagram tooth={tooth} summary={summary} />
          <ToothDrawing tooth={tooth} dentition={dentition} summary={summary} />
          {number}
        </>
      )}
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
        dentition={dentition}
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
