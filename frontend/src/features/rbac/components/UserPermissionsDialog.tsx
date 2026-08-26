"use client";

import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { RbacService } from "@/features/rbac/services/rbac.service";
import type { Member } from "@/features/members/types/member.types";
import {
  type UserPermissionEffect,
  type UserPermissionsResponse,
} from "@/features/rbac/types/rbac.types";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { ScrollArea } from "@/components/ui/scroll-area";

type UserPermissionsDialogProps = {
  member: Member | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
};

/**
 * Sprint 20 (Fase 2): mostra perfis, permissões efetivas e overrides
 * individuais (ALLOW/DENY) de um usuário. INHERIT = sem override.
 */
export function UserPermissionsDialog({ member, open, onOpenChange }: UserPermissionsDialogProps) {
  const queryClient = useQueryClient();
  const [saving, setSaving] = useState<string | null>(null);

  const permissionsQuery = useQuery({
    queryKey: ["user-permissions", member?.userId],
    queryFn: () => RbacService.getUserPermissions(member!.userId),
    enabled: open && !!member,
  });

  const allPermissionsQuery = useQuery({
    queryKey: ["permissions", "all"],
    queryFn: () => RbacService.listAllPermissions(),
    enabled: open,
  });

  if (!member) return null;

  const data = permissionsQuery.data as UserPermissionsResponse | undefined;
  const overridesByName = new Map(
    (data?.overrides ?? []).map((o: { permissionName: string; effect: string }) => [
      o.permissionName,
      o.effect,
    ]),
  );

  const applyEffect = async (permissionId: string, effect: UserPermissionEffect | null) => {
    setSaving(permissionId);
    try {
      if (effect === null) {
        await RbacService.removeUserPermissionOverride(member.userId, permissionId);
        toast.success("Permissão voltou para HERDADA");
      } else {
        await RbacService.setUserPermissionOverride(member.userId, permissionId, effect);
        toast.success(`Override ${effect} aplicado`);
      }
      queryClient.invalidateQueries({ queryKey: ["user-permissions", member.userId] });
    } catch (e) {
      toast.error(e instanceof Error ? e.message : "Erro ao salvar override");
    } finally {
      setSaving(null);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>Permissões — {member.name}</DialogTitle>
          <DialogDescription>
            Perfis: {data?.roles.join(", ") || "nenhum"}. Overrides individuais têm precedência
            sobre os perfis (DENY vence ALLOW).
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-2 text-xs">
          <div className="flex flex-wrap gap-2">
            <Badge variant="secondary">Herdada</Badge>
            <Badge className="bg-green-100 text-green-700">ALLOW individual</Badge>
            <Badge className="bg-red-100 text-red-700">DENY individual</Badge>
          </div>

          {permissionsQuery.isLoading && (
            <p className="py-4 text-center text-muted-foreground">Carregando...</p>
          )}

          <ScrollArea className="h-80 rounded-md border p-3">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-muted-foreground">
                  <th className="pb-2">Permissão</th>
                  <th className="pb-2">Estado</th>
                  <th className="pb-2 text-right">Ações</th>
                </tr>
              </thead>
              <tbody>
                {(allPermissionsQuery.data ?? []).map((permission) => {
                  const isEffective = data?.effective.includes(permission.name) ?? false;
                  const override = overridesByName.get(permission.name);
                  return (
                    <tr key={permission.id} className="border-t">
                      <td className="py-1.5 font-mono text-xs">{permission.name}</td>
                      <td className="py-1.5">
                        {override === "ALLOW" ? (
                          <Badge className="bg-green-100 text-green-700">ALLOW</Badge>
                        ) : override === "DENY" ? (
                          <Badge className="bg-red-100 text-red-700">DENY</Badge>
                        ) : isEffective ? (
                          <Badge variant="secondary">Herdada ✓</Badge>
                        ) : (
                          <Badge variant="outline">Sem acesso</Badge>
                        )}
                      </td>
                      <td className="py-1.5 text-right">
                        <div className="flex justify-end gap-1">
                          <Button
                            size="sm"
                            variant={override === "ALLOW" ? "default" : "outline"}
                            disabled={saving === permission.id}
                            onClick={() =>
                              applyEffect(permission.id, override === "ALLOW" ? null : "ALLOW")
                            }
                          >
                            Permitir
                          </Button>
                          <Button
                            size="sm"
                            variant={override === "DENY" ? "destructive" : "outline"}
                            disabled={saving === permission.id}
                            onClick={() =>
                              applyEffect(permission.id, override === "DENY" ? null : "DENY")
                            }
                          >
                            Negar
                          </Button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </ScrollArea>
        </div>
      </DialogContent>
    </Dialog>
  );
}
