"use client";

import { Columns3, RotateCcw } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";

export interface ColumnMenuItem {
  id: string;
  label: string;
  hideable: boolean;
}

interface DataTableColumnsMenuProps {
  columns: ColumnMenuItem[];
  hidden: Set<string>;
  isCustomized: boolean;
  onToggle: (id: string) => void;
  onReset: () => void;
}

/** Botões "Redefinir" (só quando mexeu) e "Colunas", no canto superior direito do grid. */
export function DataTableColumnsMenu({
  columns,
  hidden,
  isCustomized,
  onToggle,
  onReset,
}: DataTableColumnsMenuProps) {
  const visibleCount = columns.filter((c) => !hidden.has(c.id)).length;

  return (
    <div className="flex items-center gap-2">
      {isCustomized && (
        <Button variant="ghost" size="sm" className="h-8 gap-1.5" onClick={onReset}>
          <RotateCcw className="h-3.5 w-3.5" />
          Redefinir
        </Button>
      )}
      <Popover>
        <PopoverTrigger asChild>
          <Button variant="outline" size="sm" className="h-8 gap-1.5">
            <Columns3 className="h-3.5 w-3.5" />
            Colunas
          </Button>
        </PopoverTrigger>
        <PopoverContent align="end" className="w-64 p-2">
          <p className="px-2 pb-2 text-xs font-semibold text-muted-foreground">Mostrar colunas</p>
          <ul className="space-y-0.5">
            {columns.map((col) => {
              const checked = !hidden.has(col.id);
              const lockLast = checked && visibleCount <= 1;
              return (
                <li
                  key={col.id}
                  className="flex items-center gap-2 rounded-md px-2 py-1 hover:bg-muted/60"
                >
                  <Checkbox
                    id={`col-${col.id}`}
                    checked={checked}
                    disabled={!col.hideable || lockLast}
                    onCheckedChange={() => onToggle(col.id)}
                  />
                  <label htmlFor={`col-${col.id}`} className="flex-1 truncate text-sm">
                    {col.label}
                  </label>
                </li>
              );
            })}
          </ul>
        </PopoverContent>
      </Popover>
    </div>
  );
}
