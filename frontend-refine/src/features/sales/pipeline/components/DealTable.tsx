"use client";

import { MoreHorizontal, Pencil, Trash2, GitBranch } from "lucide-react";
import type { Deal } from "../types/deal.types";
import { DealStageBadge } from "./DealStageBadge";
import { DealForecastBadge } from "./DealForecastBadge";
import { formatCurrency, formatDate } from "../schemas/deal.schema";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { DataTable } from "@/components/ui/data-table";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

type DealTableProps = {
  deals: Deal[];
  groupTitle: string;
  onEdit?: (deal: Deal) => void;
  onDelete?: (deal: Deal) => void;
  onChangeStage?: (deal: Deal) => void;
};

const dash = <span className="text-muted-foreground">—</span>;

export function DealTable({ deals, groupTitle, onEdit, onDelete, onChangeStage }: DealTableProps) {
  return (
    <Card className="overflow-hidden">
      <div className="flex items-center justify-between border-b px-4 py-3">
        <h2 className="text-sm font-semibold">{groupTitle}</h2>
        <span className="rounded-full bg-muted px-2 py-0.5 text-xs text-muted-foreground">
          {deals.length} {deals.length === 1 ? "negociação" : "negociações"}
        </span>
      </div>
      <DataTable
        tableId="deals"
        data={deals ?? []}
        bordered={false}
        columns={[
          {
            id: "nome",
            header: "Nome",
            className: "whitespace-nowrap font-medium",
            sortValue: (deal) => deal.name,
            cell: (deal) => <>{deal.name}</>,
          },
          {
            id: "tarefas",
            header: "Tarefas",
            className: "whitespace-nowrap text-sm text-muted-foreground",
            sortValue: (deal) => deal.tasks,
            cell: (deal) => <>{deal.tasks ?? dash}</>,
          },
          {
            id: "cronograma-de-atividades",
            header: "Cronograma de atividades",
            className: "whitespace-nowrap text-sm text-muted-foreground",
            sortValue: (deal) => deal.schedule,
            cell: (deal) => <>{deal.schedule ?? dash}</>,
          },
          { id: "etapa", header: "Etapa", cell: (deal) => <DealStageBadge stage={deal.stage} /> },
          {
            id: "resp",
            header: "Resp.",
            className: "whitespace-nowrap text-sm",
            sortValue: (deal) => deal.responsible,
            cell: (deal) => <>{deal.responsible ?? dash}</>,
          },
          {
            id: "valor-da-negociacao",
            header: "Valor da negociação",
            headerClassName: "text-right",
            className: "whitespace-nowrap text-right font-medium",
            sortValue: (deal) => deal.value,
            cell: (deal) => <>{formatCurrency(deal.value)}</>,
          },
          {
            id: "contato",
            header: "Contato",
            className: "whitespace-nowrap text-sm",
            sortValue: (deal) => deal.contact,
            cell: (deal) => <>{deal.contact}</>,
          },
          {
            id: "fechamento-esperado",
            header: "Fechamento esperado",
            className: "whitespace-nowrap text-sm text-muted-foreground",
            sortValue: (deal) => deal.expectedCloseDate,
            cell: (deal) => <>{formatDate(deal.expectedCloseDate)}</>,
          },
          {
            id: "probabilidade",
            header: "Probabilidade",
            className: "whitespace-nowrap",
            cell: (deal) => (
              <span className="inline-flex min-w-[48px] items-center justify-center rounded-full bg-muted px-2 py-0.5 text-xs font-medium">
                {deal.probability}%
              </span>
            ),
          },
          {
            id: "valor-previsto",
            header: "Valor previsto",
            headerClassName: "text-right",
            className: "whitespace-nowrap text-right text-sm",
            sortValue: (deal) => deal.expectedValue,
            cell: (deal) => <>{formatCurrency(deal.expectedValue)}</>,
          },
          {
            id: "ultima-interacao",
            header: "Última interação",
            className: "whitespace-nowrap text-sm text-muted-foreground",
            sortValue: (deal) => deal.lastInteraction,
            cell: (deal) => <>{deal.lastInteraction ?? dash}</>,
          },
          {
            id: "cotacoes-e-faturas",
            header: "Cotações e faturas",
            className: "whitespace-nowrap text-sm text-muted-foreground",
            sortValue: (deal) => deal.quotesInvoices,
            cell: (deal) => <>{deal.quotesInvoices ?? dash}</>,
          },
          {
            id: "categoria-de-previsao",
            header: "Categoria de previsão",
            cell: (deal) => <DealForecastBadge category={deal.forecastCategory} />,
          },
          {
            id: "acoes",
            header: "",
            label: "Ações",
            hideable: false,
            headerClassName: "w-[50px]",
            cell: (deal) => (
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button variant="ghost" size="icon" className="h-8 w-8">
                    <MoreHorizontal className="h-4 w-4" />
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end">
                  <DropdownMenuItem onClick={() => onEdit?.(deal)}>
                    <Pencil className="mr-2 h-4 w-4" />
                    Editar
                  </DropdownMenuItem>
                  <DropdownMenuItem onClick={() => onChangeStage?.(deal)}>
                    <GitBranch className="mr-2 h-4 w-4" />
                    Alterar etapa
                  </DropdownMenuItem>
                  {onDelete && (
                    <>
                      <DropdownMenuSeparator />
                      <DropdownMenuItem
                        onClick={() => onDelete?.(deal)}
                        className="text-destructive"
                      >
                        <Trash2 className="mr-2 h-4 w-4" />
                        Excluir
                      </DropdownMenuItem>
                    </>
                  )}
                </DropdownMenuContent>
              </DropdownMenu>
            ),
          },
        ]}
        keyExtractor={(deal) => deal.id}
      />
    </Card>
  );
}
