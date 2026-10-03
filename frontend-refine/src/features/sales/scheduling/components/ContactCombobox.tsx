"use client";

import { useState, useMemo } from "react";
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
import type { Contact } from "@/features/masterdata/contacts/types/contact.types";

interface Props {
  contacts: Contact[];
  value: string | undefined;
  onChange: (contactId: string | undefined) => void;
  placeholder?: string;
}

export function ContactCombobox({
  contacts,
  value,
  onChange,
  placeholder = "Selecione um contato",
}: Props) {
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState("");

  const filtered = useMemo(() => {
    if (!search) return contacts;
    const q = search.toLowerCase();
    return contacts.filter(
      (c) =>
        `${c.firstName} ${c.lastName}`.toLowerCase().includes(q) ||
        c.email?.toLowerCase().includes(q) ||
        c.phone?.includes(q),
    );
  }, [contacts, search]);

  const selected = contacts.find((c) => c.id === value);
  const displayName = selected ? `${selected.firstName} ${selected.lastName}`.trim() : null;

  return (
    <div className="flex items-center gap-1">
      <Popover open={open} onOpenChange={setOpen}>
        <PopoverTrigger asChild>
          <Button
            variant="outline"
            role="combobox"
            aria-expanded={open}
            className="w-full justify-between font-normal"
          >
            <span className={cn("truncate", !displayName && "text-muted-foreground")}>
              {displayName ?? placeholder}
            </span>
            <ChevronsUpDown className="ml-2 h-4 w-4 shrink-0 opacity-50" />
          </Button>
        </PopoverTrigger>
        <PopoverContent className="w-[--radix-popover-trigger-width] p-0" align="start">
          <Command shouldFilter={false}>
            <CommandInput placeholder="Buscar contato…" value={search} onValueChange={setSearch} />
            <CommandList>
              <CommandEmpty>Nenhum contato encontrado.</CommandEmpty>
              <CommandGroup>
                {filtered.map((c) => {
                  const name = `${c.firstName} ${c.lastName}`.trim();
                  return (
                    <CommandItem
                      key={c.id}
                      value={c.id}
                      onSelect={() => {
                        onChange(c.id === value ? undefined : c.id);
                        setOpen(false);
                        setSearch("");
                      }}
                    >
                      <Check
                        className={cn("mr-2 h-4 w-4", value === c.id ? "opacity-100" : "opacity-0")}
                      />
                      <div className="flex flex-col">
                        <span className="text-sm">{name}</span>
                        {(c.phone || c.email) && (
                          <span className="text-xs text-muted-foreground">
                            {c.phone ?? c.email}
                          </span>
                        )}
                      </div>
                    </CommandItem>
                  );
                })}
              </CommandGroup>
            </CommandList>
          </Command>
        </PopoverContent>
      </Popover>

      {value && (
        <Button
          type="button"
          variant="ghost"
          size="icon"
          className="h-8 w-8 shrink-0"
          onClick={() => onChange(undefined)}
        >
          <X className="h-3.5 w-3.5" />
        </Button>
      )}
    </div>
  );
}
