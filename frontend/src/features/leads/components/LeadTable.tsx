"use client";

import { useRouter } from "next/navigation";
import { MoreHorizontal, Eye, Pencil, Trash2, Target } from "lucide-react";
import type { Lead } from "../types/lead.types";
import { LeadStatusBadge, LeadSourceBadge, LeadClassificationBadge } from "./LeadBadges";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { ROUTES } from "@/lib/constants";

interface LeadTableProps {
  leads: Lead[];
  isLoading?: boolean;
  onDelete?: (lead: Lead) => void;
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });
}

export function LeadTable({ leads, isLoading, onDelete }: LeadTableProps) {
  const router = useRouter();

  if (isLoading) {
    return (
      <div className="space-y-3">
        {Array.from({ length: 5 }).map((_, i) => (
          <div key={i} className="h-16 animate-pulse rounded bg-muted" />
        ))}
      </div>
    );
  }

  if (leads.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-12 text-muted-foreground">
        <Target className="mb-4 h-12 w-12 opacity-50" />
        <p className="text-lg font-medium">Nenhum lead encontrado</p>
        <p className="text-sm">Crie um lead para começar a qualificar.</p>
      </div>
    );
  }

  return (
    <div className="rounded-md border">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Status</TableHead>
            <TableHead>Origem</TableHead>
            <TableHead>Classificação</TableHead>
            <TableHead>Score</TableHead>
            <TableHead>Criado em</TableHead>
            <TableHead className="w-[60px]"></TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {leads.map((lead) => (
            <TableRow key={lead.id}>
              <TableCell>
                <LeadStatusBadge status={lead.status} />
              </TableCell>
              <TableCell>
                <LeadSourceBadge source={lead.source} />
              </TableCell>
              <TableCell>
                <LeadClassificationBadge classification={lead.classification} />
              </TableCell>
              <TableCell className="text-sm font-medium">{lead.score}</TableCell>
              <TableCell className="text-sm text-muted-foreground">
                {formatDate(lead.createdAt)}
              </TableCell>
              <TableCell>
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button variant="ghost" size="icon" className="h-8 w-8">
                      <MoreHorizontal className="h-4 w-4" />
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="end">
                    <DropdownMenuItem onClick={() => router.push(`${ROUTES.LEADS}/${lead.id}`)}>
                      <Eye className="mr-2 h-4 w-4" />
                      Visualizar
                    </DropdownMenuItem>
                    <DropdownMenuItem
                      onClick={() => router.push(`${ROUTES.LEADS}/${lead.id}/edit`)}
                    >
                      <Pencil className="mr-2 h-4 w-4" />
                      Editar
                    </DropdownMenuItem>
                    <DropdownMenuSeparator />
                    <DropdownMenuItem onClick={() => onDelete?.(lead)} className="text-destructive">
                      <Trash2 className="mr-2 h-4 w-4" />
                      Excluir
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}
