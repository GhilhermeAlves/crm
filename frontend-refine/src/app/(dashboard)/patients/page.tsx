"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { useActiveCompanyModules } from "@/features/identity/modules/hooks/useCompanyModules";
import Link from "next/link";
import { HeartPulse, Loader2, Plus, Search, SearchX } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { EmptyState } from "@/components/common/EmptyState";
import { ErrorCard } from "@/components/common/ErrorCard";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { useContactPermissions } from "@/features/masterdata/contacts/hooks/useContacts";
import { usePatients } from "@/features/clinical/patients/hooks/usePatient";
import { PatientTable } from "@/features/clinical/patients/components/PatientTable";
import { ROUTES } from "@/lib/constants";
import { ListPagination } from "@/components/common/ListPagination";
import { DEFAULT_PAGE_SIZE } from "@/types/page";

export default function PatientsPage() {
  const router = useRouter();
  const { enabledModules } = useActiveCompanyModules();
  // A lista de pacientes passou para Pessoas → Clientes (doc 22.3); a ficha clínica continua aqui.
  useEffect(() => {
    if (enabledModules?.includes("PEOPLE")) router.replace(ROUTES.PEOPLE_CUSTOMERS);
  }, [enabledModules, router]);
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;
  const { canCreate, canUpdate } = useContactPermissions();

  const [draft, setDraft] = useState("");
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);

  const { data: patients, isLoading, isError, refetch } = usePatients(companyId, search);

  const total = patients?.length ?? 0;
  const pageCount = Math.max(1, Math.ceil(total / pageSize));
  const visible = useMemo(
    () => (patients ?? []).slice(page * pageSize, (page + 1) * pageSize),
    [patients, page, pageSize],
  );

  const applySearch = () => {
    setSearch(draft.trim());
    setPage(0);
  };

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold tracking-tight">
        Lista de Pacientes{patients ? ` (${total})` : ""}
      </h1>

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <form
          className="flex flex-1 items-center gap-2"
          onSubmit={(e) => {
            e.preventDefault();
            applySearch();
          }}
        >
          <div className="relative max-w-lg flex-1">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              value={draft}
              onChange={(e) => setDraft(e.target.value)}
              placeholder="Pesquise por nome, CPF, celular (com DDD) ou prontuário"
              className="pl-9"
              aria-label="Buscar paciente"
            />
          </div>
          <Button type="submit" variant="outline">
            Buscar
          </Button>
        </form>
        {canCreate && (
          <Button asChild>
            <Link href={`${ROUTES.PATIENTS}/new`}>
              <Plus className="mr-2 h-4 w-4" /> Novo paciente
            </Link>
          </Button>
        )}
      </div>

      {isLoading ? (
        <div className="flex items-center justify-center gap-2 py-20 text-muted-foreground">
          <Loader2 className="h-5 w-5 animate-spin" /> Carregando pacientes…
        </div>
      ) : isError ? (
        <ErrorCard message="Não foi possível carregar os pacientes." onRetry={() => refetch()} />
      ) : total === 0 ? (
        search ? (
          <EmptyState
            icon={<SearchX className="h-8 w-8" />}
            title="Nenhum resultado"
            description="Não encontramos pacientes para a pesquisa aplicada."
            action={
              <Button
                variant="outline"
                size="sm"
                onClick={() => {
                  setDraft("");
                  setSearch("");
                }}
              >
                Limpar busca
              </Button>
            }
          />
        ) : (
          <EmptyState
            icon={<HeartPulse className="h-8 w-8" />}
            title="Nenhum paciente cadastrado"
            description="Cadastre um paciente ou agende uma consulta para um contato."
          />
        )
      ) : (
        <div className="space-y-4">
          <PatientTable patients={visible} canUpdate={canUpdate} />
          <ListPagination
            page={page}
            pageSize={pageSize}
            totalPages={pageCount}
            totalElements={total}
            onPageChange={setPage}
            onPageSizeChange={(size) => {
              setPageSize(size);
              setPage(0);
            }}
          />
        </div>
      )}
    </div>
  );
}
