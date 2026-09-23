"use client";

import { useRouter } from "next/navigation";
import { MoreHorizontal, Eye, Pencil, Trash2, Target, RefreshCcw } from "lucide-react";
import type { Lead } from "../types/lead.types";
import { LeadStatusBadge, LeadSourceBadge } from "./LeadBadges";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
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

type LeadContactInfo = {
  firstName: string;
  lastName?: string;
  email: string | null;
  phone: string | null;
};

interface LeadTableProps {
  leads: Lead[];
  isLoading?: boolean;
  onDelete?: (lead: Lead) => void;
  contacts?: Record<string, LeadContactInfo>;
  responsibleMap?: Record<string, string>;
  onConvert?: (lead: Lead) => void;
  canConvert?: boolean;
  emptyState?: boolean;
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });
}

function getInitials(name: string): string {
  return name
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0]?.toUpperCase() ?? "")
    .join("");
}

export function LeadTable({
  leads,
  isLoading,
  onDelete,
  contacts,
  responsibleMap,
  onConvert,
  canConvert,
  emptyState = true,
}: LeadTableProps) {
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

  if (leads.length === 0 && emptyState) {
    return (
      <div className="flex flex-col items-center justify-center py-12 text-muted-foreground">
        <Target className="mb-4 h-12 w-12 opacity-50" />
        <p className="text-lg font-medium">Nenhum lead encontrado</p>
        <p className="text-sm">Crie um lead para começar a qualificar.</p>
      </div>
    );
  }

  if (leads.length === 0) {
    return null;
  }

  return (
    <div className="rounded-md border">
      <div className="overflow-x-auto">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Lead</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>E-mail</TableHead>
              <TableHead>Telefone</TableHead>
              <TableHead>Fonte</TableHead>
              <TableHead>Score</TableHead>
              <TableHead>Responsável</TableHead>
              <TableHead>Criado em</TableHead>
              <TableHead className="w-[60px]"></TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {leads.map((lead) => {
              const contact = lead.contactId ? contacts?.[lead.contactId] : undefined;
              const name = contact
                ? `${contact.firstName}${contact.lastName ? ` ${contact.lastName}` : ""}`
                : "Contato desconhecido";
              return (
                <TableRow key={lead.id}>
                  <TableCell>
                    <div className="flex items-center gap-3">
                      <Avatar className="h-8 w-8">
                        <AvatarFallback className="bg-muted text-xs">
                          {getInitials(name)}
                        </AvatarFallback>
                      </Avatar>
                      <div className="leading-tight">
                        <p className="font-medium">{name}</p>
                      </div>
                    </div>
                  </TableCell>
                  <TableCell>
                    <LeadStatusBadge status={lead.status} />
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {contact?.email ?? "—"}
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {contact?.phone ?? "—"}
                  </TableCell>
                  <TableCell>
                    <LeadSourceBadge source={lead.source} />
                  </TableCell>
                  <TableCell className="text-sm font-medium">{lead.score}</TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {lead.assignedTo
                      ? (responsibleMap?.[lead.assignedTo] ?? lead.assignedTo.slice(0, 8))
                      : "—"}
                  </TableCell>
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
                        {canConvert && lead.status !== "CONVERTED" && (
                          <>
                            <DropdownMenuSeparator />
                            <DropdownMenuItem onClick={() => onConvert?.(lead)}>
                              <RefreshCcw className="mr-2 h-4 w-4" />
                              Converter em contato
                            </DropdownMenuItem>
                          </>
                        )}
                        {onDelete && (
                          <>
                            <DropdownMenuSeparator />
                            <DropdownMenuItem
                              onClick={() => onDelete?.(lead)}
                              className="text-destructive"
                            >
                              <Trash2 className="mr-2 h-4 w-4" />
                              Excluir
                            </DropdownMenuItem>
                          </>
                        )}
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      </div>
    </div>
  );
}
