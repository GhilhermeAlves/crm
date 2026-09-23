"use client";

import * as React from "react";
import { MoreHorizontal } from "lucide-react";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { TableCell } from "@/components/ui/table";
import type { TableAction } from "../data-table";

type DataTableRowActionsProps<T> = {
  row: T;
  actions: TableAction<T>[];
};

export function DataTableRowActions<T>({ row, actions }: DataTableRowActionsProps<T>) {
  const visibleActions = actions.filter((a) =>
    typeof a.hidden === "function" ? !a.hidden(row) : !a.hidden,
  );

  return (
    <TableCell className="p-3 text-right" onClick={(e) => e.stopPropagation()}>
      {visibleActions.length > 0 && (
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button
              variant="ghost"
              size="icon"
              className="h-8 w-8 text-muted-foreground hover:text-foreground"
              aria-label="Abrir menu de ações"
            >
              <MoreHorizontal className="h-4 w-4" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-44">
            {visibleActions.map((act, aIdx) => {
              const isDisabled =
                typeof act.disabled === "function" ? act.disabled(row) : Boolean(act.disabled);
              const Icon = act.icon;

              return (
                <React.Fragment key={`${act.label}-${aIdx}`}>
                  {act.destructive && aIdx > 0 && <DropdownMenuSeparator />}
                  <DropdownMenuItem
                    onClick={() => act.onClick(row)}
                    disabled={isDisabled}
                    className={cn(act.destructive && "text-destructive focus:text-destructive")}
                  >
                    {Icon && <Icon className="mr-2 h-4 w-4" />}
                    <span>{act.label}</span>
                  </DropdownMenuItem>
                </React.Fragment>
              );
            })}
          </DropdownMenuContent>
        </DropdownMenu>
      )}
    </TableCell>
  );
}
