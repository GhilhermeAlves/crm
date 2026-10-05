"use client";

import { useMemo, useState } from "react";
import { Pencil, Plus, Search, Trash2 } from "lucide-react";

import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import {
  useCatalog,
  useCreateCatalogItem,
  useUpdateCatalogItem,
  useSetCatalogItemActive,
} from "@/features/masterdata/catalog/hooks/useCatalog";
import { CatalogItemDialog } from "@/features/masterdata/catalog/components/CatalogItemDialog";
import {
  formatPrice,
  type CatalogItem,
  type CatalogItemRequest,
} from "@/features/masterdata/catalog/types/catalog.types";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";

export default function ServicesPage() {
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;
  const { data, isLoading } = useCatalog(companyId, { type: "SERVICE" });
  const createItem = useCreateCatalogItem(companyId);
  const updateItem = useUpdateCatalogItem(companyId);
  const setActive = useSetCatalogItemActive(companyId);

  const [search, setSearch] = useState("");
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<CatalogItem | null>(null);

  const items = data?.content ?? [];

  const filtered = useMemo(() => {
    if (!search.trim()) return items;
    const q = search.toLowerCase();
    return items.filter((i) => i.name.toLowerCase().includes(q));
  }, [items, search]);

  const handleSubmit = (req: CatalogItemRequest) => {
    const payload = { ...req, type: "SERVICE" as const };
    if (editingItem) {
      updateItem.mutate(
        { id: editingItem.id, data: payload },
        {
          onSuccess: () => {
            setDialogOpen(false);
            setEditingItem(null);
          },
        },
      );
    } else {
      createItem.mutate(payload, {
        onSuccess: () => {
          setDialogOpen(false);
        },
      });
    }
  };

  const handleEdit = (item: CatalogItem) => {
    setEditingItem(item);
    setDialogOpen(true);
  };

  const handleDelete = (item: CatalogItem) => {
    setActive.mutate({ id: item.id, active: false });
  };

  const openCreate = () => {
    setEditingItem(null);
    setDialogOpen(true);
  };

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <h2 className="text-xl font-semibold">Lista de serviços</h2>

      <div className="flex items-center justify-between gap-4">
        <div className="relative w-64">
          <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="Procurar serviço"
            className="pl-9"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        <Button size="sm" onClick={openCreate}>
          <Plus className="mr-1.5 h-3.5 w-3.5" />
          Adicionar serviço
        </Button>
      </div>

      <section className="rounded-lg border bg-card">
        {isLoading ? (
          <p className="py-8 text-center text-sm text-muted-foreground">
            Carregando...
          </p>
        ) : filtered.length === 0 ? (
          <p className="py-8 text-center text-sm text-muted-foreground">
            Nenhum serviço encontrado.
          </p>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="text-xs font-semibold text-primary">
                  Serviço
                </TableHead>
                <TableHead className="text-xs">Categoria</TableHead>
                <TableHead className="text-right text-xs">Preço</TableHead>
                <TableHead className="text-right text-xs">Ações</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {filtered.map((item) => (
                <TableRow key={item.id}>
                  <TableCell className="text-sm text-primary">
                    {item.name}
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {item.category || "—"}
                  </TableCell>
                  <TableCell className="text-right text-sm">
                    {formatPrice(item)}
                  </TableCell>
                  <TableCell className="text-right">
                    <div className="inline-flex gap-1">
                      <Button
                        variant="ghost"
                        size="icon"
                        className="h-7 w-7"
                        onClick={() => handleEdit(item)}
                      >
                        <Pencil className="h-3.5 w-3.5" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon"
                        className="h-7 w-7 text-destructive hover:text-destructive"
                        onClick={() => handleDelete(item)}
                      >
                        <Trash2 className="h-3.5 w-3.5" />
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </section>

      <CatalogItemDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        item={editingItem}
        isSubmitting={createItem.isPending || updateItem.isPending}
        onSubmit={handleSubmit}
      />
    </div>
  );
}
