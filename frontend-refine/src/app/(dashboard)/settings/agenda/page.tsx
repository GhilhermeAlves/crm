"use client";

import { useState, useMemo } from "react";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { useMembers } from "@/features/identity/members/hooks/useMembers";
import { PageTitle } from "@/components/common/PageTitle";
import {
  useAppointmentTypes,
  useCreateAppointmentType,
  useUpdateAppointmentType,
  useDeleteAppointmentType,
  useAvailability,
  useSetAvailability,
} from "@/features/sales/scheduling/hooks/useScheduling";
import { useSchedulingPermissions } from "@/features/sales/scheduling/schemas/scheduling.schema";
import { AppointmentTypeList } from "@/features/sales/scheduling/components/AppointmentTypeList";
import { AppointmentTypeFormDialog } from "@/features/sales/scheduling/components/AppointmentTypeFormDialog";
import { AvailabilityEditor } from "@/features/sales/scheduling/components/AvailabilityEditor";
import type { AppointmentType } from "@/features/sales/scheduling/types/scheduling.types";
import type { CreateAppointmentTypeFormValues } from "@/features/sales/scheduling/schemas/scheduling.schema";

export default function SettingsAgendaPage() {
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;
  const userId = user?.id ?? null;
  const perms = useSchedulingPermissions();

  const { data: types = [], isLoading: typesLoading } = useAppointmentTypes(companyId);
  const { data: members = [] } = useMembers(companyId);
  const { data: availability, isLoading: availLoading } = useAvailability(companyId, userId);

  const createType = useCreateAppointmentType(companyId);
  const updateType = useUpdateAppointmentType(companyId);
  const deleteType = useDeleteAppointmentType(companyId);
  const setAvailability = useSetAvailability(companyId);

  const [typeFormOpen, setTypeFormOpen] = useState(false);
  const [editingType, setEditingType] = useState<AppointmentType | null>(null);

  const memberOptions = useMemo(
    () => members.map((m) => ({ id: m.userId, name: m.name })),
    [members],
  );

  const handleCreateOrUpdate = (values: CreateAppointmentTypeFormValues) => {
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
      createType.mutate(values, {
        onSuccess: () => setTypeFormOpen(false),
      });
    }
  };

  const handleEdit = (type: AppointmentType) => {
    setEditingType(type);
    setTypeFormOpen(true);
  };

  const handleDelete = (type: AppointmentType) => {
    deleteType.mutate(type.id);
  };

  return (
    <div className="space-y-6">
      <PageTitle>Configurações da Agenda</PageTitle>

      <AppointmentTypeList
        types={types}
        isLoading={typesLoading}
        canConfigure={perms.canConfigure}
        onCreateClick={() => {
          setEditingType(null);
          setTypeFormOpen(true);
        }}
        onEditClick={handleEdit}
        onDeleteClick={handleDelete}
      />

      <AvailabilityEditor
        availability={availability}
        isLoading={availLoading}
        isSaving={setAvailability.isPending}
        onSave={(data) => {
          if (userId) setAvailability.mutate({ userId, data });
        }}
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
