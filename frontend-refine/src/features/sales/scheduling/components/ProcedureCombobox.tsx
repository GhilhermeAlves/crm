"use client";

import { useMemo, useState } from "react";
import { Check, ChevronsUpDown, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Command,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
} from "@/components/ui/command";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { cn } from "@/lib/utils";

export interface ProcedureOption {
  id: string;
  name: string;
}

interface Props {
  procedures: ProcedureOption[];
  value: string[];
  onChange: (ids: string[]) => void;
}

/** Busca sem diferenciar maiúsculas nem acentos ("restauracao" acha "Restauração"). */
function normalize(text: string) {
  return text.normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase();
}

/** Seleção múltipla de procedimentos com busca: a lista fica fechada até o usuário abrir. */
export function ProcedureCombobox({ procedures, value, onChange }: Props) {
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState("");

  const filtered = useMemo(() => {
    const q = normalize(search.trim());
    const list = q ? procedures.filter((p) => normalize(p.name).includes(q)) : procedures;
    return [...list].sort((a, b) => a.name.localeCompare(b.name, "pt-BR"));
  }, [procedures, search]);

  const selected = value
    .map((id) => procedures.find((p) => p.id === id))
    .filter((p): p is ProcedureOption => !!p);

  const toggle = (id: string) =>
    onChange(value.includes(id) ? value.filter((v) => v !== id) : [...value, id]);

  return (
    <div className="space-y-2">
      <Popover
        open={open}
        onOpenChange={(next) => {
          setOpen(next);
          if (!next) setSearch("");
        }}
      >
        <PopoverTrigger asChild>
          <Button
            type="button"
            variant="outline"
            role="combobox"
            aria-expanded={open}
            aria-label="Selecionar procedimentos"
            className="w-full justify-between font-normal"
          >
            <span className={cn("truncate", selected.length === 0 && "text-muted-foreground")}>
              {selected.length === 0
                ? "Buscar procedimento…"
                : `${selected.length} procedimento${selected.length > 1 ? "s" : ""} selecionado${
                    selected.length > 1 ? "s" : ""
                  }`}
            </span>
            <ChevronsUpDown className="ml-2 h-4 w-4 shrink-0 opacity-50" />
          </Button>
        </PopoverTrigger>
        <PopoverContent className="w-[--radix-popover-trigger-width] p-0" align="start">
          <Command shouldFilter={false}>
            <CommandInput
              placeholder="Digite o nome do procedimento…"
              value={search}
              onValueChange={setSearch}
            />
            <CommandList>
              <CommandEmpty>Nenhum procedimento encontrado.</CommandEmpty>
              <CommandGroup>
                {filtered.map((p) => {
                  const isSelected = value.includes(p.id);
                  return (
                    <CommandItem key={p.id} value={p.id} onSelect={() => toggle(p.id)}>
                      <Check
                        className={cn("mr-2 h-4 w-4", isSelected ? "opacity-100" : "opacity-0")}
                      />
                      <span className="text-sm">{p.name}</span>
                    </CommandItem>
                  );
                })}
              </CommandGroup>
            </CommandList>
          </Command>
        </PopoverContent>
      </Popover>

      {selected.length > 0 && (
        <div className="flex flex-wrap gap-1.5" aria-label="Procedimentos selecionados">
          {selected.map((p) => (
            <span
              key={p.id}
              className="inline-flex items-center gap-1 rounded-full border border-primary bg-primary/10 py-0.5 pl-2.5 pr-1 text-xs font-medium"
            >
              {p.name}
              <button
                type="button"
                aria-label={`Remover ${p.name}`}
                onClick={() => toggle(p.id)}
                className="rounded-full p-0.5 hover:bg-primary/20"
              >
                <X className="h-3 w-3" />
              </button>
            </span>
          ))}
        </div>
      )}
    </div>
  );
}
