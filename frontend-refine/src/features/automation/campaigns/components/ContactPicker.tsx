"use client";

import { useMemo, useState } from "react";
import { Checkbox } from "@/components/ui/checkbox";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useContacts } from "@/features/masterdata/contacts/hooks/useContacts";
import type { Contact } from "@/features/masterdata/contacts/types/contact.types";

type ContactPickerProps = {
  companyId: string | null;
  selected: string[];
  onChange: (ids: string[]) => void;
};

const fullName = (c: Contact) => `${c.firstName ?? ""} ${c.lastName ?? ""}`.trim();
const phoneOf = (c: Contact) => c.mobile || c.phone || null;

/** Escolha dos contatos da campanha; quem não tem telefone não recebe WhatsApp. */
export function ContactPicker({ companyId, selected, onChange }: ContactPickerProps) {
  const { data: contacts = [], isLoading } = useContacts(companyId);
  const [query, setQuery] = useState("");

  const visible = useMemo(() => {
    const q = query.trim().toLowerCase();
    const digits = q.replace(/\D/g, "");
    return contacts
      .filter((c) => {
        if (!q) return true;
        if (fullName(c).toLowerCase().includes(q)) return true;
        return digits.length > 0 && (phoneOf(c) ?? "").replace(/\D/g, "").includes(digits);
      })
      .sort((a, b) => fullName(a).localeCompare(fullName(b), "pt-BR"));
  }, [contacts, query]);

  const selectedSet = new Set(selected);
  const selectable = visible.filter((c) => phoneOf(c));

  const toggle = (id: string, checked: boolean) =>
    onChange(checked ? [...selected, id] : selected.filter((s) => s !== id));

  const selectVisible = () =>
    onChange(Array.from(new Set([...selected, ...selectable.map((c) => c.id)])));

  return (
    <div className="space-y-3">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center">
        <Input
          aria-label="Buscar contato"
          placeholder="Buscar por nome ou telefone"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
        <div className="flex gap-2">
          <Button type="button" variant="outline" size="sm" onClick={selectVisible}>
            Selecionar todos
          </Button>
          <Button type="button" variant="ghost" size="sm" onClick={() => onChange([])}>
            Limpar
          </Button>
        </div>
      </div>
      <p className="text-sm text-muted-foreground">{selected.length} contato(s) selecionado(s)</p>
      <div className="max-h-72 overflow-y-auto rounded-md border">
        {isLoading && <p className="p-3 text-sm text-muted-foreground">Carregando contatos...</p>}
        {!isLoading && visible.length === 0 && (
          <p className="p-3 text-sm text-muted-foreground">Nenhum contato encontrado.</p>
        )}
        {visible.map((contact) => {
          const phone = phoneOf(contact);
          const id = `contact-${contact.id}`;
          return (
            <label
              key={contact.id}
              htmlFor={id}
              className="flex items-center gap-3 border-b px-3 py-2 text-sm last:border-b-0"
            >
              <Checkbox
                id={id}
                checked={selectedSet.has(contact.id)}
                disabled={!phone}
                onCheckedChange={(checked) => toggle(contact.id, checked === true)}
              />
              <span className="min-w-0 flex-1 truncate">{fullName(contact) || "Sem nome"}</span>
              <span className="text-xs text-muted-foreground">{phone ?? "sem telefone"}</span>
            </label>
          );
        })}
      </div>
    </div>
  );
}
