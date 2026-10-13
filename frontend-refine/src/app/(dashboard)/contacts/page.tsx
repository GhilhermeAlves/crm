"use client";

import { useState, useMemo } from "react";
import { useRouter } from "next/navigation";
import { Plus, Search, SearchX, Users } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { useAuthorization } from "@/features/identity/auth/hooks/useAuthorization";
import { ContactTable } from "@/features/masterdata/contacts/components/ContactTable";
import {
  useContacts,
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
  const deleteContact = useDeleteContact(companyId);
  const { canCreate, canUpdate, canDelete } = useContactPermissions();

  const [search, setSearch] = useState("");
  const [deletingContact, setDeletingContact] = useState<Contact | null>(null);

  const filteredContacts = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return contacts ?? [];
    const digits = q.replace(/\D/g, "");
    return (contacts ?? []).filter((c) => {
      const fullName = `${c.firstName} ${c.lastName ?? ""}`.trim().toLowerCase();
      const matchesText =
        fullName.includes(q) ||
        (c.email ?? "").toLowerCase().includes(q) ||
        (c.recordNumber ?? "").toLowerCase().includes(q);
      if (matchesText) return true;
      if (!digits) return false;
      const onlyDigits = (value: string | null | undefined) =>
        (value ?? "").replace(/\D/g, "").includes(digits);
      return (
        onlyDigits(c.cpf) ||
        onlyDigits(c.mobile) ||
        onlyDigits(c.phone) ||
        onlyDigits(c.recordNumber)
      );
    });
  }, [contacts, search]);

  const handleDelete = () => {
    if (!deletingContact) return;
    deleteContact.mutate(deletingContact.id, {
      onSuccess: () => setDeletingContact(null),
    });
  };

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
      <h1 className="text-2xl font-bold tracking-tight">Contatos</h1>

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex flex-1 items-center gap-2">
          <div className="relative max-w-lg flex-1">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Pesquise por nome, CPF, celular ou prontuário"
              className="pl-9"
            />
          </div>
          <Button variant="outline">Buscar</Button>
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
          <ErrorCard message="Não foi possível carregar os contatos." onRetry={() => refetch()} />
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
            onEdit={canUpdate ? (c) => router.push(`/contacts/${c.id}/edit`) : undefined}
            onDelete={canDelete ? setDeletingContact : undefined}
          />
        )}
      </div>

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
