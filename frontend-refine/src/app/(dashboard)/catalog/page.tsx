"use client";

import { useState } from "react";
import { Package, Plus } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
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
import { useAuth } from "@/features/auth/hooks/useAuth";
import { useAuthorization } from "@/features/auth/hooks/useAuthorization";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import { CatalogItemDialog } from "@/features/catalog/components/CatalogItemDialog";
import {
  useCatalog,
  useCreateCatalogItem,
  useSetCatalogItemActive,
  useUpdateCatalogItem,
} from "@/features/catalog/hooks/useCatalog";
import {
  CATALOG_TYPE_LABELS,
  formatPrice,
  type CatalogItem,
  type CatalogItemType,
} from "@/features/catalog/types/catalog.types";

const ALL = "ALL";
const PAGE_SIZE = 20;

export default function CatalogPage() {
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;
  const { can } = useAuthorization();
  const canManage = can("catalog:manage");

  const [search, setSearch] = useState("");
  const [type, setType] = useState<CatalogItemType | typeof ALL>(ALL);
  const [status, setStatus] = useState<"active" | "inactive" | typeof ALL>(ALL);
  const [page, setPage] = useState(0);
  const q = useDebouncedValue(search, 300);

  const { data, isLoading, error, refetch } = useCatalog(companyId, {
    q,
    type: type === ALL ? undefined : type,
    active: status === ALL ? undefined : status === "active",
    page,
    pageSize: PAGE_SIZE,
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
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Item</TableHead>
                    <TableHead>Tipo</TableHead>
                    <TableHead>Categoria</TableHead>
                    <TableHead className="text-right">Preço</TableHead>
                    <TableHead>Situação</TableHead>
                    {canManage && <TableHead className="text-right">Ações</TableHead>}
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {items.map((item) => (
                    <TableRow key={item.id}>
                      <TableCell>
                        <p className="font-medium">{item.name}</p>
                        {item.sku && (
                          <p className="text-xs text-muted-foreground">SKU {item.sku}</p>
                        )}
                      </TableCell>
                      <TableCell>{CATALOG_TYPE_LABELS[item.type]}</TableCell>
                      <TableCell className="text-muted-foreground">
                        {item.category ?? "—"}
                      </TableCell>
                      <TableCell className="text-right tabular-nums">{formatPrice(item)}</TableCell>
                      <TableCell>
                        <Badge variant={item.active ? "default" : "secondary"}>
                          {item.active ? "Ativo" : "Inativo"}
                        </Badge>
                      </TableCell>
                      {canManage && (
                        <TableCell className="text-right">
                          <div className="flex justify-end gap-2">
                            <Button
                              variant="outline"
                              size="sm"
                              disabled={setActive.isPending}
                              onClick={() =>
                                setActive.mutate({ id: item.id, active: !item.active })
                              }
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
                        </TableCell>
                      )}
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
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

      {totalPages > 1 && (
        <div className="flex items-center justify-end gap-2 text-sm">
          <span className="text-muted-foreground">
            Página {page + 1} de {totalPages}
          </span>
          <Button
            variant="outline"
            size="sm"
            disabled={page === 0}
            onClick={() => setPage(page - 1)}
          >
            Anterior
          </Button>
          <Button
            variant="outline"
            size="sm"
            disabled={page + 1 >= totalPages}
            onClick={() => setPage(page + 1)}
          >
            Próxima
          </Button>
        </div>
      )}

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
