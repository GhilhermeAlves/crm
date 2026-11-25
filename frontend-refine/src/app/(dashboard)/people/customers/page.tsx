"use client";

import { useState } from "react";
import Link from "next/link";
import { HeartPulse, Loader2, Plus, Search, UsersRound } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { EmptyState } from "@/components/common/EmptyState";
import { ErrorCard } from "@/components/common/ErrorCard";
import { ListPagination } from "@/components/common/ListPagination";
import { ROUTES } from "@/lib/constants";
import { NativeSelect, Pill } from "@/features/masterdata/people/components/FormBits";
import {
  useCustomers,
  useHealthSegment,
  usePeoplePermissions,
} from "@/features/masterdata/people/hooks/usePeople";
import {
  formatDate,
  formatPhone,
  formatTaxId,
} from "@/features/masterdata/people/lib/people-labels";

const MONTHS = ["Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez"];

/** Pessoas → Clientes (no segmento saúde, os pacientes). Doc 22.3. */
export default function CustomersPage() {
  const health = useHealthSegment();
  const { canCreate } = usePeoplePermissions();
  const [draft, setDraft] = useState("");
  const [q, setQ] = useState("");
  const [status, setStatus] = useState("true");
  const [blocked, setBlocked] = useState("");
  const [plan, setPlan] = useState("");
  const [upcoming, setUpcoming] = useState(false);
  const [birthMonth, setBirthMonth] = useState("");
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(25);
  const { data, isLoading, isError, refetch, isFetching } = useCustomers({
    q,
    active: status === "" ? undefined : status,
    blocked: blocked === "" ? undefined : blocked,
    healthPlan: plan,
    upcoming: upcoming || undefined,
    birthMonth: birthMonth || undefined,
    page,
    size,
  });
  const reset = () => setPage(0);

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-bold tracking-tight">
          {health ? "Clientes (pacientes)" : "Clientes"}
          {data ? ` (${data.total})` : ""}
        </h1>
        {canCreate && (
          <Button asChild>
            <Link href={`${ROUTES.PEOPLE}/new?role=CUSTOMER`}>
              <Plus className="mr-2 h-4 w-4" /> {health ? "Novo paciente" : "Novo cliente"}
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
            placeholder={
              health
                ? "Nome, CPF, celular, prontuário ou código"
                : "Nome, CPF/CNPJ, celular ou código"
            }
            className="pl-9"
            aria-label="Buscar cliente"
          />
        </div>
        <NativeSelect
          aria-label="Situação"
          className="lg:w-36"
          value={status}
          onChange={(e) => {
            setStatus(e.target.value);
            reset();
          }}
        >
          <option value="true">Ativos</option>
          <option value="false">Inativos</option>
          <option value="">Todos</option>
        </NativeSelect>
        <NativeSelect
          aria-label="Bloqueio"
          className="lg:w-40"
          value={blocked}
          onChange={(e) => {
            setBlocked(e.target.value);
            reset();
          }}
        >
          <option value="">Bloqueados ou não</option>
          <option value="true">Só bloqueados</option>
          <option value="false">Só liberados</option>
        </NativeSelect>
        {health && (
          <>
            <Input
              aria-label="Convênio"
              className="lg:w-40"
              placeholder="Convênio"
              value={plan}
              onChange={(e) => {
                setPlan(e.target.value);
                reset();
              }}
            />
            <NativeSelect
              aria-label="Aniversariantes do mês"
              className="lg:w-40"
              value={birthMonth}
              onChange={(e) => {
                setBirthMonth(e.target.value);
                reset();
              }}
            >
              <option value="">Aniversário: todos</option>
              {MONTHS.map((m, i) => (
                <option key={m} value={i + 1}>
                  Aniversário em {m}
                </option>
              ))}
            </NativeSelect>
            <label className="flex items-center gap-2 text-sm">
              <input
                type="checkbox"
                checked={upcoming}
                onChange={(e) => {
                  setUpcoming(e.target.checked);
                  reset();
                }}
              />
              Com consulta futura
            </label>
          </>
        )}
        <Button type="submit" variant="outline">
          Buscar
        </Button>
      </form>

      {isLoading ? (
        <div className="flex items-center justify-center gap-2 py-20 text-muted-foreground">
          <Loader2 className="h-5 w-5 animate-spin" /> Carregando…
        </div>
      ) : isError ? (
        <ErrorCard message="Não foi possível carregar os clientes." onRetry={() => refetch()} />
      ) : !data || data.items.length === 0 ? (
        <EmptyState
          icon={<UsersRound className="h-8 w-8" />}
          title="Nenhum cliente encontrado"
          description="Ajuste os filtros ou cadastre um cliente."
        />
      ) : (
        <div className="space-y-4">
          <div className="overflow-x-auto rounded-lg border">
            <table className="w-full text-sm">
              <thead className="bg-muted/50 text-left text-xs uppercase tracking-wide text-muted-foreground">
                <tr>
                  <th className="px-4 py-3">Nome</th>
                  <th className="px-4 py-3">CPF/CNPJ</th>
                  <th className="px-4 py-3">Celular</th>
                  {health ? (
                    <>
                      <th className="px-4 py-3">Prontuário</th>
                      <th className="px-4 py-3">Convênio</th>
                    </>
                  ) : (
                    <th className="px-4 py-3">Código</th>
                  )}
                  <th className="px-4 py-3">Cliente desde</th>
                  <th className="px-4 py-3">Situação</th>
                  {health && (
                    <th className="px-4 py-3">
                      <span className="sr-only">Ficha clínica</span>
                    </th>
                  )}
                </tr>
              </thead>
              <tbody className={isFetching ? "opacity-60" : undefined}>
                {data.items.map((c) => (
                  <tr key={c.id} className="border-t hover:bg-muted/30">
                    <td className="px-4 py-3">
                      <Link
                        href={`${ROUTES.PEOPLE}/${c.id}`}
                        className="font-medium text-primary hover:underline"
                      >
                        {c.name}
                      </Link>
                    </td>
                    <td className="whitespace-nowrap px-4 py-3">{formatTaxId(c.taxId)}</td>
                    <td className="whitespace-nowrap px-4 py-3">{formatPhone(c.mobile)}</td>
                    {health ? (
                      <>
                        <td className="px-4 py-3">{c.recordNumber ?? "—"}</td>
                        <td className="px-4 py-3">{c.healthPlan ?? "—"}</td>
                      </>
                    ) : (
                      <td className="px-4 py-3">{c.customerCode ?? "—"}</td>
                    )}
                    <td className="px-4 py-3">{formatDate(c.customerSince)}</td>
                    <td className="px-4 py-3">
                      {c.blocked ? (
                        <Pill tone="red">Bloqueado</Pill>
                      ) : c.active ? (
                        <Pill tone="green">Ativo</Pill>
                      ) : (
                        <Pill>Inativo</Pill>
                      )}
                    </td>
                    {health && (
                      <td className="px-4 py-3 text-right">
                        {c.patient && (
                          <Button asChild size="sm" variant="ghost">
                            <Link href={`${ROUTES.PATIENTS}/${c.id}`}>
                              <HeartPulse className="mr-1 h-4 w-4" /> Ficha clínica
                            </Link>
                          </Button>
                        )}
                      </td>
                    )}
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
