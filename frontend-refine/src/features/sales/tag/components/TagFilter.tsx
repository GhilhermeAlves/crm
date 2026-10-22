"use client";

import { ChevronDown } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import type { Tag } from "../types/tag.types";

type Props = {
  tags: Pick<Tag, "id" | "name" | "color">[];
  /** Vazio = sem filtro (todos os agendamentos, com ou sem marcador). */
  value: Set<string>;
  onChange: (value: Set<string>) => void;
};

/** Filtro da agenda por marcador: mostra agendamentos com qualquer um dos marcados. */
export function TagFilter({ tags, value, onChange }: Props) {
  if (tags.length === 0) return null;

  const toggle = (id: string) => {
    const next = new Set(value);
    if (next.has(id)) next.delete(id);
    else next.add(id);
    onChange(next);
  };

  return (
    <Popover>
      <PopoverTrigger asChild>
        <Button variant="outline" size="sm" className="h-8 gap-1.5">
          Marcadores
          {value.size > 0 && (
            <span className="rounded-full bg-primary px-1.5 text-xs text-primary-foreground">
              {value.size}
            </span>
          )}
          <ChevronDown className="h-3.5 w-3.5" />
        </Button>
      </PopoverTrigger>
      <PopoverContent align="start" className="w-56 space-y-2 p-3">
        <div className="flex items-center gap-2 border-b pb-2">
          <Checkbox
            id="tag-all"
            checked={value.size === 0}
            onCheckedChange={() => onChange(new Set())}
          />
          <Label htmlFor="tag-all" className="text-sm font-medium">
            Todos
          </Label>
        </div>
        {tags.map((tag) => (
          <div key={tag.id} className="flex items-center gap-2">
            <Checkbox
              id={`tag-${tag.id}`}
              checked={value.has(tag.id)}
              onCheckedChange={() => toggle(tag.id)}
            />
            <span
              className="h-2 w-2 shrink-0 rounded-full"
              style={{ backgroundColor: tag.color ?? "currentColor" }}
            />
            <Label htmlFor={`tag-${tag.id}`} className="text-sm">
              {tag.name}
            </Label>
          </div>
        ))}
      </PopoverContent>
    </Popover>
  );
}
