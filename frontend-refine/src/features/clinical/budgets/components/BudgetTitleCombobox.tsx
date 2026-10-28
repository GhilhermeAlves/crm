"use client";

import { useState } from "react";
import { Check, ChevronsUpDown, Plus, X } from "lucide-react";
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
import {
  useBudgetTitles,
  useCreateBudgetTitle,
  useDeleteBudgetTitle,
} from "../hooks/useBudgetTitles";

interface Props {
  id: string;
  companyId: string;
  value: string;
  onChange: (value: string) => void;
}

/**
 * Título do orçamento: escolhe um título salvo da empresa ou cria um novo, que
 * fica disponível para os próximos lançamentos.
 */
export function BudgetTitleCombobox({ id, companyId, value, onChange }: Props) {
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState("");
  const { data: titles = [] } = useBudgetTitles(companyId);
  const createTitle = useCreateBudgetTitle(companyId);
  const deleteTitle = useDeleteBudgetTitle(companyId);

  const typed = search.trim();
  const exists = titles.some((t) => t.title.toLowerCase() === typed.toLowerCase());

  const choose = (title: string) => {
    onChange(title);
    setSearch("");
    setOpen(false);
  };

  const create = () => {
    if (!typed) return;
    createTitle.mutate(typed, { onSuccess: (saved) => choose(saved.title) });
  };

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button
          id={id}
          type="button"
          variant="outline"
          role="combobox"
          aria-expanded={open}
          className="w-full justify-between font-normal"
        >
          <span className={cn("truncate", !value && "text-muted-foreground")}>
            {value || "Selecione ou crie um título"}
          </span>
          <ChevronsUpDown className="ml-2 h-4 w-4 shrink-0 opacity-50" />
        </Button>
      </PopoverTrigger>
      <PopoverContent className="w-[--radix-popover-trigger-width] p-0" align="start">
        <Command>
          <CommandInput
            placeholder="Buscar ou digitar novo título…"
            value={search}
            onValueChange={setSearch}
          />
          <CommandList>
            <CommandEmpty>Nenhum título salvo.</CommandEmpty>
            {titles.length > 0 && (
              <CommandGroup heading="Títulos salvos">
                {titles.map((t) => (
                  <CommandItem
                    key={t.id}
                    value={t.title}
                    onSelect={() => choose(t.title)}
                    className="group"
                  >
                    <Check
                      className={cn(
                        "mr-2 h-4 w-4",
                        value === t.title ? "opacity-100" : "opacity-0",
                      )}
                    />
                    <span className="flex-1 truncate">{t.title}</span>
                    <button
                      type="button"
                      aria-label={`Remover título ${t.title}`}
                      className="opacity-0 transition-opacity hover:text-destructive group-hover:opacity-100"
                      onClick={(e) => {
                        e.stopPropagation();
                        deleteTitle.mutate(t.id);
                      }}
                    >
                      <X className="h-3.5 w-3.5" />
                    </button>
                  </CommandItem>
                ))}
              </CommandGroup>
            )}
            {typed && !exists && (
              <CommandGroup>
                <CommandItem
                  value={`__create__${typed}`}
                  onSelect={create}
                  disabled={createTitle.isPending}
                >
                  <Plus className="mr-2 h-4 w-4" />
                  Criar título “{typed}”
                </CommandItem>
                <CommandItem value={`__use__${typed}`} onSelect={() => choose(typed)}>
                  <span className="ml-6 text-muted-foreground">Usar só neste orçamento</span>
                </CommandItem>
              </CommandGroup>
            )}
          </CommandList>
        </Command>
      </PopoverContent>
    </Popover>
  );
}
