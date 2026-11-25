"use client";

import { useState } from "react";
import Link from "next/link";
import { Loader2, Plus, Search, UsersRound } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { EmptyState } from "@/components/common/EmptyState";
import { ErrorCard } from "@/components/common/ErrorCard";
import { ListPagination } from "@/components/common/ListPagination";
import { ROUTES } from "@/lib/constants";
import { NativeSelect, Pill } from "@/features/masterdata/people/components/FormBits";
import {
  usePeoplePermissions,
  usePeopleSearch,
} from "@/features/masterdata/people/hooks/usePeople";
import {
  ROLE_LABELS,
  formatPhone,
  formatTaxId,
  UF_LIST,
} from "@/features/masterdata/people/lib/people-labels";

/** Pessoas → lista central (doc 22.2). */
export default function PeoplePage() {
  const { canCreate } = usePeoplePermissions();
  const [draft, setDraft] = useState("");
  const [q, setQ] = useState("");
  const [type, setType] = useState("");
  const [role, setRole] = useState("");
  const [status, setStatus] = useState("true");
  const [provisional, setProvisional] = useState("");
  const [uf, setUf] = useState("");
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(25);

  const { data, isLoading, isError, refetch, isFetching } = usePeopleSearch({
    q,
    type,
    role,
    active: status === "" ? undefined : status,
    provisional: provisional === "" ? undefined : provisional,
    uf,
    page,
    size,
  });
  const reset = () => setPage(0);

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-bold tracking-tight">
          Pessoas{data ? ` (${data.total})` : ""}
        </h1>
        {canCreate && (
          <Button asChild>
            <Link href={`${ROUTES.PEOPLE}/new`}>
              <Plus className="mr-2 h-4 w-4" /> Nova pessoa
            </Link>
          </Button>
        )}
      </div>

      <form
        className="flex flex-col gap-3 lg:flex-row lg:flex-wrap lg:items-end"
        onSubmit={(e) => {
          e.preventDefault();
          setQ(draft.trim());
          reset();
        }}
      >
        <div className="relative min-w-[240px] flex-1">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            placeholder="Nome, CPF/CNPJ, telefone ou e-mail"
            className="pl-9"
            aria-label="Buscar pessoa"
          />
        </div>
        <NativeSelect
          aria-label="Tipo"
          className="lg:w-40"
          value={type}
          onChange={(e) => {
            setType(e.target.value);
            reset();
          }}
        >
          <option value="">Física e jurídica</option>
          <option value="F">Pessoa física</option>
          <option value="J">Pessoa jurídica</option>
        </NativeSelect>
        <NativeSelect
          aria-label="Papel"
          className="lg:w-44"
          value={role}
          onChange={(e) => {
            setRole(e.target.value);
            reset();
          }}
        >
          <option value="">Todos os papéis</option>
          <option value="CUSTOMER">Clientes</option>
          <option value="PATIENT">Pacientes</option>
          <option value="SUPPLIER">Fornecedores</option>
          <option value="EMPLOYEE">Funcionários</option>
          <option value="NONE">Sem papel</option>
        </NativeSelect>
        <NativeSelect
          aria-label="Situação"
          className="lg:w-36"
          value={status}
          onChange={(e) => {
            setStatus(e.target.value);
            reset();
          }}
        >
          <option value="true">Ativas</option>
          <option value="false">Inativas</option>
          <option value="">Todas</option>
        </NativeSelect>
        <NativeSelect
          aria-label="Provisório"
          className="lg:w-40"
          value={provisional}
          onChange={(e) => {
            setProvisional(e.target.value);
            reset();
          }}
        >
          <option value="">Provisórias ou não</option>
          <option value="true">Só provisórias</option>
          <option value="false">Sem provisórias</option>
        </NativeSelect>
        <NativeSelect
          aria-label="UF"
          className="lg:w-24"
          value={uf}
          onChange={(e) => {
            setUf(e.target.value);
            reset();
          }}
        >
          <option value="">UF</option>
          {UF_LIST.map((u) => (
            <option key={u} value={u}>
              {u}
            </option>
          ))}
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
        <ErrorCard message="Não foi possível carregar as pessoas." onRetry={() => refetch()} />
      ) : !data || data.items.length === 0 ? (
        <EmptyState
          icon={<UsersRound className="h-8 w-8" />}
          title="Nenhuma pessoa encontrada"
          description="Ajuste os filtros ou cadastre uma nova pessoa."
        />
      ) : (
        <div className="space-y-4">
          <div className="overflow-x-auto rounded-lg border">
            <table className="w-full text-sm">
              <thead className="bg-muted/50 text-left text-xs uppercase tracking-wide text-muted-foreground">
                <tr>
                  <th className="px-4 py-3">Nome</th>
                  <th className="px-4 py-3">CPF/CNPJ</th>
                  <th className="px-4 py-3">Papéis</th>
                  <th className="px-4 py-3">Celular</th>
                  <th className="px-4 py-3">Cidade</th>
                  <th className="px-4 py-3">Situação</th>
                </tr>
              </thead>
              <tbody className={isFetching ? "opacity-60" : undefined}>
                {data.items.map((p) => (
                  <tr key={p.id} className="border-t hover:bg-muted/30">
                    <td className="px-4 py-3">
                      <Link
                        href={`${ROUTES.PEOPLE}/${p.id}`}
                        className="font-medium text-primary hover:underline"
                      >
                        {p.name}
                      </Link>
                      {p.tradeName && (
                        <div className="text-xs text-muted-foreground">{p.tradeName}</div>
                      )}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3">{formatTaxId(p.taxId)}</td>
                    <td className="px-4 py-3">
                      <div className="flex flex-wrap gap-1">
                        {p.roles.length === 0 && <span className="text-muted-foreground">—</span>}
                        {p.roles.map((r) => (
                          <Pill
                            key={r}
                            tone={
                              r === "PATIENT"
                                ? "blue"
                                : r === "SUPPLIER"
                                  ? "amber"
                                  : r === "EMPLOYEE"
                                    ? "purple"
                                    : "green"
                            }
                          >
                            {ROLE_LABELS[r]}
                          </Pill>
                        ))}
                      </div>
                    </td>
                    <td className="whitespace-nowrap px-4 py-3">{formatPhone(p.mobile)}</td>
                    <td className="px-4 py-3">{p.city ? `${p.city}/${p.uf ?? ""}` : "—"}</td>
                    <td className="px-4 py-3">
                      {p.provisional ? (
                        <Pill tone="amber">Provisório</Pill>
                      ) : p.active ? (
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
