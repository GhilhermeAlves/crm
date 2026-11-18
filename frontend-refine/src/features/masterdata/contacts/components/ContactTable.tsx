"use client";

import { useState } from "react";
import Link from "next/link";
import { Pencil, Trash2, XCircle, History } from "lucide-react";
import type { Contact } from "../types/contact.types";
import { ROUTES } from "@/lib/constants";
import { formatCpf, formatPhone } from "@/lib/format";
import { Checkbox } from "@/components/ui/checkbox";
import { DataTable, type ColumnDef } from "@/components/ui/data-table";
import { Button } from "@/components/ui/button";
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from "@/components/ui/tooltip";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { EmptyState } from "@/components/common/EmptyState";
import { Users } from "lucide-react";

const formatDate = (iso: string): string =>
  new Date(iso).toLocaleDateString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });

const formatDateTime = (iso: string): string =>
  new Date(iso).toLocaleString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });

const fullName = (c: Contact): string => {
  const first = c.firstName ?? "";
  const last = c.lastName ?? "";
  return `${first}${last ? ` ${last}` : ""}`;
};

type Props = {
  contacts: Contact[];
  isLoading?: boolean;
  onEdit?: (contact: Contact) => void;
  onDelete?: (contact: Contact) => void;
};

export function ContactTable({ contacts, isLoading, onEdit, onDelete }: Props) {
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [auditContact, setAuditContact] = useState<Contact | null>(null);

  const allSelected = contacts.length > 0 && selectedIds.size === contacts.length;

  const toggleAll = () => {
    if (allSelected) {
      setSelectedIds(new Set());
    } else {
      setSelectedIds(new Set(contacts.map((c) => c.id)));
    }
  };

  const toggleOne = (id: string) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const columns: ColumnDef<Contact>[] = [
    {
      id: "select",
      label: "Seleção",
      hideable: false,
      headerClassName: "w-[40px]",
      header: (
        <Checkbox checked={allSelected} onCheckedChange={toggleAll} aria-label="Selecionar todos" />
      ),
      cell: (c) => (
        <Checkbox
          checked={selectedIds.has(c.id)}
          onCheckedChange={() => toggleOne(c.id)}
          aria-label={`Selecionar ${fullName(c)}`}
        />
      ),
    },
    {
      id: "name",
      header: "Nome",
      hideable: false,
      sortValue: fullName,
      cell: (c) => (
        <Link href={`${ROUTES.CONTACTS}/${c.id}`} className="font-medium hover:underline">
          {fullName(c)}
        </Link>
      ),
    },
    {
      id: "cpf",
      header: "CPF",
      className: "text-sm text-muted-foreground",
      sortValue: (c) => c.cpf,
      cell: (c) => (c.cpf ? formatCpf(c.cpf) : "—"),
    },
    {
      id: "mobile",
      header: "Celular",
      className: "text-sm text-muted-foreground",
      cell: (c) => (c.mobile ? formatPhone(c.mobile) : c.phone ? formatPhone(c.phone) : "—"),
    },
    {
      id: "recordNumber",
      header: "Prontuário",
      className: "text-sm text-muted-foreground",
      sortValue: (c) => c.recordNumber,
      cell: (c) => c.recordNumber ?? "—",
    },
    {
      id: "createdAt",
      header: "Paciente desde",
      className: "text-sm text-muted-foreground",
      sortValue: (c) => c.createdAt,
      cell: (c) => formatDate(c.createdAt),
    },
    {
      id: "actions",
      header: "Ações",
      hideable: false,
      headerClassName: "w-[160px] text-center",
      cell: (c) => (
        <div className="flex items-center justify-center gap-1">
          {onEdit && (
            <Tooltip>
              <TooltipTrigger asChild>
                <Button variant="ghost" size="icon" className="h-8 w-8" onClick={() => onEdit(c)}>
                  <Pencil className="h-4 w-4" />
                </Button>
              </TooltipTrigger>
              <TooltipContent>Editar</TooltipContent>
            </Tooltip>
          )}
          {onDelete && (
            <Tooltip>
              <TooltipTrigger asChild>
                <Button
                  variant="ghost"
                  size="icon"
                  className="h-8 w-8 text-destructive hover:text-destructive"
                  onClick={() => onDelete(c)}
                >
                  <Trash2 className="h-4 w-4" />
                </Button>
              </TooltipTrigger>
              <TooltipContent>Excluir</TooltipContent>
            </Tooltip>
          )}
          <Tooltip>
            <TooltipTrigger asChild>
              <Button
                variant="ghost"
                size="icon"
                className="h-8 w-8"
                onClick={() => setAuditContact(c)}
              >
                <History className="h-4 w-4" />
              </Button>
            </TooltipTrigger>
            <TooltipContent>Auditoria</TooltipContent>
          </Tooltip>
        </div>
      ),
    },
  ];

  if (isLoading) {
    return (
      <div className="space-y-3">
        {Array.from({ length: 5 }).map((_, i) => (
          <div key={i} className="h-14 animate-pulse rounded-lg border bg-muted" />
        ))}
      </div>
    );
  }

  if (contacts.length === 0) {
    return (
      <EmptyState
        icon={<Users className="h-8 w-8" />}
        title="Nenhum contato cadastrado ainda."
        description="Crie seu primeiro contato para começar a organizar seus clientes."
      />
    );
  }

  return (
    <TooltipProvider delayDuration={300}>
      <DataTable tableId="contacts" data={contacts} columns={columns} bordered={false} />

      <Dialog open={!!auditContact} onOpenChange={(open) => !open && setAuditContact(null)}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>Auditoria — {auditContact ? fullName(auditContact) : ""}</DialogTitle>
          </DialogHeader>
          {auditContact && (
            <div className="space-y-3 text-sm">
              <div className="flex justify-between border-b pb-2">
                <span className="text-muted-foreground">Criado em</span>
                <span>{formatDateTime(auditContact.createdAt)}</span>
              </div>
              <div className="flex justify-between border-b pb-2">
                <span className="text-muted-foreground">ID</span>
                <span className="font-mono text-xs">{auditContact.id}</span>
              </div>
              <div className="flex justify-between border-b pb-2">
                <span className="text-muted-foreground">Empresa</span>
                <span className="font-mono text-xs">{auditContact.companyId}</span>
              </div>
              <p className="pt-2 text-xs text-muted-foreground">
                Para mais detalhes de auditoria, consulte o painel de auditoria do sistema.
              </p>
            </div>
          )}
        </DialogContent>
      </Dialog>
    </TooltipProvider>
  );
}
