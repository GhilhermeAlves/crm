"use client";

import { Sheet, SheetContent, SheetHeader, SheetTitle } from "@/components/ui/sheet";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { StatusBadge } from "@/components/ui/status-badge";
import { useItemMovements } from "../hooks/useInventory";
import { STOCK_MOVEMENT_TYPE_LABELS, type InventoryItem } from "../types/inventory.types";

type Props = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  companyId: string;
  item: InventoryItem;
};

function deltaBadge(delta: number) {
  if (delta > 0) return <StatusBadge intent="success">+{delta}</StatusBadge>;
  if (delta < 0) return <StatusBadge intent="danger">{delta}</StatusBadge>;
  return <StatusBadge intent="neutral">0</StatusBadge>;
}

export function MovementHistorySheet({ open, onOpenChange, companyId, item }: Props) {
  const { data, isLoading, error, refetch } = useItemMovements(companyId, open ? item.id : null);

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent className="w-full sm:max-w-lg">
        <SheetHeader>
          <SheetTitle>Histórico — {item.name}</SheetTitle>
        </SheetHeader>
        <div className="mt-4">
          {isLoading ? (
            <p className="text-sm text-muted-foreground">Carregando movimentos…</p>
          ) : error ? (
            <div className="space-y-2">
              <p className="text-sm text-destructive">Não foi possível carregar o histórico.</p>
              <button onClick={() => refetch()} className="text-sm underline">
                Tentar novamente
              </button>
            </div>
          ) : data && data.content.length > 0 ? (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Data</TableHead>
                  <TableHead>Tipo</TableHead>
                  <TableHead className="text-right">Delta</TableHead>
                  <TableHead>Observação</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.content.map((movement) => (
                  <TableRow key={movement.id}>
                    <TableCell>{new Date(movement.createdAt).toLocaleString("pt-BR")}</TableCell>
                    <TableCell>{STOCK_MOVEMENT_TYPE_LABELS[movement.type]}</TableCell>
                    <TableCell className="text-right">{deltaBadge(movement.delta)}</TableCell>
                    <TableCell>{movement.note ?? "—"}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          ) : (
            <p className="text-sm text-muted-foreground">Nenhuma movimentação registrada.</p>
          )}
        </div>
      </SheetContent>
    </Sheet>
  );
}