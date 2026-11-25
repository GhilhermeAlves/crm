"use client";

import { useState } from "react";
import Link from "next/link";
import { Loader2, Plus, Search, Truck } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { EmptyState } from "@/components/common/EmptyState";
import { ErrorCard } from "@/components/common/ErrorCard";
import { ListPagination } from "@/components/common/ListPagination";
import { ROUTES } from "@/lib/constants";
import { NativeSelect, Pill } from "@/features/masterdata/people/components/FormBits";
import { usePeoplePermissions, useSuppliers } from "@/features/masterdata/people/hooks/usePeople";
import {
  SUPPLIER_TYPE_LABELS,
  formatPhone,
  formatTaxId,
} from "@/features/masterdata/people/lib/people-labels";

/** Pessoas → Fornecedores (doc 22.4). */
export default function SuppliersPage() {
  const { canSuppliers } = usePeoplePermissions();
  const [draft, setDraft] = useState("");
  const [q, setQ] = useState("");
  const [type, setType] = useState("");
  const [status, setStatus] = useState("true");
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(25);
  const { data, isLoading, isError, refetch, isFetching } = useSuppliers({
    q,
    type,
    active: status === "" ? undefined : status,
    page,
    size,
  });

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-bold tracking-tight">
          Fornecedores{data ? ` (${data.total})` : ""}
        </h1>
        {canSuppliers && (
          <Button asChild>
            <Link href={`${ROUTES.PEOPLE}/new?role=SUPPLIER`}>
              <Plus className="mr-2 h-4 w-4" /> Novo fornecedor
            </Link>
          </Button>
        )}
      </div>
      <form
        className="flex flex-col gap-3 sm:flex-row sm:flex-wrap"
        onSubmit={(e) => {
          e.preventDefault();
          setQ(draft.trim());
          setPage(0);
        }}
      >
        <div className="relative min-w-[240px] flex-1">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            placeholder="Razão social, CNPJ, telefone ou e-mail"
            className="pl-9"
            aria-label="Buscar fornecedor"
          />
        </div>
        <NativeSelect
          aria-label="Tipo"
          className="sm:w-48"
          value={type}
          onChange={(e) => {
            setType(e.target.value);
            setPage(0);
          }}
        >
          <option value="">Todos os tipos</option>
          {Object.entries(SUPPLIER_TYPE_LABELS).map(([k, l]) => (
            <option key={k} value={k}>
              {l}
            </option>
          ))}
        </NativeSelect>
        <NativeSelect
          aria-label="Situação"
          className="sm:w-36"
          value={status}
          onChange={(e) => {
            setStatus(e.target.value);
            setPage(0);
          }}
        >
          <option value="true">Ativos</option>
          <option value="false">Inativos</option>
          <option value="">Todos</option>
        </NativeSelect>
        <Button type="submit" variant="outline">
          Buscar
        </Button>
      </form>
      {isLoading ? (
        <div className="flex items-center justify-center gap-2 py-20 text-muted-foreground">
          <Loader2 className="h-5 w-5 animate-spin" /> Carregando…
        </div>
      ) : isError ? (
        <ErrorCard message="Não foi possível carregar os fornecedores." onRetry={() => refetch()} />
      ) : !data || data.items.length === 0 ? (
        <EmptyState
          icon={<Truck className="h-8 w-8" />}
          title="Nenhum fornecedor"
          description="Cadastre laboratórios, dentais e prestadores de serviço."
        />
      ) : (
        <div className="space-y-4">
          <div className="overflow-x-auto rounded-lg border">
            <table className="w-full text-sm">
              <thead className="bg-muted/50 text-left text-xs uppercase tracking-wide text-muted-foreground">
                <tr>
                  <th className="px-4 py-3">Nome</th>
                  <th className="px-4 py-3">CNPJ/CPF</th>
                  <th className="px-4 py-3">Tipo</th>
                  <th className="px-4 py-3">Telefone</th>
                  <th className="px-4 py-3">E-mail</th>
                  <th className="px-4 py-3">Situação</th>
                </tr>
              </thead>
              <tbody className={isFetching ? "opacity-60" : undefined}>
                {data.items.map((s) => (
                  <tr key={s.id} className="border-t hover:bg-muted/30">
                    <td className="px-4 py-3">
                      <Link
                        href={`${ROUTES.PEOPLE}/${s.id}`}
                        className="font-medium text-primary hover:underline"
                      >
                        {s.name}
                      </Link>
                      {s.tradeName && (
                        <div className="text-xs text-muted-foreground">{s.tradeName}</div>
                      )}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3">{formatTaxId(s.taxId)}</td>
                    <td className="px-4 py-3">{SUPPLIER_TYPE_LABELS[s.supplierType]}</td>
                    <td className="whitespace-nowrap px-4 py-3">{formatPhone(s.phone)}</td>
                    <td className="px-4 py-3">{s.email ?? "—"}</td>
                    <td className="px-4 py-3">
                      {s.blocked ? (
                        <Pill tone="red">Bloqueado</Pill>
                      ) : s.active ? (
                        <Pill tone="green">Ativo</Pill>
                      ) : (
                        <Pill>Inativo</Pill>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <ListPagination
            page={page}
            pageSize={size}
            totalPages={Math.max(1, Math.ceil(data.total / size))}
            totalElements={data.total}
            onPageChange={setPage}
            onPageSizeChange={(s) => {
              setSize(s);
              setPage(0);
            }}
          />
        </div>
      )}
    </div>
  );
}
