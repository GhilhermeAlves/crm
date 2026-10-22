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
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
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
  return (
    <div className="overflow-x-auto rounded-md border">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Nome</TableHead>
            <TableHead>CPF</TableHead>
            <TableHead>Celular</TableHead>
            <TableHead>Prontuário</TableHead>
            <TableHead>Paciente desde</TableHead>
            <TableHead className="w-12">
              <span className="sr-only">Ações</span>
            </TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {patients.map((patient) => (
            <TableRow key={patient.id}>
              <TableCell className="font-medium">
                <Link
                  href={`${ROUTES.PATIENTS}/${patient.id}`}
                  className="hover:text-primary hover:underline"
                >
                  {fullName(patient)}
                </Link>
              </TableCell>
              <TableCell>{patient.cpf ? formatCpf(patient.cpf) : "—"}</TableCell>
              <TableCell>{patient.mobile ? formatPhone(patient.mobile) : "—"}</TableCell>
              <TableCell>{patient.recordNumber ?? "—"}</TableCell>
              <TableCell>{formatSince(patient.patientSince)}</TableCell>
              <TableCell>
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button
                      variant="ghost"
                      size="icon"
                      aria-label={`Ações de ${fullName(patient)}`}
                    >
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
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}
