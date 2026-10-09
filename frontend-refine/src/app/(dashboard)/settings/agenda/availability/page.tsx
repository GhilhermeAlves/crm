"use client";

import { useMemo, useState } from "react";
import { Pencil } from "lucide-react";

import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { useMembers } from "@/features/identity/members/hooks/useMembers";
import {
  useAppointmentTypes,
  useAvailability,
  useSetAvailability,
  useSetProfessionalScheduling,
} from "@/features/sales/scheduling/hooks/useScheduling";
import { AvailabilityEditor } from "@/features/sales/scheduling/components/AvailabilityEditor";
import { Button } from "@/components/ui/button";
import { Switch } from "@/components/ui/switch";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";

export default function AvailabilityPage() {
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;
  const { data: members = [], isLoading } = useMembers(companyId);
  const { data: types = [], isLoading: typesLoading } = useAppointmentTypes(companyId);
  const setProfessional = useSetProfessionalScheduling(companyId);

  // Responsáveis em algum tipo de agendamento = profissionais que recebem agendamentos.
  const hostIds = useMemo(() => new Set(types.flatMap((t) => t.hostIds)), [types]);

  // O OWNER é quem criou a empresa (ex.: o dono da conta que a administra) e não
  // atende — só aparece como profissional se já for responsável em algum tipo.
  const professionals = useMemo(
    () =>
      members
        .filter((m) => m.status === "ACTIVE")
        .filter((m) => m.role !== "OWNER" || hostIds.has(m.userId))
        .map((m) => ({ ...m, enabled: hostIds.has(m.userId) })),
    [members, hostIds],
  );

  const [selectedId, setSelectedId] = useState<string | null>(null);
  const selected =
    professionals.find((p) => p.userId === selectedId) ??
    professionals.find((p) => p.userId === user?.id) ??
    professionals[0] ??
    null;
  const selectedUserId = selected?.userId ?? null;

  const { data: availability, isLoading: availLoading } = useAvailability(
    companyId,
    selectedUserId,
  );
  const setAvailability = useSetAvailability(companyId);

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div>
        <h2 className="text-xl font-semibold">Disponibilidade do link de agendamento</h2>
        <p className="text-sm text-muted-foreground">
          Ative os profissionais que recebem agendamentos (inclusive os marcados pelo agente) e
          defina os horários de atendimento de cada um.
        </p>
      </div>

      <section className="rounded-lg border bg-card">
        {isLoading || typesLoading ? (
          <p className="py-8 text-center text-sm text-muted-foreground">Carregando...</p>
        ) : professionals.length === 0 ? (
          <p className="py-8 text-center text-sm text-muted-foreground">
            Nenhum profissional encontrado.
          </p>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="text-xs">Profissional</TableHead>
                <TableHead className="text-xs">Agenda</TableHead>
                <TableHead className="text-right text-xs">Horários</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {professionals.map((m) => (
                <TableRow
                  key={m.userId}
                  data-state={m.userId === selectedUserId ? "selected" : undefined}
                >
                  <TableCell>
                    <div className="flex items-center gap-2">
                      <div className="flex h-7 w-7 items-center justify-center rounded-full bg-primary/10 text-xs font-medium text-primary">
                        {m.name.charAt(0).toUpperCase()}
                      </div>
                      <span className="text-sm">{m.name}</span>
                    </div>
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center gap-2">
                      <Switch
                        checked={m.enabled}
                        disabled={types.length === 0 || setProfessional.isPending}
                        aria-label={`Agenda de ${m.name}`}
                        onCheckedChange={(enabled) =>
                          setProfessional.mutate({ userId: m.userId, enabled, types })
                        }
                      />
                      <span className="text-sm text-muted-foreground">
                        {m.enabled ? "Ativada" : "Desativada"}
                      </span>
                    </div>
                  </TableCell>
                  <TableCell className="text-right">
                    <Button
                      variant="ghost"
                      size="icon"
                      className="h-7 w-7"
                      aria-label={`Editar horários de ${m.name}`}
                      onClick={() => setSelectedId(m.userId)}
                    >
                      <Pencil className="h-3.5 w-3.5" />
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
        {!typesLoading && types.length === 0 && (
          <p className="border-t px-4 py-3 text-sm text-muted-foreground">
            Cadastre um tipo de consulta em Ajustes gerais para ativar profissionais.
          </p>
        )}
      </section>

      {selectedUserId && (
        <section className="space-y-4">
          <AvailabilityEditor
            title={
              selectedUserId === user?.id
                ? "Minha disponibilidade"
                : `Disponibilidade de ${selected?.name}`
            }
            availability={availability}
            isLoading={availLoading}
            isSaving={setAvailability.isPending}
            onSave={(data) => setAvailability.mutate({ userId: selectedUserId, data })}
          />
        </section>
      )}
    </div>
  );
}
