"use client";

import { useEffect, useMemo, useState } from "react";
import { MoreHorizontal, ShieldAlert, UserMinus, UserRoundCog } from "lucide-react";

import { usePermission } from "@/features/identity/auth/hooks/useAuthorization";
import { useRoles } from "@/features/identity/rbac/hooks/useRoles";
import { useRemoveMember, useUpdateMemberRole } from "@/features/identity/members/hooks/useMembers";
import { isAdminLevelRole, roleLabel } from "@/features/identity/members/lib/labels";
import { computeMembershipLimits } from "@/features/identity/members/lib/membership-limits";
import type { Member } from "@/features/identity/members/types/member.types";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

/** Catálogo fixo usado apenas enquanto /roles ainda não respondeu. */
const FALLBACK_ROLES = ["SUPER_ADMIN", "ADMIN", "MANAGER", "AGENT", "VIEWER"];

type MemberRowActionsProps = {
  companyId: string;
  member: Member;
  /** Membros ATIVOS: base dos guardas de último ADMIN e último membro. */
  activeMembers: Member[];
};

export function MemberRowActions({ companyId, member, activeMembers }: MemberRowActionsProps) {
  const can = usePermission();
  const canUpdate = can("user:update");
  const canDelete = can("user:delete");

  const [roleOpen, setRoleOpen] = useState(false);
  const [confirmOpen, setConfirmOpen] = useState(false);

  const updateRole = useUpdateMemberRole(companyId);
  const removeMember = useRemoveMember(companyId);

  // Espelha MembershipService.assertNotLastAdmin/assertNotLastMember.
  const { isLastAdmin, isLastMember } = computeMembershipLimits(member.role, activeMembers);
  const cannotDeactivate = isLastAdmin || isLastMember;
  const deactivateBlockReason = isLastAdmin
    ? "Este é o único administrador ativo da empresa."
    : "Este é o último membro ativo da empresa.";

  if (!canUpdate && !canDelete) return null;

  return (
    <>
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button
            variant="ghost"
            size="icon"
            className="h-7 w-7"
            aria-label={`Ações de ${member.name}`}
          >
            <MoreHorizontal className="h-4 w-4" />
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end">
          <DropdownMenuLabel className="text-xs text-muted-foreground">
            {member.email}
          </DropdownMenuLabel>
          <DropdownMenuSeparator />
          {canUpdate && (
            <DropdownMenuItem onSelect={() => setRoleOpen(true)}>
              <UserRoundCog />
              Alterar papel
            </DropdownMenuItem>
          )}
          {canDelete && (
            <DropdownMenuItem
              onSelect={() => setConfirmOpen(true)}
              disabled={cannotDeactivate}
              title={cannotDeactivate ? deactivateBlockReason : undefined}
              className="text-destructive focus:text-destructive"
            >
              {isLastAdmin ? <ShieldAlert /> : <UserMinus />}
              Desativar membro
            </DropdownMenuItem>
          )}
        </DropdownMenuContent>
      </DropdownMenu>

      {canUpdate && (
        <ChangeRoleDialog
          open={roleOpen}
          onOpenChange={setRoleOpen}
          member={member}
          isLastAdmin={isLastAdmin}
          onSave={(role) =>
            updateRole.mutate(
              { userId: member.userId, role },
              { onSuccess: () => setRoleOpen(false) },
            )
          }
          isPending={updateRole.isPending}
        />
      )}

      <AlertDialog open={confirmOpen} onOpenChange={setConfirmOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Desativar membro</AlertDialogTitle>
            <AlertDialogDescription>
              O acesso de <strong>{member.name}</strong> ({member.email}) à empresa será encerrado e
              ele será movido para a aba Inativos. As permissões de acesso são revogadas.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancelar</AlertDialogCancel>
            <AlertDialogAction
              onClick={(event) => {
                event.preventDefault();
                removeMember.mutate(member.userId, { onSuccess: () => setConfirmOpen(false) });
              }}
              disabled={removeMember.isPending}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            >
              {removeMember.isPending ? "Desativando..." : "Desativar"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  );
}

type ChangeRoleDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  member: Member;
  isLastAdmin: boolean;
  isPending: boolean;
  onSave: (role: string) => void;
};

function ChangeRoleDialog({
  open,
  onOpenChange,
  member,
  isLastAdmin,
  isPending,
  onSave,
}: ChangeRoleDialogProps) {
  const { data: roles = [] } = useRoles();
  const [selected, setSelected] = useState(member.role);

  // Re-sincroniza ao abrir e após o papel ter sido salvo.
  useEffect(() => {
    if (open) setSelected(member.role);
  }, [open, member.role]);

  const availableRoles = useMemo(() => {
    const base =
      roles.length > 0
        ? roles.filter((role) => role.isActive).map((role) => role.name)
        : [...FALLBACK_ROLES];
    if (member.role && !base.includes(member.role)) base.push(member.role);
    // Último ADMIN só pode continuar em papéis de nível administrador.
    return isLastAdmin ? base.filter(isAdminLevelRole) : base;
  }, [roles, member.role, isLastAdmin]);

  const options = availableRoles.includes(selected)
    ? availableRoles
    : [selected, ...availableRoles];

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Alterar papel</DialogTitle>
          <DialogDescription>
            Defina o papel de <strong>{member.name}</strong> nesta empresa. A alteração vale para os
            próximos acessos e é aplicada imediatamente.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-3">
          <Select value={selected} onValueChange={setSelected}>
            <SelectTrigger aria-label="Papel">
              <SelectValue placeholder="Selecione o papel" />
            </SelectTrigger>
            <SelectContent>
              {options.map((role) => (
                <SelectItem key={role} value={role}>
                  {roleLabel(role)}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>

          {isLastAdmin && (
            <p className="flex items-start gap-1.5 text-xs text-muted-foreground">
              <ShieldAlert className="mt-0.5 h-3.5 w-3.5 shrink-0 text-destructive" />
              Este é o único administrador ativo da empresa: apenas papéis administrativos estão
              disponíveis.
            </p>
          )}
        </div>

        <DialogFooter>
          <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
            Cancelar
          </Button>
          <Button
            type="button"
            disabled={isPending || selected === member.role}
            onClick={() => onSave(selected)}
          >
            {isPending ? "Salvando..." : "Salvar"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
