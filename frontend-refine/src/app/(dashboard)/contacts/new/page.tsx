"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { Camera, ChevronRight, Loader2, Search } from "lucide-react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import {
  useCreateContact,
  useSearchContacts,
} from "@/features/masterdata/contacts/hooks/useContacts";
import { PatientForm } from "@/features/clinical/patients/components/PatientForm";
import {
  contactToFormValues,
  formValuesToContactPayload,
  type PatientFormValues,
} from "@/features/clinical/patients/schemas/patient.schema";
import { formatCpf } from "@/lib/format";
import type { Contact } from "@/features/masterdata/contacts/types/contact.types";
import { ROUTES } from "@/lib/constants";

const MAX_PHOTO_SIZE = 4 * 1024 * 1024;

function ContactSearchDialog({
  open,
  onOpenChange,
  companyId,
  onSelect,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  companyId: string | null;
  onSelect: (contact: Contact) => void;
}) {
  const [query, setQuery] = useState("");
  const { data: results, isLoading } = useSearchContacts(companyId, query);

  useEffect(() => {
    if (!open) setQuery("");
  }, [open]);

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle>Buscar contato existente</DialogTitle>
        </DialogHeader>
        <div className="relative">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            placeholder="Digite nome ou CPF..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            className="pl-9"
            autoFocus
          />
        </div>
        <div className="max-h-60 overflow-y-auto">
          {isLoading && (
            <div className="flex items-center justify-center py-4">
              <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
            </div>
          )}
          {results && results.length === 0 && query.length >= 2 && (
            <p className="py-4 text-center text-sm text-muted-foreground">
              Nenhum contato encontrado
            </p>
          )}
          {results?.map((c) => (
            <button
              key={c.id}
              type="button"
              className="flex w-full items-center gap-3 rounded-md px-3 py-2 text-left text-sm hover:bg-accent"
              onClick={() => {
                onSelect(c);
                onOpenChange(false);
              }}
            >
              <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary/10 text-xs font-medium text-primary">
                {(c.firstName?.[0] ?? "").toUpperCase()}
              </div>
              <div className="min-w-0 flex-1">
                <p className="truncate font-medium">
                  {c.firstName} {c.lastName}
                </p>
                <p className="truncate text-xs text-muted-foreground">
                  {c.cpf ? formatCpf(c.cpf) : ""}
                  {c.email ? ` · ${c.email}` : ""}
                </p>
              </div>
            </button>
          ))}
        </div>
      </DialogContent>
    </Dialog>
  );
}

export default function NewContactPage() {
  const router = useRouter();
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;
  const createContact = useCreateContact(companyId);

  const [avatarPreview, setAvatarPreview] = useState<string | null>(null);
  const [photoError, setPhotoError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [searchOpen, setSearchOpen] = useState(false);
  const [preset, setPreset] = useState<Partial<PatientFormValues>>({});
  const [formKey, setFormKey] = useState(0);

  const handleAvatarChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (file.size > MAX_PHOTO_SIZE) {
      setPhotoError("A foto deve ter no máximo 4MB");
      return;
    }
    setPhotoError(null);
    const reader = new FileReader();
    reader.onload = () => setAvatarPreview(reader.result as string);
    reader.readAsDataURL(file);
  };

  const handleSelectExisting = (c: Contact) => {
    setPreset(contactToFormValues(c));
    setFormKey((k) => k + 1);
  };

  const handleSubmit = (values: PatientFormValues) => {
    createContact.mutate(formValuesToContactPayload(values), {
      onSuccess: (created) => router.push(`${ROUTES.CONTACTS}/${created.id}`),
    });
  };

  return (
    <div className="space-y-6">
      <nav className="flex items-center gap-1 text-sm text-muted-foreground">
        <Link href={ROUTES.CONTACTS} className="hover:text-foreground">
          Contatos
        </Link>
        <ChevronRight className="h-4 w-4" />
        <span className="text-foreground">Novo cadastro</span>
      </nav>

      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold tracking-tight">Novo Contato</h1>
        <Button
          type="button"
          variant="outline"
          size="sm"
          className="gap-1.5"
          onClick={() => setSearchOpen(true)}
        >
          <Search className="h-4 w-4" />
          Buscar existente
        </Button>
      </div>

      <ContactSearchDialog
        open={searchOpen}
        onOpenChange={setSearchOpen}
        companyId={companyId}
        onSelect={handleSelectExisting}
      />

      {/* Foto */}
      <section className="flex items-center gap-6">
        <button
          type="button"
          onClick={() => fileInputRef.current?.click()}
          className="relative flex h-24 w-24 shrink-0 items-center justify-center overflow-hidden rounded-full border-2 border-dashed border-muted-foreground/30 bg-muted/50 transition-colors hover:border-primary/50"
        >
          {avatarPreview ? (
            <img src={avatarPreview} alt="Foto do contato" className="h-full w-full object-cover" />
          ) : (
            <Camera className="h-8 w-8 text-muted-foreground/50" />
          )}
        </button>
        <input
          ref={fileInputRef}
          type="file"
          accept="image/*"
          className="hidden"
          onChange={handleAvatarChange}
        />
        <div className="text-sm text-muted-foreground">
          <p className="font-medium text-foreground">Foto do contato</p>
          <p>Clique para adicionar uma foto (máx. 4MB)</p>
          {photoError && <p className="text-sm text-destructive">{photoError}</p>}
        </div>
      </section>

      <PatientForm
        key={formKey}
        defaultValues={preset}
        onSubmit={handleSubmit}
        isLoading={createContact.isPending}
        submitLabel="Salvar"
        onCancel={() => router.push(ROUTES.CONTACTS)}
      />
    </div>
  );
}
