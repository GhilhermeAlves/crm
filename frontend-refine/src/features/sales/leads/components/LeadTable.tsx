"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { MoreHorizontal, Eye, Pencil, Trash2, Target, RefreshCcw } from "lucide-react";
import type { Lead } from "../types/lead.types";
import { LeadStatusBadge, LeadSourceBadge } from "./LeadBadges";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { DataTable } from "@/components/ui/data-table";
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
    <DataTable
      tableId="leads"
      data={leads ?? []}
      columns={[
        {
          id: "lead",
          header: "Lead",
          cell: (lead) => {
            const contact = lead.contactId ? contacts?.[lead.contactId] : undefined;
            const name = contact
              ? `${contact.firstName}${contact.lastName ? ` ${contact.lastName}` : ""}`
              : "Contato desconhecido";
            return (
              <div className="flex items-center gap-3">
                <Avatar className="h-8 w-8">
                  <AvatarFallback className="bg-muted text-xs">{getInitials(name)}</AvatarFallback>
                </Avatar>
                <div className="leading-tight">
                  {contact ? (
                    <Link
                      href={`${ROUTES.CONTACTS}/${lead.contactId}`}
                      className="font-medium hover:text-primary hover:underline"
                    >
                      {name}
                    </Link>
                  ) : (
                    <p className="font-medium">{name}</p>
                  )}
                </div>
              </div>
            );
          },
        },
        {
          id: "status",
          header: "Status",
          cell: (lead) => <LeadStatusBadge status={lead.status} />,
        },
        {
          id: "e-mail",
          header: "E-mail",
          className: "text-sm text-muted-foreground",
          cell: (lead) => {
            const contact = lead.contactId ? contacts?.[lead.contactId] : undefined;
            return <>{contact?.email ?? "—"}</>;
          },
        },
        {
          id: "telefone",
          header: "Telefone",
          className: "text-sm text-muted-foreground",
          cell: (lead) => {
            const contact = lead.contactId ? contacts?.[lead.contactId] : undefined;
            return <>{contact?.phone ?? "—"}</>;
          },
        },
        { id: "fonte", header: "Fonte", cell: (lead) => <LeadSourceBadge source={lead.source} /> },
        {
          id: "score",
          header: "Score",
          className: "text-sm font-medium",
          sortValue: (lead) => lead.score,
          cell: (lead) => <>{lead.score}</>,
        },
        {
          id: "responsavel",
          header: "Responsável",
          className: "text-sm text-muted-foreground",
          cell: (lead) => (
            <>
              {lead.assignedTo
                ? (responsibleMap?.[lead.assignedTo] ?? lead.assignedTo.slice(0, 8))
                : "—"}
            </>
          ),
        },
        {
          id: "criado-em",
          header: "Criado em",
          className: "text-sm text-muted-foreground",
          sortValue: (lead) => lead.createdAt,
          cell: (lead) => <>{formatDate(lead.createdAt)}</>,
        },
        {
          id: "acoes",
          header: "",
          label: "Ações",
          hideable: false,
          headerClassName: "w-[60px]",
          cell: (lead) => (
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="ghost" size="icon" className="h-8 w-8" aria-label="Ações do lead">
                  <MoreHorizontal className="h-4 w-4" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end">
                <DropdownMenuItem onClick={() => router.push(`${ROUTES.LEADS}/${lead.id}`)}>
                  <Eye className="mr-2 h-4 w-4" />
                  Visualizar
                </DropdownMenuItem>
                <DropdownMenuItem onClick={() => router.push(`${ROUTES.LEADS}/${lead.id}/edit`)}>
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
                    <DropdownMenuItem onClick={() => onDelete?.(lead)} className="text-destructive">
                      <Trash2 className="mr-2 h-4 w-4" />
                      Excluir
                    </DropdownMenuItem>
                  </>
                )}
              </DropdownMenuContent>
            </DropdownMenu>
          ),
        },
      ]}
      keyExtractor={(lead) => lead.id}
    />
  );
}
