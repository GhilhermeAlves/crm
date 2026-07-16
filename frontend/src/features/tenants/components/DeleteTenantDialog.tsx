"use client";

import { useState } from "react";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { useDeleteTenant } from "../hooks/useTenants";

type DeleteTenantDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  tenantId: string;
  tenantName: string;
};

export function DeleteTenantDialog({
  open,
  onOpenChange,
  tenantId,
  tenantName,
}: DeleteTenantDialogProps) {
  const deleteMutation = useDeleteTenant();

  const handleConfirm = () => {
    deleteMutation.mutate(tenantId, {
      onSuccess: () => {
        onOpenChange(false);
      },
    });
  };

  return (
    <ConfirmDialog
      open={open}
      onOpenChange={onOpenChange}
      title="Excluir empresa"
      description={`Tem certeza que deseja excluir "${tenantName}"? Esta ação não pode ser desfeita.`}
      confirmLabel="Excluir"
      cancelLabel="Cancelar"
      variant="destructive"
      onConfirm={handleConfirm}
      isLoading={deleteMutation.isPending}
    />
  );
}
