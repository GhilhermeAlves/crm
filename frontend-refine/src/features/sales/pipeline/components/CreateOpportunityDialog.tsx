"use client";

import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { CreateOpportunityForm } from "./CreateOpportunityForm";

interface CreateOpportunityDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSubmit: (values: {
    title: string;
    value: number;
    contactId: string;
    expectedCloseDate?: string;
    notes?: string;
  }) => void;
  isLoading?: boolean;
}

export function CreateOpportunityDialog({
  open,
  onOpenChange,
  onSubmit,
  isLoading,
}: CreateOpportunityDialogProps) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Nova Oportunidade</DialogTitle>
        </DialogHeader>
        <CreateOpportunityForm
          onSubmit={onSubmit}
          onCancel={() => onOpenChange(false)}
          isLoading={isLoading}
        />
      </DialogContent>
    </Dialog>
  );
}
