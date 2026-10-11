"use client";

import type { ReactNode } from "react";
import Link from "next/link";
import { ArrowLeft, Download } from "lucide-react";

import { ROUTES } from "@/lib/constants";

type ReportShellProps = {
  title: string;
  subtitle: string;
  panelTitle: string;
  onExport: () => void;
  children: ReactNode;
};

export function ReportShell({ title, subtitle, panelTitle, onExport, children }: ReportShellProps) {
  return (
    <div className="space-y-4">
      <Link
        href={ROUTES.REPORTS}
        className="inline-flex items-center gap-1 text-xs font-medium text-primary underline-offset-4 hover:underline"
      >
        <ArrowLeft className="h-3.5 w-3.5" />
        Voltar
      </Link>

      <div className="space-y-1">
        <h1 className="text-2xl font-bold tracking-tight">{title}</h1>
        <p className="text-sm text-muted-foreground">{subtitle}</p>
      </div>

      <div className="rounded-lg border bg-card shadow-sm">
        <div className="flex items-center justify-between border-b px-4 py-3">
          <h2 className="text-sm font-semibold">{panelTitle}</h2>
          <button
            type="button"
            onClick={onExport}
            aria-label="Baixar relatório em CSV"
            className="inline-flex h-8 w-8 items-center justify-center rounded-md text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
          >
            <Download className="h-4 w-4" />
          </button>
        </div>

        <div className="space-y-6 p-4">{children}</div>
      </div>
    </div>
  );
}
