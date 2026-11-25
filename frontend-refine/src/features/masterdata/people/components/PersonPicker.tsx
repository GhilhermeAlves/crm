"use client";

import { useEffect, useState } from "react";
import { Loader2, Search } from "lucide-react";
import { Input } from "@/components/ui/input";
import { usePeopleSearch } from "../hooks/usePeople";
import { formatTaxId } from "../lib/people-labels";
import type { PersonSummary } from "../types/people.types";

/** Busca uma pessoa já cadastrada (responsável, cônjuge, unificação...). */
export function PersonPicker({
  id,
  excludeId,
  onSelect,
  selected,
}: {
  id: string;
  excludeId?: string;
  onSelect: (p: PersonSummary | null) => void;
  selected: PersonSummary | null;
}) {
  const [draft, setDraft] = useState("");
  const [q, setQ] = useState("");
  useEffect(() => {
    const t = setTimeout(() => setQ(draft.trim()), 300);
    return () => clearTimeout(t);
  }, [draft]);
  const { data, isFetching } = usePeopleSearch({ q: q.length >= 2 ? q : "__none__", size: 8 });
  const items = q.length >= 2 ? (data?.items ?? []).filter((p) => p.id !== excludeId) : [];

  if (selected) {
    return (
      <div className="flex items-center justify-between rounded-md border p-3 text-sm">
        <span>
          <strong>{selected.name}</strong>
          {selected.taxId ? ` · ${formatTaxId(selected.taxId)}` : ""}
        </span>
        <button type="button" className="text-primary underline" onClick={() => onSelect(null)}>
          Trocar
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-2">
      <div className="relative">
        <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
        <Input
          id={id}
          className="pl-9"
          value={draft}
          placeholder="Digite nome, CPF ou celular"
          onChange={(e) => setDraft(e.target.value)}
          autoComplete="off"
        />
        {isFetching && (
          <Loader2 className="absolute right-3 top-1/2 h-4 w-4 -translate-y-1/2 animate-spin text-muted-foreground" />
        )}
      </div>
      {items.length > 0 && (
        <ul
          className="max-h-60 overflow-y-auto rounded-md border"
          role="listbox"
          aria-label="Pessoas encontradas"
        >
          {items.map((p) => (
            <li key={p.id}>
              <button
                type="button"
                role="option"
                aria-selected={false}
                className="flex w-full items-center justify-between px-3 py-2 text-left text-sm hover:bg-muted"
                onClick={() => onSelect(p)}
              >
                <span className="font-medium">{p.name}</span>
                <span className="text-xs text-muted-foreground">
                  {p.taxId ? formatTaxId(p.taxId) : (p.mobile ?? "")}
                </span>
              </button>
            </li>
          ))}
        </ul>
      )}
      {q.length >= 2 && !isFetching && items.length === 0 && (
        <p className="text-xs text-muted-foreground">
          Ninguém encontrado. Cadastre a pessoa antes em Pessoas.
        </p>
      )}
    </div>
  );
}
