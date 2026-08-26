"use client";

import { useState } from "react";
import { Loader2, Plus, ShieldOff } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { useAuthorization } from "@/features/auth/hooks/useAuthorization";
import { ContactTable } from "@/features/contacts/components/ContactTable";
import { CreateContactDialog } from "@/features/contacts/components/CreateContactDialog";
import {
  useContacts,
  useCreateContact,
  useContactPermissions,
} from "@/features/contacts/hooks/useContacts";

export default function ContactsPage() {
  const { user } = useAuth();
  const { can } = useAuthorization();
  const companyId = user?.companyId ?? null;
  const { data: contacts, isLoading } = useContacts(companyId);
  const createContact = useCreateContact(companyId);
  const { canCreate } = useContactPermissions();

  const [open, setOpen] = useState(false);

  if (!can("contact:page:view")) {
    return (
      <Card>
        <CardContent className="flex flex-col items-center py-12 text-muted-foreground">
          <ShieldOff className="mb-4 h-10 w-10 opacity-50" />
          <p>Você não tem permissão para acessar a página de Contatos.</p>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Contatos</h1>
          <p className="text-sm text-muted-foreground">Diretório de clientes da sua empresa.</p>
        </div>
        {canCreate && (
          <Button onClick={() => setOpen(true)}>
            <Plus className="mr-1 h-4 w-4" /> Novo contato
          </Button>
        )}
      </div>

      <Card>
        <CardHeader className="pb-4">
          <CardTitle className="text-base font-semibold">
            {isLoading ? (
              <span className="flex items-center gap-2">
                <Loader2 className="h-4 w-4 animate-spin" /> Carregando…
              </span>
            ) : (
              `${contacts?.length ?? 0} contato(s)`
            )}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <ContactTable contacts={contacts ?? []} isLoading={isLoading} />
        </CardContent>
      </Card>

      <CreateContactDialog
        open={open}
        onOpenChange={setOpen}
        isLoading={createContact.isPending}
        onSubmit={(values) => createContact.mutate(values)}
      />
    </div>
  );
}
