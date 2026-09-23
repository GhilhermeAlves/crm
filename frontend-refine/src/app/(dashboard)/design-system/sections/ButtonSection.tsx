"use client";

import { useState } from "react";
import { toast } from "sonner";
import { Copy, Plus, RefreshCw, SlidersHorizontal } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Switch } from "@/components/ui/switch";

export function ButtonSection() {
  const [interactiveLoading, setInteractiveLoading] = useState(false);
  const [interactiveDisabled, setInteractiveDisabled] = useState(false);
  const [buttonSize, setButtonSize] = useState<"sm" | "default" | "lg">("default");

  const handleCopy = (text: string, label: string) => {
    navigator.clipboard.writeText(text);
    toast.success(`${label} copiado!`, {
      description: text,
      duration: 2000,
    });
  };

  return (
    <section className="space-y-8" aria-labelledby="design-system-button-title">
      <h2 id="design-system-button-title" className="text-xl font-bold tracking-tight">
        Botões & Ações
      </h2>

      {/* Barra de Controles da Bancada de Testes */}
      <div className="flex flex-wrap items-center justify-between gap-4 rounded-xl border bg-muted/30 p-4">
        <div className="flex items-center gap-2">
          <SlidersHorizontal className="h-4 w-4 text-crm-primary" />
          <span className="text-sm font-semibold">Bancada de Testes Interativa:</span>
        </div>

        <div className="flex flex-wrap items-center gap-6">
          <div className="flex items-center gap-2">
            <Switch
              id="loading-toggle"
              checked={interactiveLoading}
              onCheckedChange={setInteractiveLoading}
            />
            <label htmlFor="loading-toggle" className="cursor-pointer text-xs font-medium">
              Simular Carregamento
            </label>
          </div>

          <div className="flex items-center gap-2">
            <Switch
              id="disabled-toggle"
              checked={interactiveDisabled}
              onCheckedChange={setInteractiveDisabled}
            />
            <label htmlFor="disabled-toggle" className="cursor-pointer text-xs font-medium">
              Simular Desabilitado
            </label>
          </div>

          <div className="flex items-center gap-1.5">
            <span className="text-xs text-muted-foreground">Tamanho:</span>
            {(["sm", "default", "lg"] as const).map((sz) => (
              <Button
                key={sz}
                size="sm"
                variant={buttonSize === sz ? "default" : "outline"}
                className="h-7 px-2 text-xs uppercase"
                onClick={() => setButtonSize(sz)}
              >
                {sz}
              </Button>
            ))}
          </div>
        </div>
      </div>

      {/* Botões */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between pb-2">
          <div>
            <CardTitle className="text-base">Botões (Button Variants)</CardTitle>
            <CardDescription>Ações primárias, secundárias, destrutivas e sutis.</CardDescription>
          </div>
          <Button
            variant="ghost"
            size="sm"
            className="gap-1 text-xs text-muted-foreground"
            onClick={() =>
              handleCopy(
                `<Button variant="crm" size="${buttonSize}">Ação Primária</Button>`,
                "Código JSX",
              )
            }
          >
            <Copy className="h-3 w-3" /> Copiar Exemplo
          </Button>
        </CardHeader>
        <CardContent className="space-y-6 pt-4">
          <div className="flex flex-wrap items-center gap-3">
            <Button variant="crm" size={buttonSize} disabled={interactiveDisabled}>
              {interactiveLoading ? (
                <RefreshCw className="mr-2 h-4 w-4 animate-spin" />
              ) : (
                <Plus className="mr-2 h-4 w-4" />
              )}
              CRM Primary
            </Button>

            <Button variant="default" size={buttonSize} disabled={interactiveDisabled}>
              Default
            </Button>

            <Button variant="secondary" size={buttonSize} disabled={interactiveDisabled}>
              Secundário
            </Button>

            <Button variant="outline" size={buttonSize} disabled={interactiveDisabled}>
              Outline
            </Button>

            <Button variant="ghost" size={buttonSize} disabled={interactiveDisabled}>
              Ghost
            </Button>

            <Button variant="destructive" size={buttonSize} disabled={interactiveDisabled}>
              Destrutivo
            </Button>

            <Button
              variant="crm"
              size="icon"
              className={
                buttonSize === "sm" ? "h-8 w-8" : buttonSize === "lg" ? "h-11 w-11" : "h-10 w-10"
              }
              disabled={interactiveDisabled}
            >
              <Plus className="h-4 w-4" />
            </Button>
          </div>
        </CardContent>
      </Card>
    </section>
  );
}
