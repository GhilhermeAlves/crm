"use client";

import { Sheet, SheetContent, SheetHeader, SheetTitle } from "@/components/ui/sheet";
import { DataTable } from "@/components/ui/data-table";
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
            <DataTable
              tableId="inventory-movements"
              data={data.content ?? []}
              bordered={false}
              columns={[
                {
                  id: "data",
                  header: "Data",
                  cell: (movement) => <>{new Date(movement.createdAt).toLocaleString("pt-BR")}</>,
                },
                {
                  id: "tipo",
                  header: "Tipo",
                  cell: (movement) => <>{STOCK_MOVEMENT_TYPE_LABELS[movement.type]}</>,
                },
                {
                  id: "delta",
                  header: "Delta",
                  headerClassName: "text-right",
                  className: "text-right",
                  cell: (movement) => <>{deltaBadge(movement.delta)}</>,
                },
                {
                  id: "observacao",
                  header: "Observação",
                  sortValue: (movement) => movement.note,
                  cell: (movement) => <>{movement.note ?? "—"}</>,
                },
              ]}
              keyExtractor={(movement) => movement.id}
            />
          ) : (
            <p className="text-sm text-muted-foreground">Nenhuma movimentação registrada.</p>
          )}
        </div>
      </SheetContent>
    </Sheet>
  );
}
