"use client";

import { cn } from "@/lib/utils";
import type { Dentition, ToothFace } from "../types/procedure.types";
import {
  DENTITION_LABELS,
  QUADRANTS,
  STATE_FILL,
  STATE_LABELS,
  STATE_TEXT,
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

const OUTLINE = "stroke-slate-400 dark:stroke-slate-500";

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
  const crownFill = wholeTooth ? STATE_FILL[summary.state] : "fill-white dark:fill-slate-200";
  const rootFill = wholeTooth ? STATE_FILL[summary.state] : "fill-slate-100 dark:fill-slate-400";
  return (
    <svg viewBox="0 0 40 80" className="h-16 w-8 drop-shadow-sm" aria-hidden>
      <g transform={isUpper(tooth) ? undefined : "matrix(1 0 0 -1 0 80)"}>
        <path
          d={root}
          className={cn(OUTLINE, rootFill, wholeTooth && "opacity-80")}
          strokeWidth={1.2}
          strokeLinejoin="round"
        />
        <path
          d={crown}
          className={cn(OUTLINE, crownFill)}
          strokeWidth={1.2}
          strokeLinejoin="round"
        />
        {/* Brilho do esmalte */}
        <path
          d="M16 50 C15 56 15 62 16.5 67"
          className="fill-none stroke-white/70"
          strokeWidth={1.6}
          strokeLinecap="round"
        />
      </g>
    </svg>
  );
}

function FaceDiagram({ tooth, summary }: { tooth: number; summary?: ToothSummary }) {
  const layout = faceLayout(tooth);
  const fillFor = (face: ToothFace) => {
    const state = summary?.faces[face];
    return state ? STATE_FILL[state] : "fill-white dark:fill-slate-200";
  };
  return (
    <svg viewBox="0 0 40 40" className="h-6 w-6" aria-hidden>
      {(Object.keys(FACE_SECTOR_PATHS) as (keyof typeof FACE_SECTOR_PATHS)[]).map((position) => (
        <path
          key={position}
          d={FACE_SECTOR_PATHS[position]}
          className={cn(OUTLINE, fillFor(layout[position]))}
          strokeWidth={1.2}
        />
      ))}
      <circle
        cx="20"
        cy="20"
        r="8"
        className={cn(OUTLINE, fillFor(layout.center))}
        strokeWidth={1.2}
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
  const number = (
    <span
      className={cn(
        "text-[11px] font-semibold tabular-nums transition-colors",
        summary ? STATE_TEXT[summary.state] : "text-muted-foreground group-hover:text-foreground",
      )}
    >
      {tooth}
    </span>
  );
  return (
    <button
      type="button"
      aria-label={label}
      aria-pressed={selected}
      title={label}
      onClick={onClick}
      className={cn(
        "group flex flex-col items-center gap-1 rounded-lg px-0.5 py-1.5 outline-none transition-all duration-200",
        "hover:bg-primary/5 focus-visible:ring-2 focus-visible:ring-ring",
        upper ? "hover:translate-y-0.5" : "hover:-translate-y-0.5",
        selected && "bg-primary/10 ring-2 ring-primary",
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
    <div className="space-y-4">
      {onDentitionChange && (
        <div className="flex justify-end">
          <div className="inline-flex rounded-lg border bg-muted/40 p-0.5">
            {(Object.keys(DENTITION_LABELS) as Dentition[]).map((value) => (
              <button
                key={value}
                type="button"
                aria-pressed={value === dentition}
                onClick={() => onDentitionChange(value)}
                className={cn(
                  "rounded-md px-3 py-1 text-sm font-medium transition-colors",
                  value === dentition
                    ? "bg-primary text-primary-foreground shadow-sm"
                    : "text-muted-foreground hover:text-foreground",
                )}
              >
                {DENTITION_LABELS[value]}
              </button>
            ))}
          </div>
        </div>
      )}
      <div className="overflow-x-auto rounded-xl bg-gradient-to-b from-muted/30 to-transparent p-3">
        <div className="mx-auto grid w-max grid-cols-2 gap-x-3 gap-y-3">
          <div className="flex justify-end border-b border-r border-dashed pb-3 pr-3">
            {row(upperRight)}
          </div>
          <div className="flex border-b border-dashed pb-3">{row(upperLeft)}</div>
          <div className="flex justify-end border-r border-dashed pr-3">
            {row(lowerRight.slice().reverse())}
          </div>
          <div className="flex">{row(lowerLeft)}</div>
        </div>
      </div>
      {showLegend && (
        <div className="flex flex-wrap justify-center gap-2 text-xs">
          {(Object.keys(STATE_LABELS) as (keyof typeof STATE_LABELS)[]).map((state) => (
            <span
              key={state}
              className="flex items-center gap-1.5 rounded-full border bg-background px-2.5 py-1 text-muted-foreground"
            >
              <svg viewBox="0 0 10 10" className="h-2.5 w-2.5" aria-hidden>
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
