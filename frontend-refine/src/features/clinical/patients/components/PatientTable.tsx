"use client";

import Link from "next/link";
import { format, parseISO } from "date-fns";
import { MoreVertical } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { DataTable, type ColumnDef } from "@/components/ui/data-table";
import { formatCpf, formatPhone } from "@/lib/format";
import { ROUTES } from "@/lib/constants";
import type { Contact } from "@/features/masterdata/contacts/types/contact.types";

function fullName(patient: Contact): string {
  return [patient.firstName, patient.lastName].filter(Boolean).join(" ") || "Paciente";
}

function formatSince(value: string | null | undefined): string {
  if (!value) return "—";
  try {
    return format(parseISO(value), "dd/MM/yyyy");
  } catch {
    return "—";
  }
}

export function PatientTable({ patients, canUpdate }: { patients: Contact[]; canUpdate: boolean }) {
  const columns: ColumnDef<Contact>[] = [
    {
      id: "name",
      header: "Nome",
      className: "font-medium",
      hideable: false,
      sortValue: (p) => fullName(p),
      cell: (patient) => (
        <Link
          href={`${ROUTES.PATIENTS}/${patient.id}`}
          className="hover:text-primary hover:underline"
        >
          {fullName(patient)}
        </Link>
      ),
    },
    {
      id: "cpf",
      header: "CPF",
      sortValue: (p) => p.cpf,
      cell: (p) => (p.cpf ? formatCpf(p.cpf) : "—"),
    },
    { id: "mobile", header: "Celular", cell: (p) => (p.mobile ? formatPhone(p.mobile) : "—") },
    {
      id: "recordNumber",
      header: "Prontuário",
      sortValue: (p) => p.recordNumber,
      cell: (p) => p.recordNumber ?? "—",
    },
    {
      id: "patientSince",
      header: "Paciente desde",
      sortValue: (p) => p.patientSince,
      cell: (p) => formatSince(p.patientSince),
    },
    {
      id: "actions",
      header: <span className="sr-only">Ações</span>,
      label: "Ações",
      headerClassName: "w-12",
      hideable: false,
      cell: (patient) => (
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" size="icon" aria-label={`Ações de ${fullName(patient)}`}>
              <MoreVertical className="h-4 w-4" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuItem asChild>
              <Link href={`${ROUTES.PATIENTS}/${patient.id}`}>Ver ficha</Link>
            </DropdownMenuItem>
            {canUpdate && (
              <DropdownMenuItem asChild>
                <Link href={`${ROUTES.PATIENTS}/${patient.id}/edit`}>Editar cadastro</Link>
              </DropdownMenuItem>
            )}
          </DropdownMenuContent>
        </DropdownMenu>
      ),
    },
  ];

  return (
    <DataTable tableId="patients" data={patients} columns={columns} keyExtractor={(p) => p.id} />
  );
}
