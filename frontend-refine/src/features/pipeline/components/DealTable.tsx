"use client";

import { MoreHorizontal, Pencil, Trash2, GitBranch } from "lucide-react";
import type { Deal } from "../types/deal.types";
import { DealStageBadge } from "./DealStageBadge";
import { DealForecastBadge } from "./DealForecastBadge";
import { formatCurrency, formatDate } from "../schemas/deal.schema";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
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
      <div className="overflow-x-auto">
        <Table>
          <TableHeader>
            <TableRow className="bg-muted/50">
              <TableHead>Nome</TableHead>
              <TableHead>Tarefas</TableHead>
              <TableHead>Cronograma de atividades</TableHead>
              <TableHead>Etapa</TableHead>
              <TableHead>Resp.</TableHead>
              <TableHead className="text-right">Valor da negociação</TableHead>
              <TableHead>Contato</TableHead>
              <TableHead>Fechamento esperado</TableHead>
              <TableHead>Probabilidade</TableHead>
              <TableHead className="text-right">Valor previsto</TableHead>
              <TableHead>Última interação</TableHead>
              <TableHead>Cotações e faturas</TableHead>
              <TableHead>Categoria de previsão</TableHead>
              <TableHead className="w-[50px]"></TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {deals.map((deal) => (
              <TableRow key={deal.id}>
                <TableCell className="whitespace-nowrap font-medium">{deal.name}</TableCell>
                <TableCell className="whitespace-nowrap text-sm text-muted-foreground">
                  {deal.tasks ?? dash}
                </TableCell>
                <TableCell className="whitespace-nowrap text-sm text-muted-foreground">
                  {deal.schedule ?? dash}
                </TableCell>
                <TableCell>
                  <DealStageBadge stage={deal.stage} />
                </TableCell>
                <TableCell className="whitespace-nowrap text-sm">
                  {deal.responsible ?? dash}
                </TableCell>
                <TableCell className="whitespace-nowrap text-right font-medium">
                  {formatCurrency(deal.value)}
                </TableCell>
                <TableCell className="whitespace-nowrap text-sm">{deal.contact}</TableCell>
                <TableCell className="whitespace-nowrap text-sm text-muted-foreground">
                  {formatDate(deal.expectedCloseDate)}
                </TableCell>
                <TableCell className="whitespace-nowrap">
                  <span className="inline-flex min-w-[48px] items-center justify-center rounded-full bg-muted px-2 py-0.5 text-xs font-medium">
                    {deal.probability}%
                  </span>
                </TableCell>
                <TableCell className="whitespace-nowrap text-right text-sm">
                  {formatCurrency(deal.expectedValue)}
                </TableCell>
                <TableCell className="whitespace-nowrap text-sm text-muted-foreground">
                  {deal.lastInteraction ?? dash}
                </TableCell>
                <TableCell className="whitespace-nowrap text-sm text-muted-foreground">
                  {deal.quotesInvoices ?? dash}
                </TableCell>
                <TableCell>
                  <DealForecastBadge category={deal.forecastCategory} />
                </TableCell>
                <TableCell>
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
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>
    </Card>
  );
}
