"use client";

import { useRouter } from "next/navigation";
import { toast } from "sonner";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { useDeleteRole } from "../hooks/useRoles";
import type { Role } from "../types/rbac.types";

interface DeleteRoleDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  role: Role;
}

export function DeleteRoleDialog({ open, onOpenChange, role }: DeleteRoleDialogProps) {
  const router = useRouter();
  const deleteRole = useDeleteRole();

  const handleDelete = () => {
    deleteRole.mutate(role.id, {
      onSuccess: () => {
        onOpenChange(false);
        router.push("/roles");
      },
    });
  };

  return (
    <AlertDialog open={open} onOpenChange={onOpenChange}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Excluir Role</AlertDialogTitle>
          <AlertDialogDescription>
            Tem certeza que deseja excluir a role{" "}
            <strong>{role.name.replace(/_/g, " ")}</strong>?
            {role.isSystem && (
              <span className="block mt-2 text-destructive font-medium">
                Esta é uma role do sistema e não pode ser excluída.
              </span>
            )}
            {!role.isSystem && (
              <span className="block mt-2">
                Esta ação não pode ser desfeita. Todas as permissões associadas serão removidas.
              </span>
            )}
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>Cancelar</AlertDialogCancel>
          {!role.isSystem && (
            <AlertDialogAction
              onClick={handleDelete}
              disabled={deleteRole.isPending}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            >
              {deleteRole.isPending ? "Excluindo..." : "Excluir"}
            </AlertDialogAction>
          )}
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
