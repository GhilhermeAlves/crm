"use client";

import { useState, useMemo } from "react";
import { useRouter } from "next/navigation";
import { Plus, Search, SearchX, Users } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { useAuthorization } from "@/features/identity/auth/hooks/useAuthorization";
import { ContactTable } from "@/features/masterdata/contacts/components/ContactTable";
import { CreateContactDialog } from "@/features/masterdata/contacts/components/CreateContactDialog";
import {
  useContacts,
  useUpdateContact,
  useDeleteContact,
  useContactPermissions,
} from "@/features/masterdata/contacts/hooks/useContacts";
import { EmptyState } from "@/components/common/EmptyState";
import { ErrorCard } from "@/components/common/ErrorCard";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { Card, CardContent } from "@/components/ui/card";
import type { Contact } from "@/features/masterdata/contacts/types/contact.types";

export default function ContactsPage() {
  const { user } = useAuth();
  const { can } = useAuthorization();
  const router = useRouter();
  const companyId = user?.companyId ?? null;

  const { data: contacts, isLoading, isError, refetch } = useContacts(companyId);
  const updateContact = useUpdateContact(companyId);
  const deleteContact = useDeleteContact(companyId);
  const { canCreate, canUpdate, canDelete } = useContactPermissions();

  const [search, setSearch] = useState("");
  const [editingContact, setEditingContact] = useState<Contact | null>(null);
  const [deletingContact, setDeletingContact] = useState<Contact | null>(null);

  const filteredContacts = useMemo(() => {
    const q = search.trim().toLowerCase();
    return (contacts ?? []).filter((c) => {
      if (!q) return true;
      const fullName = `${c.firstName} ${c.lastName ?? ""}`.trim().toLowerCase();
      return (
        fullName.includes(q) ||
        (c.email ?? "").toLowerCase().includes(q) ||
        (c.phone ?? "").toLowerCase().includes(q) ||
        (c.notes ?? "").toLowerCase().includes(q)
      );
    });
  }, [contacts, search]);

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

  const totalCount = contacts?.length ?? 0;

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
      <h1 className="text-2xl font-semibold">
        Lista de Contatos ({totalCount})
      </h1>

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex flex-1 items-center gap-2">
          <div className="relative flex-1 max-w-lg">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Pesquise pelo nome, e-mail ou telefone"
              className="pl-9"
            />
          </div>
          <Button variant="outline">
            Buscar
          </Button>
        </div>
        {canCreate && (
          <Button onClick={() => router.push("/contacts/new")}>
            <Plus className="mr-2 h-4 w-4" /> Novo contato
          </Button>
        )}
      </div>

      <div className="border-t pt-4">
        {isLoading ? (
          <ContactTable contacts={[]} isLoading />
        ) : isError ? (
          <ErrorCard
            message="Não foi possível carregar os contatos."
            onRetry={() => refetch()}
          />
        ) : search.trim() && filteredContacts.length === 0 ? (
          <EmptyState
            icon={<SearchX className="h-8 w-8" />}
            title="Nenhum resultado"
            description="Não encontramos contatos para a pesquisa aplicada."
            action={
              <Button variant="outline" size="sm" onClick={() => setSearch("")}>
                Limpar busca
              </Button>
            }
          />
        ) : (
          <ContactTable
            contacts={filteredContacts}
            onEdit={canUpdate ? setEditingContact : undefined}
            onDelete={canDelete ? setDeletingContact : undefined}
          />
        )}
      </div>

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
