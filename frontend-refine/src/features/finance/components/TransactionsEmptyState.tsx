"use client";

import { Calculator, Plus, Upload } from "lucide-react";

import { Button } from "@/components/ui/button";

type TransactionsEmptyStateProps = {
  onNew: () => void;
  onImport: () => void;
};

export function TransactionsEmptyState({ onNew, onImport }: TransactionsEmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center px-6 py-16 text-center">
      <div className="mb-4 flex h-20 w-20 items-center justify-center rounded-full bg-muted">
        <Calculator className="h-10 w-10 text-muted-foreground" />
      </div>
      <h3 className="text-base font-semibold">Nenhum lançamento financeiro encontrado</h3>
      <p className="mt-1 max-w-sm text-sm text-muted-foreground">
        Envie sua tabela com essas informações ou adicione um lançamento manualmente.
      </p>
      <div className="mt-5 flex flex-wrap items-center justify-center gap-2">
        <Button variant="outline" onClick={onNew}>
          <Plus className="h-4 w-4" />
          Novo lançamento
        </Button>
        <Button onClick={onImport}>
          <Upload className="h-4 w-4" />
          Enviar tabela
        </Button>
      </div>
    </div>
  );
}
