"use client";

import { ListPagination } from "@/components/common/ListPagination";
import { DEFAULT_PAGE_SIZE } from "@/types/page";
import { useState } from "react";
import { Package, Plus } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { DataTable } from "@/components/ui/data-table";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { SearchInput } from "@/components/common/SearchInput";
import { EmptyState } from "@/components/common/EmptyState";
import { ErrorCard } from "@/components/common/ErrorCard";
import { SkeletonTable } from "@/components/feedback/SkeletonTable";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { useAuthorization } from "@/features/identity/auth/hooks/useAuthorization";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import { CatalogItemDialog } from "@/features/masterdata/catalog/components/CatalogItemDialog";
import {
  useCatalog,
  useCreateCatalogItem,
  useSetCatalogItemActive,
  useUpdateCatalogItem,
} from "@/features/masterdata/catalog/hooks/useCatalog";
import {
  CATALOG_TYPE_LABELS,
  formatPrice,
  type CatalogItem,
  type CatalogItemType,
} from "@/features/masterdata/catalog/types/catalog.types";

const ALL = "ALL";

export default function CatalogPage() {
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;
  const { can } = useAuthorization();
  const canManage = can("catalog:manage");

  const [search, setSearch] = useState("");
  const [type, setType] = useState<CatalogItemType | typeof ALL>(ALL);
  const [status, setStatus] = useState<"active" | "inactive" | typeof ALL>(ALL);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);
  const q = useDebouncedValue(search, 300);

  const { data, isLoading, error, refetch } = useCatalog(companyId, {
    q,
    type: type === ALL ? undefined : type,
    active: status === ALL ? undefined : status === "active",
    page,
    pageSize,
  });
  const createItem = useCreateCatalogItem(companyId);
  const updateItem = useUpdateCatalogItem(companyId);
  const setActive = useSetCatalogItemActive(companyId);

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<CatalogItem | null>(null);

  const items = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;

  const openCreate = () => {
    setEditing(null);
    setDialogOpen(true);
  };

  const resetPage =
    <T,>(setter: (v: T) => void) =>
    (value: T) => {
      setter(value);
      setPage(0);
    };

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Catálogo</h1>
          <p className="text-sm text-muted-foreground">
            Produtos e serviços da empresa. Itens ativos podem ser consultados pelo agente de IA no
            WhatsApp.
          </p>
        </div>
        {canManage && (
          <Button onClick={openCreate}>
            <Plus className="mr-1 h-4 w-4" /> Novo item
          </Button>
        )}
      </div>

      <div className="flex flex-col gap-2 sm:flex-row">
        <SearchInput
          placeholder="Buscar por nome, categoria, descrição ou SKU…"
          aria-label="Buscar no catálogo"
          value={search}
          onChange={(e) => resetPage(setSearch)(e.target.value)}
          onClear={() => resetPage(setSearch)("")}
          className="sm:max-w-sm"
        />
        <Select
          value={type}
          onValueChange={(v) => resetPage(setType)(v as CatalogItemType | typeof ALL)}
        >
          <SelectTrigger className="w-full sm:w-40" aria-label="Tipo">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={ALL}>Todos os tipos</SelectItem>
            <SelectItem value="PRODUCT">Produtos</SelectItem>
            <SelectItem value="SERVICE">Serviços</SelectItem>
          </SelectContent>
        </Select>
        <Select
          value={status}
          onValueChange={(v) => resetPage(setStatus)(v as "active" | "inactive" | typeof ALL)}
        >
          <SelectTrigger className="w-full sm:w-40" aria-label="Situação">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={ALL}>Todas as situações</SelectItem>
            <SelectItem value="active">Ativos</SelectItem>
            <SelectItem value="inactive">Inativos</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {isLoading ? (
        <SkeletonTable rows={5} columns={5} />
      ) : error ? (
        <ErrorCard message={error.message} onRetry={() => refetch()} />
      ) : (
        <Card>
          <CardContent className="p-0">
            {items.length > 0 ? (
              <DataTable
                tableId="catalog"
                data={items ?? []}
                bordered={false}
                columns={[
                  {
                    id: "item",
                    header: "Item",
                    cell: (item) => (
                      <>
                        <p className="font-medium">{item.name}</p>
                        {item.sku && (
                          <p className="text-xs text-muted-foreground">SKU {item.sku}</p>
                        )}
                      </>
                    ),
                  },
                  {
                    id: "tipo",
                    header: "Tipo",
                    cell: (item) => <>{CATALOG_TYPE_LABELS[item.type]}</>,
                  },
                  {
                    id: "categoria",
                    header: "Categoria",
                    className: "text-muted-foreground",
                    sortValue: (item) => item.category,
                    cell: (item) => <>{item.category ?? "—"}</>,
                  },
                  {
                    id: "preco",
                    header: "Preço",
                    headerClassName: "text-right",
                    className: "text-right tabular-nums",
                    cell: (item) => <>{formatPrice(item)}</>,
                  },
                  {
                    id: "situacao",
                    header: "Situação",
                    cell: (item) => (
                      <Badge variant={item.active ? "default" : "secondary"}>
                        {item.active ? "Ativo" : "Inativo"}
                      </Badge>
                    ),
                  },
                  {
                    id: "acoes",
                    header: "Ações",
                    hideable: false,
                    headerClassName: "text-right",
                    className: "text-right",
                    cell: (item) => (
                      <div className="flex justify-end gap-2">
                        <Button
                          variant="outline"
                          size="sm"
                          disabled={setActive.isPending}
                          onClick={() => setActive.mutate({ id: item.id, active: !item.active })}
                        >
                          {item.active ? "Desativar" : "Ativar"}
                        </Button>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => {
                            setEditing(item);
                            setDialogOpen(true);
                          }}
                        >
                          Editar
                        </Button>
                      </div>
                    ),
                  },
                ]}
                keyExtractor={(item) => item.id}
              />
            ) : (
              <EmptyState
                icon={<Package className="h-8 w-8" />}
                title={
                  q || type !== ALL || status !== ALL ? "Nenhum item encontrado" : "Catálogo vazio"
                }
                description={
                  q || type !== ALL || status !== ALL
                    ? "Ajuste a busca ou os filtros."
                    : "Cadastre produtos e serviços para a equipe e o agente de IA consultarem."
                }
              />
            )}
          </CardContent>
        </Card>
      )}

      <ListPagination
        page={page}
        pageSize={pageSize}
        totalPages={totalPages}
        totalElements={data?.totalElements ?? 0}
        onPageChange={setPage}
        onPageSizeChange={(size) => {
          setPageSize(size);
          setPage(0);
        }}
      />

      <CatalogItemDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        item={editing}
        isSubmitting={createItem.isPending || updateItem.isPending}
        onSubmit={(request) => {
          const close = { onSuccess: () => setDialogOpen(false) };
          if (editing) updateItem.mutate({ id: editing.id, data: request }, close);
          else createItem.mutate(request, close);
        }}
      />
    </div>
  );
}
