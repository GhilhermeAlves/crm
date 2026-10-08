"use client";

import { useState } from "react";

import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import {
  useAppointmentTypes,
  useCreateAppointmentType,
  useUpdateAppointmentType,
  useDeleteAppointmentType,
} from "@/features/sales/scheduling/hooks/useScheduling";
import { useSchedulingPermissions } from "@/features/sales/scheduling/schemas/scheduling.schema";
import { useMembers } from "@/features/identity/members/hooks/useMembers";
import { AppointmentTypeFormDialog } from "@/features/sales/scheduling/components/AppointmentTypeFormDialog";
import { AppointmentTypeList } from "@/features/sales/scheduling/components/AppointmentTypeList";
import type { AppointmentType } from "@/features/sales/scheduling/types/scheduling.types";
import type { CreateAppointmentTypeFormValues } from "@/features/sales/scheduling/schemas/scheduling.schema";

/** "Procedimento Simples" → "procedimento-simples" (sem acentos, até 80 caracteres). */
function slugify(name: string): string {
  return name
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .slice(0, 80);
}

export default function AgendaGeneralPage() {
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;
  const perms = useSchedulingPermissions();

  const { data: types = [], isLoading } = useAppointmentTypes(companyId);
  const { data: members = [] } = useMembers(companyId);
  const createType = useCreateAppointmentType(companyId);
  const updateType = useUpdateAppointmentType(companyId);
  const deleteType = useDeleteAppointmentType(companyId);

  const [typeFormOpen, setTypeFormOpen] = useState(false);
  const [editingType, setEditingType] = useState<AppointmentType | null>(null);

  const handleCreateOrUpdate = (formValues: CreateAppointmentTypeFormValues) => {
    // A API exige slug e o formulário não tem esse campo: gera a partir do nome.
    const values = {
      ...formValues,
      slug: formValues.slug?.trim() || editingType?.slug || slugify(formValues.name),
    };
    if (editingType) {
      updateType.mutate(
        { id: editingType.id, data: values },
        {
          onSuccess: () => {
            setTypeFormOpen(false);
            setEditingType(null);
          },
        },
      );
    } else {
      createType.mutate(values, { onSuccess: () => setTypeFormOpen(false) });
    }
  };

  const memberOptions = members.map((m) => ({ id: m.userId, name: m.name }));

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div>
        <h2 className="text-xl font-semibold">Ajustes gerais</h2>
        <p className="text-sm text-muted-foreground">
          Cadastre os tipos de consulta, com duração e profissionais responsáveis. Os horários
          livres são calculados a partir deles e da disponibilidade de cada profissional.
        </p>
      </div>

      <AppointmentTypeList
        types={types}
        isLoading={isLoading}
        canConfigure={perms.canConfigure}
        onCreateClick={() => {
          setEditingType(null);
          setTypeFormOpen(true);
        }}
        onEditClick={(type) => {
          setEditingType(type);
          setTypeFormOpen(true);
        }}
        onDeleteClick={(type) => deleteType.mutate(type.id)}
      />

      <AppointmentTypeFormDialog
        open={typeFormOpen}
        onOpenChange={(open) => {
          setTypeFormOpen(open);
          if (!open) setEditingType(null);
        }}
        isLoading={createType.isPending || updateType.isPending}
        onSubmit={handleCreateOrUpdate}
        members={memberOptions}
        editingType={editingType}
      />
    </div>
  );
}
