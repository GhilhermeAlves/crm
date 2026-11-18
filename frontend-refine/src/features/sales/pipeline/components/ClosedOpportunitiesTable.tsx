"use client";

import { Badge } from "@/components/ui/badge";
import { DataTable, type ColumnDef } from "@/components/ui/data-table";
import type { Opportunity } from "../types/pipeline.types";

const currency = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });

function closedAt(o: Opportunity): string | null {
  return o.status === "WON" ? o.wonAt : o.lostAt;
}

function formatDate(value: string | null): string {
  if (!value) return "—";
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? "—" : d.toLocaleDateString("pt-BR");
}

const columns: ColumnDef<Opportunity>[] = [
  {
    id: "title",
    header: "Oportunidade",
    hideable: false,
    className: "font-medium",
    sortValue: (o) => o.title,
    cell: (o) => o.title,
  },
  {
    id: "result",
    header: "Resultado",
    sortValue: (o) => o.status,
    cell: (o) =>
      o.status === "WON" ? (
        <Badge className="bg-emerald-600 hover:bg-emerald-600">Ganha</Badge>
      ) : (
        <Badge variant="destructive">Perdida</Badge>
      ),
  },
  {
    id: "value",
    header: "Valor",
    sortValue: (o) => o.value,
    cell: (o) => currency.format(o.value),
  },
  {
    id: "stage",
    header: "Etapa",
    className: "text-sm text-muted-foreground",
    sortValue: (o) => o.stageName,
    cell: (o) => o.stageName ?? "—",
  },
  {
    id: "lossReason",
    header: "Motivo da perda",
    className: "text-sm text-muted-foreground",
    cell: (o) => o.lossReason ?? "—",
  },
  {
    id: "closedAt",
    header: "Concluída em",
    className: "text-sm text-muted-foreground",
    sortValue: (o) => closedAt(o),
    cell: (o) => formatDate(closedAt(o)),
  },
];

export function ClosedOpportunitiesTable({ opportunities }: { opportunities: Opportunity[] }) {
  return (
    <DataTable
      tableId="opportunities-closed"
      data={opportunities}
      columns={columns}
      keyExtractor={(o) => o.id}
      rowClassName={(o) =>
        o.status === "LOST"
          ? "border-l-4 border-l-red-600 bg-red-600/15 hover:bg-red-600/20"
          : "border-l-4 border-l-emerald-600"
      }
      emptyState={{ title: "Nenhuma oportunidade concluída ainda." }}
    />
  );
}
