"use client";

import { useState } from "react";
import Link from "next/link";
import { Briefcase, Loader2, Plus, Search } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { EmptyState } from "@/components/common/EmptyState";
import { ErrorCard } from "@/components/common/ErrorCard";
import { ListPagination } from "@/components/common/ListPagination";
import { ROUTES } from "@/lib/constants";
import { NativeSelect, Pill } from "@/features/masterdata/people/components/FormBits";
import {
  useEmployees,
  useJobTitles,
  usePeopleMutation,
  usePeoplePermissions,
} from "@/features/masterdata/people/hooks/usePeople";
import { PeopleService } from "@/features/masterdata/people/services/people.service";
import {
  EMPLOYEE_STATUS_LABELS,
  SPECIALTY_LABELS,
  formatDate,
} from "@/features/masterdata/people/lib/people-labels";

function JobTitlesDialog({
  open,
  onOpenChange,
}: {
  open: boolean;
  onOpenChange: (o: boolean) => void;
}) {
  const { data } = useJobTitles();
  const [title, setTitle] = useState("");
  const save = usePeopleMutation<{ id: string | null; title: string; active?: boolean }>(
    (c, v) => PeopleService.saveJobTitle(c, v.id, { title: v.title, active: v.active }),
    "Cargo salvo",
  );
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Cargos</DialogTitle>
        </DialogHeader>
        <form
          className="flex gap-2"
          onSubmit={(e) => {
            e.preventDefault();
            if (title.trim())
              save.mutate({ id: null, title: title.trim() }, { onSuccess: () => setTitle("") });
          }}
        >
          <Input
            aria-label="Novo cargo"
            placeholder="Ex.: Recepcionista"
            value={title}
            maxLength={80}
            onChange={(e) => setTitle(e.target.value)}
          />
          <Button type="submit" disabled={save.isPending}>
            Adicionar
          </Button>
        </form>
        <ul className="max-h-80 divide-y overflow-y-auto text-sm">
          {(data ?? []).map((j) => (
            <li key={j.id} className="flex items-center justify-between py-2">
              <span className={j.active ? "" : "text-muted-foreground line-through"}>
                {j.title}
              </span>
              <Button
                size="sm"
                variant="ghost"
                onClick={() => save.mutate({ id: j.id, title: j.title, active: !j.active })}
              >
                {j.active ? "Inativar" : "Ativar"}
              </Button>
            </li>
          ))}
          {(data ?? []).length === 0 && (
            <li className="py-2 text-muted-foreground">Nenhum cargo cadastrado.</li>
          )}
        </ul>
      </DialogContent>
    </Dialog>
  );
}

/** Pessoas → Funcionários e profissionais (doc 22.5). */
export default function EmployeesPage() {
  const { canEmployees } = usePeoplePermissions();
  const { data: titles } = useJobTitles();
  const [draft, setDraft] = useState("");
  const [q, setQ] = useState("");
  const [jobTitleId, setJobTitleId] = useState("");
  const [specialty, setSpecialty] = useState("");
  const [status, setStatus] = useState("ACTIVE");
  const [professional, setProfessional] = useState("");
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(25);
  const [titlesOpen, setTitlesOpen] = useState(false);
  const { data, isLoading, isError, refetch, isFetching } = useEmployees({
    q,
    jobTitleId,
    specialty,
    status,
    professional: professional === "" ? undefined : professional,
    page,
    size,
  });

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-bold tracking-tight">
          Funcionários{data ? ` (${data.total})` : ""}
        </h1>
        <div className="flex gap-2">
          {canEmployees && (
            <Button variant="outline" onClick={() => setTitlesOpen(true)}>
              <Briefcase className="mr-2 h-4 w-4" /> Cargos
            </Button>
          )}
          {canEmployees && (
            <Button asChild>
              <Link href={`${ROUTES.PEOPLE}/new?role=EMPLOYEE`}>
                <Plus className="mr-2 h-4 w-4" /> Novo funcionário
              </Link>
            </Button>
          )}
        </div>
      </div>
      <form
        className="flex flex-col gap-3 lg:flex-row lg:flex-wrap"
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
            placeholder="Nome, CPF ou telefone"
            className="pl-9"
            aria-label="Buscar funcionário"
          />
        </div>
        <NativeSelect
          aria-label="Cargo"
          className="lg:w-48"
          value={jobTitleId}
          onChange={(e) => {
            setJobTitleId(e.target.value);
            setPage(0);
          }}
        >
          <option value="">Todos os cargos</option>
          {(titles ?? []).map((t) => (
            <option key={t.id} value={t.id}>
              {t.title}
            </option>
          ))}
        </NativeSelect>
        <NativeSelect
          aria-label="Especialidade"
          className="lg:w-48"
          value={specialty}
          onChange={(e) => {
            setSpecialty(e.target.value);
            setPage(0);
          }}
        >
          <option value="">Todas as especialidades</option>
          {Object.entries(SPECIALTY_LABELS).map(([k, l]) => (
            <option key={k} value={k}>
              {l}
            </option>
          ))}
        </NativeSelect>
        <NativeSelect
          aria-label="Profissional"
          className="lg:w-44"
          value={professional}
          onChange={(e) => {
            setProfessional(e.target.value);
            setPage(0);
          }}
        >
          <option value="">Profissionais ou não</option>
          <option value="true">Só profissionais</option>
          <option value="false">Sem especialidade</option>
        </NativeSelect>
        <NativeSelect
          aria-label="Situação"
          className="lg:w-36"
          value={status}
          onChange={(e) => {
            setStatus(e.target.value);
            setPage(0);
          }}
        >
          <option value="">Todas</option>
          {Object.entries(EMPLOYEE_STATUS_LABELS).map(([k, l]) => (
            <option key={k} value={k}>
              {l}
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
        <ErrorCard message="Não foi possível carregar os funcionários." onRetry={() => refetch()} />
      ) : !data || data.items.length === 0 ? (
        <EmptyState
          icon={<Briefcase className="h-8 w-8" />}
          title="Nenhum funcionário"
          description="Cadastre a equipe e os profissionais de saúde."
        />
      ) : (
        <div className="space-y-4">
          <div className="overflow-x-auto rounded-lg border">
            <table className="w-full text-sm">
              <thead className="bg-muted/50 text-left text-xs uppercase tracking-wide text-muted-foreground">
                <tr>
                  <th className="px-4 py-3">Nome</th>
                  <th className="px-4 py-3">Matrícula</th>
                  <th className="px-4 py-3">Cargo</th>
                  <th className="px-4 py-3">Especialidades</th>
                  <th className="px-4 py-3">Conselho</th>
                  <th className="px-4 py-3">Usuário</th>
                  <th className="px-4 py-3">Admissão</th>
                  <th className="px-4 py-3">Situação</th>
                </tr>
              </thead>
              <tbody className={isFetching ? "opacity-60" : undefined}>
                {data.items.map((e) => (
                  <tr key={e.id} className="border-t hover:bg-muted/30">
                    <td className="px-4 py-3">
                      <Link
                        href={`${ROUTES.PEOPLE}/${e.id}`}
                        className="font-medium text-primary hover:underline"
                      >
                        {e.name}
                      </Link>
                    </td>
                    <td className="px-4 py-3">{e.registrationNumber ?? "—"}</td>
                    <td className="px-4 py-3">{e.jobTitle ?? "—"}</td>
                    <td className="px-4 py-3">
                      <div className="flex flex-wrap gap-1">
                        {e.specialties.length === 0
                          ? "—"
                          : e.specialties.map((s) => (
                              <Pill key={s} tone="blue">
                                {SPECIALTY_LABELS[s] ?? s}
                              </Pill>
                            ))}
                      </div>
                    </td>
                    <td className="px-4 py-3">{e.council ?? "—"}</td>
                    <td className="px-4 py-3">{e.userName ?? "—"}</td>
                    <td className="px-4 py-3">{formatDate(e.hireDate)}</td>
                    <td className="px-4 py-3">
                      <Pill
                        tone={
                          e.status === "ACTIVE"
                            ? "green"
                            : e.status === "ON_LEAVE"
                              ? "amber"
                              : "neutral"
                        }
                      >
                        {EMPLOYEE_STATUS_LABELS[e.status]}
                      </Pill>
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
      <JobTitlesDialog open={titlesOpen} onOpenChange={setTitlesOpen} />
    </div>
  );
}
