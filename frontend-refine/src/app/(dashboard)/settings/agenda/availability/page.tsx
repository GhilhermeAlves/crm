"use client";

import { useMemo } from "react";
import { Pencil } from "lucide-react";

import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { useMembers } from "@/features/identity/members/hooks/useMembers";
import {
  useAvailability,
  useSetAvailability,
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
  const userId = user?.id ?? null;
  const { data: members = [], isLoading } = useMembers(companyId);
  const { data: availability, isLoading: availLoading } = useAvailability(companyId, userId);
  const setAvailability = useSetAvailability(companyId);

  const memberList = useMemo(() => members.map((m) => ({ ...m, enabled: false })), [members]);

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <h2 className="text-xl font-semibold">Disponibilidade do link de agendamento</h2>

      <section className="rounded-lg border bg-card">
        {isLoading ? (
          <p className="py-8 text-center text-sm text-muted-foreground">Carregando...</p>
        ) : memberList.length === 0 ? (
          <p className="py-8 text-center text-sm text-muted-foreground">
            Nenhum profissional encontrado.
          </p>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="text-xs">Profissional</TableHead>
                <TableHead className="text-xs">Agenda</TableHead>
                <TableHead className="text-right text-xs">Ação</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {memberList.map((m) => (
                <TableRow key={m.userId}>
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
                      <Switch checked={m.enabled} />
                      <span className="text-sm text-muted-foreground">
                        {m.enabled ? "Ativada" : "Desativada"}
                      </span>
                    </div>
                  </TableCell>
                  <TableCell className="text-right">
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

      <section className="space-y-4">
        <h3 className="text-lg font-semibold">Minha disponibilidade</h3>
        <AvailabilityEditor
          availability={availability}
          isLoading={availLoading}
          isSaving={setAvailability.isPending}
          onSave={(data) => {
            if (userId) setAvailability.mutate({ userId, data });
          }}
        />
      </section>
    </div>
  );
}
