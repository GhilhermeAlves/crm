"use client";

import { useRef, useState } from "react";
import {
  Users,
  CalendarDays,
  FileText,
  DollarSign,
  ClipboardList,
  MoreHorizontal,
  Upload,
  CheckCircle2,
  Clock,
  AlertCircle,
  ArrowRight,
} from "lucide-react";
import type { LucideIcon } from "lucide-react";

import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

type MigrationStatus = "pending" | "uploading" | "done" | "error";

type MigrationCategory = {
  id: string;
  icon: LucideIcon;
  title: string;
  subtitle: string;
  description: string;
};

const CATEGORIES: MigrationCategory[] = [
  {
    id: "contacts",
    icon: Users,
    title: "Contatos",
    subtitle: "Cadastro de contatos",
    description: "Migre contatos",
  },
  {
    id: "agenda",
    icon: CalendarDays,
    title: "Agenda",
    subtitle: "Agendamentos atuais",
    description: "Migre agendamentos",
  },
  {
    id: "documents",
    icon: FileText,
    title: "Documentos",
    subtitle: "Histórico de documentos",
    description: "Migre documentos",
  },
  {
    id: "financial",
    icon: DollarSign,
    title: "Fluxo de caixa",
    subtitle: "Receitas e despesas",
    description: "Migre lançamentos",
  },
  {
    id: "services",
    icon: ClipboardList,
    title: "Serviços",
    subtitle: "Lista de itens e preços",
    description: "Migre serviços",
  },
  {
    id: "other",
    icon: MoreHorizontal,
    title: "Outros Arquivos",
    subtitle: "Arquivos adicionais",
    description: "Migre arquivos",
  },
];

const STATUS_CONFIG: Record<
  MigrationStatus,
  { label: string; icon: LucideIcon; className: string }
> = {
  pending: {
    label: "Aguardando envio",
    icon: Clock,
    className: "text-amber-600 bg-amber-50 dark:bg-amber-950/30",
  },
  uploading: {
    label: "Processando...",
    icon: Clock,
    className: "text-blue-600 bg-blue-50 dark:bg-blue-950/30",
  },
  done: {
    label: "Concluído",
    icon: CheckCircle2,
    className: "text-green-600 bg-green-50 dark:bg-green-950/30",
  },
  error: {
    label: "Erro no envio",
    icon: AlertCircle,
    className: "text-red-600 bg-red-50 dark:bg-red-950/30",
  },
};

function MigrationCard({ category }: { category: MigrationCategory }) {
  const [status, setStatus] = useState<MigrationStatus>("pending");
  const inputRef = useRef<HTMLInputElement>(null);

  const statusInfo = STATUS_CONFIG[status];
  const StatusIcon = statusInfo.icon;
  const Icon = category.icon;

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files || files.length === 0) return;
    setStatus("uploading");
    setTimeout(() => setStatus("done"), 2000);
  };

  return (
    <div className="flex flex-col items-center gap-3 rounded-xl border bg-card p-5 text-center transition-shadow hover:shadow-md">
      <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-muted">
        <Icon className="h-5 w-5 text-muted-foreground" />
      </div>
      <div>
        <h4 className="text-sm font-semibold">{category.title}</h4>
        <p className="text-xs text-muted-foreground">{category.subtitle}</p>
      </div>

      <div
        className={cn(
          "inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-medium",
          statusInfo.className,
        )}
      >
        <StatusIcon className="h-3 w-3" />
        {statusInfo.label}
      </div>

      <p className="text-xs text-muted-foreground">{category.description}</p>

      <input
        ref={inputRef}
        type="file"
        accept=".csv,.xlsx,.xls,.json"
        className="hidden"
        onChange={handleFileChange}
      />
      <Button
        size="sm"
        variant={status === "done" ? "outline" : "default"}
        className="w-full"
        onClick={() => inputRef.current?.click()}
        disabled={status === "uploading"}
      >
        <Upload className="mr-1.5 h-3.5 w-3.5" />
        {status === "done" ? "Reenviar" : "Enviar arquivos"}
      </Button>
    </div>
  );
}

export default function MigrationPage() {
  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <h2 className="text-xl font-semibold">Migração de dados</h2>

      <div className="rounded-lg border border-blue-200 bg-blue-50 p-3 text-sm text-blue-800 dark:border-blue-900 dark:bg-blue-950/30 dark:text-blue-300">
        Comece enviando os arquivos de Contatos.
      </div>

      <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-6">
        {CATEGORIES.map((cat) => (
          <MigrationCard key={cat.id} category={cat} />
        ))}
      </div>

      <div className="flex items-center gap-4 rounded-lg border bg-card p-4">
        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-amber-100 text-2xl dark:bg-amber-950/50">
          💡
        </div>
        <div className="flex-1">
          <p className="text-sm font-medium">
            Precisa de ajuda para importar seus arquivos?
          </p>
          <p className="text-xs text-muted-foreground">
            Siga nosso guia simples para baixar seus dados da plataforma
            anterior e começar a usar o CRM ao máximo!
          </p>
        </div>
        <Button variant="ghost" size="icon" className="shrink-0">
          <ArrowRight className="h-4 w-4" />
        </Button>
      </div>
    </div>
  );
}
