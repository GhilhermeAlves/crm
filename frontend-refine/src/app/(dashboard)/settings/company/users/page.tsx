"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { format, parseISO } from "date-fns";
import { ptBR } from "date-fns/locale";
import { ArrowUpDown, Pencil, Plus } from "lucide-react";

import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { useMembers } from "@/features/identity/members/hooks/useMembers";
import { Button } from "@/components/ui/button";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";

type SortField = "name" | "joinedAt";
type SortDir = "asc" | "desc";

export default function ManageUsersPage() {
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;
  const { data: members = [], isLoading } = useMembers(companyId);
  const [sortField, setSortField] = useState<SortField>("name");
  const [sortDir, setSortDir] = useState<SortDir>("asc");

  const toggleSort = (field: SortField) => {
    if (sortField === field) {
      setSortDir((d) => (d === "asc" ? "desc" : "asc"));
    } else {
      setSortField(field);
      setSortDir("asc");
    }
  };

  const sorted = useMemo(() => {
    const list = [...members];
    list.sort((a, b) => {
      let cmp = 0;
      if (sortField === "name") {
        cmp = (a.name ?? "").localeCompare(b.name ?? "");
      } else if (sortField === "joinedAt") {
        cmp = (a.joinedAt ?? "").localeCompare(b.joinedAt ?? "");
      }
      return sortDir === "asc" ? cmp : -cmp;
    });
    return list;
  }, [members, sortField, sortDir]);

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-semibold">Gerenciar usuários</h2>
        <Button asChild size="sm">
          <Link href="/settings/company/users/new">
            <Plus className="mr-1.5 h-3.5 w-3.5" />
            Novo usuário
          </Link>
        </Button>
      </div>

      <section className="rounded-lg border bg-card p-5">
        <h3 className="mb-4 text-sm font-semibold text-primary">Gestão de usuários</h3>

        {isLoading ? (
          <p className="py-8 text-center text-sm text-muted-foreground">Carregando...</p>
        ) : sorted.length === 0 ? (
          <p className="py-8 text-center text-sm text-muted-foreground">
            Nenhum usuário encontrado.
          </p>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>
                  <button
                    className="inline-flex items-center gap-1 text-xs font-medium"
                    onClick={() => toggleSort("name")}
                  >
                    Nome
                    <ArrowUpDown className="h-3 w-3" />
                  </button>
                </TableHead>
                <TableHead>
                  <button
                    className="inline-flex items-center gap-1 text-xs font-medium"
                    onClick={() => toggleSort("joinedAt")}
                  >
                    Data de criação
                    <ArrowUpDown className="h-3 w-3" />
                  </button>
                </TableHead>
                <TableHead className="text-xs">Celular</TableHead>
                <TableHead className="text-xs">E-mail</TableHead>
                <TableHead className="text-xs">Cargo</TableHead>
                <TableHead className="text-xs">Ações</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {sorted.map((m) => (
                <TableRow key={m.userId}>
                  <TableCell className="text-sm">{m.name}</TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {m.joinedAt
                      ? format(parseISO(m.joinedAt), "dd/MM/yyyy", {
                          locale: ptBR,
                        })
                      : "—"}
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">—</TableCell>
                  <TableCell className="text-sm text-muted-foreground">{m.email}</TableCell>
                  <TableCell className="text-sm text-muted-foreground">{m.role || "—"}</TableCell>
                  <TableCell>
                    <Button variant="ghost" size="icon" className="h-7 w-7">
                      <Pencil className="h-3.5 w-3.5" />
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </section>
    </div>
  );
}
