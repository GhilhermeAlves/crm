"use client";

import { useState, useCallback, useMemo } from "react";
import { Plus, SearchX, ChevronDown, ChevronUp, Users, SlidersHorizontal } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { useAuthorization } from "@/features/auth/hooks/useAuthorization";
import { ContactTable } from "@/features/contacts/components/ContactTable";
import { CreateContactDialog } from "@/features/contacts/components/CreateContactDialog";
import {
  useContacts,
  useCreateContact,
  useUpdateContact,
  useDeleteContact,
  useContactPermissions,
} from "@/features/contacts/hooks/useContacts";
import { PageTitle } from "@/components/common/PageTitle";
import { SearchInput } from "@/components/common/SearchInput";
import { EmptyState } from "@/components/common/EmptyState";
import { ErrorCard } from "@/components/common/ErrorCard";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { FilterBar } from "@/components/common/FilterBar";
import { Card, CardContent } from "@/components/ui/card";
import type { Contact } from "@/features/contacts/types/contact.types";

export default function ContactsPage() {
  const { user } = useAuth();
  const { can } = useAuthorization();
  const companyId = user?.companyId ?? null;

  const { data: contacts, isLoading, isError, refetch } = useContacts(companyId);
  const createContact = useCreateContact(companyId);
  const updateContact = useUpdateContact(companyId);
  const deleteContact = useDeleteContact(companyId);
  const { canCreate, canUpdate, canDelete } = useContactPermissions();

  const [search, setSearch] = useState("");
  const [createdFrom, setCreatedFrom] = useState("");
  const [createdTo, setCreatedTo] = useState("");
  const [filtersOpen, setFiltersOpen] = useState(false);
  const [createOpen, setCreateOpen] = useState(false);
  const [editingContact, setEditingContact] = useState<Contact | null>(null);
  const [deletingContact, setDeletingContact] = useState<Contact | null>(null);

  const filteredContacts = useMemo(() => {
    const q = search.trim().toLowerCase();
    const from = createdFrom ? new Date(`${createdFrom}T00:00:00`) : null;
    const to = createdTo ? new Date(`${createdTo}T23:59:59.999`) : null;
    return (contacts ?? []).filter((c) => {
      if (q) {
        const fullName = `${c.firstName} ${c.lastName ?? ""}`.trim().toLowerCase();
        const match =
          fullName.includes(q) ||
          (c.email ?? "").toLowerCase().includes(q) ||
          (c.phone ?? "").toLowerCase().includes(q) ||
          (c.notes ?? "").toLowerCase().includes(q);
        if (!match) return false;
      }
      if (from || to) {
        const createdAt = new Date(c.createdAt);
        if (from && createdAt < from) return false;
        if (to && createdAt > to) return false;
      }
      return true;
    });
  }, [contacts, search, createdFrom, createdTo]);

  const hasActiveFilters = search.trim() !== "" || createdFrom !== "" || createdTo !== "";

  const clearFilters = useCallback(() => {
    setSearch("");
    setCreatedFrom("");
    setCreatedTo("");
  }, []);

  const handleCreate = (values: Parameters<typeof createContact.mutate>[0]) => {
    createContact.mutate(values, { onSuccess: () => setCreateOpen(false) });
  };

  const handleUpdate = (values: Parameters<typeof updateContact.mutate>[0]["data"]) => {
    if (!editingContact) return;
    updateContact.mutate(
      { id: editingContact.id, data: values },
      { onSuccess: () => setEditingContact(null) },
    );
  };

  const handleDelete = () => {
    if (!deletingContact) return;
    deleteContact.mutate(deletingContact.id, {
      onSuccess: () => setDeletingContact(null),
    });
  };

  const listError = isError;

  if (!can("contact:page:view")) {
    return (
      <Card>
        <CardContent className="flex flex-col items-center py-12 text-muted-foreground">
          <Users className="mb-4 h-10 w-10 opacity-50" />
          <p>Você não tem permissão para acessar a página de Contatos.</p>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <PageTitle>Contatos</PageTitle>
          <p className="text-sm text-muted-foreground">
            Gerencie seus contatos, empresas relacionadas e relacionamentos comerciais.
          </p>
        </div>
        {canCreate && (
          <Button onClick={() => setCreateOpen(true)}>
            <Plus className="mr-2 h-4 w-4" /> Novo contato
          </Button>
        )}
      </div>

      <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
        <div className="w-full max-w-sm">
          <SearchInput
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Pesquisar contatos..."
          />
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => setFiltersOpen((open) => !open)}
            className="gap-2"
          >
            {filtersOpen ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
            Filtros
          </Button>
          {hasActiveFilters && (
            <Button variant="ghost" size="sm" onClick={clearFilters} className="gap-1">
              <SearchX className="h-4 w-4" />
              Limpar filtros
            </Button>
          )}
        </div>
      </div>

      {filtersOpen && (
        <FilterBar>
          <div className="flex flex-wrap items-end gap-3">
            <div className="space-y-1">
              <label className="flex items-center gap-1 text-xs text-muted-foreground">
                <SlidersHorizontal className="h-3 w-3" /> Criado a partir de
              </label>
              <Input
                type="date"
                value={createdFrom}
                onChange={(e) => setCreatedFrom(e.target.value)}
              />
            </div>
            <div className="space-y-1">
              <label className="block text-xs text-muted-foreground">Até</label>
              <Input type="date" value={createdTo} onChange={(e) => setCreatedTo(e.target.value)} />
            </div>
          </div>
        </FilterBar>
      )}

      <Card>
        <CardContent className="p-0 sm:p-6 sm:pt-0">
          <div className="border-b p-4 sm:px-0 sm:pt-6">
            <p className="text-sm text-muted-foreground">
              {hasActiveFilters
                ? `${filteredContacts.length} de ${contacts?.length ?? 0} contato(s)`
                : `${contacts?.length ?? 0} contato(s)`}
            </p>
          </div>
          {isLoading ? (
            <div className="p-4">
              <ContactTable contacts={[]} isLoading />
            </div>
          ) : listError ? (
            <div className="p-4">
              <ErrorCard
                message="Não foi possível carregar os contatos."
                onRetry={() => refetch()}
              />
            </div>
          ) : filteredContacts.length === 0 && hasActiveFilters ? (
            <div className="p-4">
              <EmptyState
                icon={<SearchX className="h-8 w-8" />}
                title="Nenhum resultado"
                description="Não encontramos contatos para a pesquisa ou filtros aplicados."
                action={
                  <Button variant="outline" size="sm" onClick={clearFilters}>
                    Limpar filtros
                  </Button>
                }
              />
            </div>
          ) : (
            <div className="p-4">
              <ContactTable
                contacts={filteredContacts}
                onEdit={canUpdate ? setEditingContact : undefined}
                onDelete={canDelete ? setDeletingContact : undefined}
              />
            </div>
          )}
        </CardContent>
      </Card>

      <CreateContactDialog
        open={createOpen}
        onOpenChange={setCreateOpen}
        isLoading={createContact.isPending}
        onSubmit={handleCreate}
      />

      <CreateContactDialog
        open={!!editingContact}
        onOpenChange={(open) => !open && setEditingContact(null)}
        isLoading={updateContact.isPending}
        contact={editingContact}
        onSubmit={handleUpdate}
      />

      <ConfirmDialog
        open={!!deletingContact}
        onOpenChange={(open) => !open && setDeletingContact(null)}
        title="Excluir contato"
        description={`Tem certeza que deseja excluir ${
          deletingContact
            ? `${deletingContact.firstName} ${deletingContact.lastName ?? ""}`.trim()
            : "este contato"
        }? Essa ação não pode ser desfeita.`}
        confirmLabel="Excluir"
        variant="destructive"
        onConfirm={handleDelete}
        isLoading={deleteContact.isPending}
      />
    </div>
  );
}
