"use client";

import { ChevronRight, FileText, Upload, type LucideIcon } from "lucide-react";

import { Card } from "@/components/ui/card";

type ActionRowProps = {
  icon: LucideIcon;
  label: string;
  onClick: () => void;
};

function ActionRow({ icon: Icon, label, onClick }: ActionRowProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="flex w-full items-center gap-3 px-4 py-3.5 text-left transition-colors first:rounded-t-lg last:rounded-b-lg hover:bg-muted/50"
    >
      <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-md bg-muted">
        <Icon className="h-4 w-4 text-muted-foreground" />
      </span>
      <span className="flex-1 text-sm font-medium">{label}</span>
      <ChevronRight className="h-4 w-4 text-muted-foreground" />
    </button>
  );
}

type FinanceActionsCardProps = {
  onAccountingReport: () => void;
  onImport: () => void;
};

export function FinanceActionsCard({ onAccountingReport, onImport }: FinanceActionsCardProps) {
  return (
    <Card className="divide-y">
      {/* etapa 7: abre o diálogo de relatório contábil */}
      <ActionRow icon={FileText} label="Gerar relatório contábil" onClick={onAccountingReport} />
      {/* etapa 6: abre o diálogo de envio/importação de tabela */}
      <ActionRow icon={Upload} label="Enviar tabela" onClick={onImport} />
    </Card>
  );
}
