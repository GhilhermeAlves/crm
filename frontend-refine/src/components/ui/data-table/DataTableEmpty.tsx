"use client";

import { Inbox } from "lucide-react";
import { EmptyState } from "@/components/common/EmptyState";
import { TableCell, TableRow } from "@/components/ui/table";

type DataTableEmptyProps = {
  totalColumns: number;
  emptyState?: {
    icon?: React.ReactNode;
    title: string;
    description?: string;
    action?: React.ReactNode;
  };
};

export function DataTableEmpty({ totalColumns, emptyState }: DataTableEmptyProps) {
  return (
    <TableRow className="hover:bg-transparent">
      <TableCell colSpan={totalColumns} className="h-48 p-6 text-center">
        <EmptyState
          icon={emptyState?.icon || <Inbox className="h-8 w-8 text-muted-foreground" />}
          title={emptyState?.title || "Nenhum dado encontrado"}
          description={emptyState?.description || "Não há registros correspondentes cadastrados."}
          action={emptyState?.action}
        />
      </TableCell>
    </TableRow>
  );
}
