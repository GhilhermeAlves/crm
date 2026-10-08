"use client";

import { useState, type FormEvent } from "react";
import { Check, Pencil, Plus, Trash2, UserX, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import {
  useAddIgnoredContact,
  useIgnoredContacts,
  useRemoveIgnoredContact,
  useUpdateIgnoredContact,
} from "../hooks/useOmnichannel";
import type { IgnoredContact } from "../types/omnichannel.types";

/** 5534999998888 → +55 (34) 99999-8888; outros formatos ficam como vieram. */
function formatPhone(digits: string): string {
  const m = /^55(\d{2})(\d{4,5})(\d{4})$/.exec(digits);
  return m ? `+55 (${m[1]}) ${m[2]}-${m[3]}` : `+${digits}`;
}

/**
 * Números que a IA nunca responde e cujas mensagens não são gravadas no CRM —
 * para quando o WhatsApp do atendimento é também o celular pessoal.
 */
export function IgnoredContactsCard({ canEdit }: { canEdit: boolean }) {
  const { data: contacts = [], isLoading } = useIgnoredContacts();
  const addContact = useAddIgnoredContact();
  const removeContact = useRemoveIgnoredContact();
  const [phone, setPhone] = useState("");
  const [label, setLabel] = useState("");
  const updateContact = useUpdateIgnoredContact();
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editPhone, setEditPhone] = useState("");
  const [editLabel, setEditLabel] = useState("");

  const startEdit = (c: IgnoredContact) => {
    setEditingId(c.id);
    setEditPhone(formatPhone(c.phone));
    setEditLabel(c.label ?? "");
  };

  const handleSaveEdit = (e: FormEvent) => {
    e.preventDefault();
    if (!editingId || !editPhone.trim()) return;
    updateContact.mutate(
      { id: editingId, data: { phone: editPhone.trim(), label: editLabel.trim() || undefined } },
      { onSuccess: () => setEditingId(null) },
    );
  };

  const handleAdd = (e: FormEvent) => {
    e.preventDefault();
    if (!phone.trim()) return;
    addContact.mutate(
      { phone: phone.trim(), label: label.trim() || undefined },
      {
        onSuccess: () => {
          setPhone("");
          setLabel("");
        },
      },
    );
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2 text-base">
          <UserX className="h-4 w-4" />
          Contatos ignorados
        </CardTitle>
        <p className="text-sm text-muted-foreground">
          A IA não responde estes números e as conversas deles não são gravadas no CRM. Use para
          família e amigos quando o WhatsApp do atendimento for também o número pessoal.
        </p>
      </CardHeader>
      <CardContent className="space-y-4">
        {canEdit && (
          <form onSubmit={handleAdd} className="flex flex-col gap-2 sm:flex-row">
            <Input
              placeholder="Telefone com DDD, ex.: (34) 99999-8888"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              aria-label="Telefone"
              inputMode="tel"
            />
            <Input
              placeholder="Identificação (opcional), ex.: Mãe"
              value={label}
              onChange={(e) => setLabel(e.target.value)}
              aria-label="Identificação"
            />
            <Button type="submit" disabled={addContact.isPending || !phone.trim()}>
              <Plus className="mr-2 h-4 w-4" />
              Adicionar
            </Button>
          </form>
        )}

        {isLoading ? (
          <p className="text-sm text-muted-foreground">Carregando…</p>
        ) : contacts.length === 0 ? (
          <p className="text-sm text-muted-foreground">Nenhum contato ignorado.</p>
        ) : (
          <ul className="divide-y rounded-md border">
            {contacts.map((c) =>
              editingId === c.id ? (
                <li key={c.id} className="px-3 py-2">
                  <form onSubmit={handleSaveEdit} className="flex flex-col gap-2 sm:flex-row">
                    <Input
                      value={editPhone}
                      onChange={(e) => setEditPhone(e.target.value)}
                      aria-label="Telefone"
                      inputMode="tel"
                      autoFocus
                    />
                    <Input
                      placeholder="Identificação (opcional)"
                      value={editLabel}
                      onChange={(e) => setEditLabel(e.target.value)}
                      aria-label="Identificação"
                    />
                    <div className="flex gap-1">
                      <Button
                        type="submit"
                        size="icon"
                        aria-label="Salvar"
                        disabled={updateContact.isPending || !editPhone.trim()}
                      >
                        <Check className="h-4 w-4" />
                      </Button>
                      <Button
                        type="button"
                        variant="ghost"
                        size="icon"
                        aria-label="Cancelar edição"
                        onClick={() => setEditingId(null)}
                      >
                        <X className="h-4 w-4" />
                      </Button>
                    </div>
                  </form>
                </li>
              ) : (
                <li key={c.id} className="flex items-center justify-between gap-3 px-3 py-2">
                  <div className="min-w-0">
                    <p className="text-sm font-medium">{formatPhone(c.phone)}</p>
                    {c.label && <p className="truncate text-xs text-muted-foreground">{c.label}</p>}
                  </div>
                  {canEdit && (
                    <div className="flex shrink-0 gap-1">
                      <Button
                        variant="ghost"
                        size="icon"
                        aria-label={`Editar ${formatPhone(c.phone)}`}
                        onClick={() => startEdit(c)}
                      >
                        <Pencil className="h-4 w-4" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon"
                        aria-label={`Remover ${formatPhone(c.phone)}`}
                        disabled={removeContact.isPending}
                        onClick={() => removeContact.mutate(c.id)}
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </div>
                  )}
                </li>
              ),
            )}
          </ul>
        )}
      </CardContent>
    </Card>
  );
}
