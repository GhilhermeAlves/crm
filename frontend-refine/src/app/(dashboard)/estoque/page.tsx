"use client";

import { useState } from "react";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { useAuthorization } from "@/features/identity/auth/hooks/useAuthorization";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import { SearchInput } from "@/components/common/SearchInput";
import { EmptyState } from "@/components/common/EmptyState";
import { ErrorCard } from "@/components/common/ErrorCard";
import { SkeletonTable } from "@/components/feedback/SkeletonTable";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { useInventory, useRegisterMovement } from "@/features/masterdata/inventory/hooks/useInventory";
import { StockMovementDialog } from "@/features/masterdata/inventory/components/StockMovementDialog";
import { MovementHistorySheet } from "@/features/masterdata/inventory/components/MovementHistorySheet";
import { LowStockBadge } from "@/features/masterdata/inventory/components/LowStockBadge";
import {
  UNIT_LABELS,
  type InventoryItem,
} from "@/features/masterdata/inventory/types/inventory.types";
import {
  CATALOG_CATEGORIES,
  CATALOG_CATEGORY_LABELS,
  type CatalogCategory,
} from "@/features/masterdata/catalog/types/catalog.types";

const ALL = "ALL";
const PAGE_SIZE = 20;

function categoryLabel(category: string | null): string {
  if (!category) return "—";
  return CATALOG_CATEGORY_LABELS[category as CatalogCategory] ?? category;
}

export default function EstoquePage() {
  const { user } = useAuth();
  const { can } = useAuthorization();
  const companyId = user?.companyId ?? null;
  const canManage = can("inventory:manage");

  const [search, setSearch] = useState("");
  const debouncedSearch = useDebouncedValue(search, 300);
  const [category, setCategory] = useState("");
  const [lowStock, setLowStock] = useState<boolean | undefined>(undefined);
  const [page, setPage] = useState(0);

  const { data, isLoading, error, refetch } = useInventory(companyId, {
    q: debouncedSearch,
    category: category || undefined,
    lowStock,
    page,
    pageSize: PAGE_SIZE,
  });

  const registerMovement = useRegisterMovement(companyId);

  const [movementItem, setMovementItem] = useState<InventoryItem | null>(null);
  const [historyItem, setHistoryItem] = useState<InventoryItem | null>(null);

  const resetPage = (setter: (v: string) => void) => (v: string) => {
    setPage(0);
    setter(v);
  };

  const totalPages = data?.totalPages ?? 0;

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold tracking-tight">Estoque</h1>

      <div className="flex flex-wrap items-center gap-2">
        <SearchInput
          aria-label="Buscar no estoque"
          placeholder="Buscar no estoque"
          value={search}
          onChange={(e) => resetPage(setSearch)(e.target.value)}
        />
        <Select
          value={category || ALL}
          onValueChange={(v) => {
            setPage(0);
            setCategory(v === ALL ? "" : v);
          }}
        >
          <SelectTrigger className="w-[200px]" aria-label="Filtrar por categoria">
            <SelectValue placeholder="Todas as categorias" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={ALL}>Todas as categorias</SelectItem>
            {CATALOG_CATEGORIES.map((c) => (
              <SelectItem key={c} value={c}>
                {CATALOG_CATEGORY_LABELS[c]}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Button
          variant={lowStock ? "default" : "outline"}
          onClick={() => {
            setPage(0);
            setLowStock(lowStock ? undefined : true);
          }}
        >
          {lowStock ? "Mostrando só baixo estoque" : "Só baixo estoque"}
        </Button>
      </div>

      <Card>
        <CardContent className="pt-6">
          {isLoading ? (
            <SkeletonTable rows={5} columns={6} />
          ) : error ? (
            <ErrorCard message={error.message} onRetry={() => refetch()} />
          ) : data && data.content.length > 0 ? (
            <div className="space-y-4">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Nome</TableHead>
                    <TableHead>Categoria</TableHead>
                    <TableHead>Unidade</TableHead>
                    <TableHead className="text-right">Quantidade</TableHead>
                    <TableHead className="text-right">Mínimo</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead className="text-right">Ações</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data.content.map((item) => (
                    <TableRow key={item.id}>
                      <TableCell className="font-medium">{item.name}</TableCell>
                      <TableCell>{categoryLabel(item.category)}</TableCell>
                      <TableCell>{item.unit ? UNIT_LABELS[item.unit] : "—"}</TableCell>
                      <TableCell className="text-right" data-testid="inventory-quantity">
                        {item.quantity}
                      </TableCell>
                      <TableCell className="text-right">{item.minStock}</TableCell>
                      <TableCell>
                        {item.active ? <Badge>Ativo</Badge> : <Badge variant="secondary">Inativo</Badge>}
                        <LowStockBadge item={item} />
                      </TableCell>
                      <TableCell className="text-right">
                        {canManage ? (
                          <Button variant="outline" size="sm" onClick={() => setMovementItem(item)}>
                            Movimentar
                          </Button>
                        ) : null}
                        <Button variant="ghost" size="sm" onClick={() => setHistoryItem(item)}>
                          Histórico
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
              <div className="flex items-center justify-between">
                <span className="text-sm text-muted-foreground">
                  Página {page + 1} de {Math.max(totalPages, 1)}
                </span>
                <div className="flex gap-2">
                  <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage(page - 1)}>
                    Anterior
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={page >= totalPages - 1}
                    onClick={() => setPage(page + 1)}
                  >
                    Próxima
                  </Button>
                </div>
              </div>
            </div>
          ) : (
            <EmptyState title="Nenhum produto em estoque" description="Cadastre produtos no Catálogo." />
          )}
        </CardContent>
      </Card>

      {movementItem ? (
        <StockMovementDialog
          open={!!movementItem}
          onOpenChange={(open) => !open && setMovementItem(null)}
          item={movementItem}
          isSubmitting={registerMovement.isPending}
          onSubmit={(request) =>
            registerMovement.mutate(
              { itemId: movementItem.id, request },
              { onSuccess: () => setMovementItem(null) },
            )
          }
        />
      ) : null}

      {historyItem && companyId ? (
        <MovementHistorySheet
          open={!!historyItem}
          onOpenChange={(open) => !open && setHistoryItem(null)}
          companyId={companyId}
          item={historyItem}
        />
      ) : null}
    </div>
  );
}