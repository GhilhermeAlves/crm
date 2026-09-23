"use client";

import { Boxes, Layers, Palette, ShieldCheck, Sparkles, SunMoon } from "lucide-react";
import { ThemeToggle } from "@/components/common/ThemeToggle";
import { ButtonSection } from "./sections/ButtonSection";
import { ColorsSection } from "./sections/ColorsSection";
import { DataDisplaySection } from "./sections/DataDisplaySection";
import { FeedbackSection } from "./sections/FeedbackSection";
import { FormSection } from "./sections/FormSection";
import { TypographySection } from "./sections/TypographySection";

export default function DesignSystemPage() {
  return (
    <div className="space-y-16 pb-16">
      {/* ========================================================================= */}
      {/* HERO HEADER COM BENTO-GRID & GLASSMORPHISM                                */}
      {/* ========================================================================= */}
      <div className="relative overflow-hidden rounded-2xl border bg-gradient-to-br from-card via-card to-muted/40 p-6 shadow-sm md:p-8">
        <div className="pointer-events-none absolute right-0 top-0 -mr-16 -mt-16 h-64 w-64 rounded-full bg-crm-primary/10 blur-3xl" />

        <div className="flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
          <div className="space-y-3">
            <div className="inline-flex items-center gap-2 rounded-full border border-crm-primary/30 bg-crm-primary/10 px-3 py-1 text-xs font-semibold text-crm-primary">
              <Sparkles className="h-3.5 w-3.5" />
              <span>Design System v1.0 • Live Foundations</span>
            </div>
            <h1 className="text-3xl font-extrabold tracking-tight text-foreground md:text-4xl">
              Sistema de Design do CRM
            </h1>
            <p className="max-w-2xl text-sm text-muted-foreground md:text-base">
              Catálogo visual vivo de tokens semânticos, componentes atômicos, tipografia e padrões
              de tela. Use como bancada de trabalho para testar novos componentes antes de plugá-los
              nos módulos.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-3">
            <div className="shadow-xs backdrop-blur-xs flex items-center gap-2 rounded-xl border bg-background/80 p-1.5">
              <SunMoon className="ml-2 h-4 w-4 text-muted-foreground" />
              <span className="text-xs font-medium text-muted-foreground">Alternar Tema:</span>
              <ThemeToggle />
            </div>
          </div>
        </div>

        {/* Bento Stats / Pills Row */}
        <div className="mt-8 grid grid-cols-2 gap-3 sm:grid-cols-4 md:gap-4">
          <div className="backdrop-blur-xs rounded-xl border bg-background/60 p-3.5">
            <div className="flex items-center gap-2 text-crm-primary">
              <Palette className="h-4 w-4" />
              <span className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                Tokens
              </span>
            </div>
            <p className="mt-1.5 text-2xl font-bold">24 Cores</p>
            <p className="text-[11px] text-muted-foreground">CSS Vars & Tailwind</p>
          </div>

          <div className="backdrop-blur-xs rounded-xl border bg-background/60 p-3.5">
            <div className="flex items-center gap-2 text-emerald-600 dark:text-emerald-400">
              <Boxes className="h-4 w-4" />
              <span className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                Componentes
              </span>
            </div>
            <p className="mt-1.5 text-2xl font-bold">20+ Base</p>
            <p className="text-[11px] text-muted-foreground">Radix UI + CVA</p>
          </div>

          <div className="backdrop-blur-xs rounded-xl border bg-background/60 p-3.5">
            <div className="flex items-center gap-2 text-amber-600 dark:text-amber-400">
              <ShieldCheck className="h-4 w-4" />
              <span className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                Acessibilidade
              </span>
            </div>
            <p className="mt-1.5 text-2xl font-bold">WCAG AA</p>
            <p className="text-[11px] text-muted-foreground">Contraste & ARIA nativo</p>
          </div>

          <div className="backdrop-blur-xs rounded-xl border bg-background/60 p-3.5">
            <div className="flex items-center gap-2 text-indigo-600 dark:text-indigo-400">
              <Layers className="h-4 w-4" />
              <span className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                Temas
              </span>
            </div>
            <p className="mt-1.5 text-2xl font-bold">Light / Dark</p>
            <p className="text-[11px] text-muted-foreground">Persistência automática</p>
          </div>
        </div>
      </div>

      <TypographySection />
      <ColorsSection />
      <ButtonSection />
      <FormSection />
      <FeedbackSection />
      <DataDisplaySection />
    </div>
  );
}
