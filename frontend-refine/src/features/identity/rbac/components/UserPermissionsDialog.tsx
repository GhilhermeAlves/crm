"use client";

import {
  permissionLabel,
  permissionPrefixLabel,
} from "@/features/identity/rbac/utils/permissionLabels";
import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { RbacService } from "@/features/identity/rbac/services/rbac.service";
import type { Member } from "@/features/identity/members/types/member.types";
import {
  type UserPermissionEffect,
  type UserPermissionsResponse,
} from "@/features/identity/rbac/types/rbac.types";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { ScrollArea } from "@/components/ui/scroll-area";
import {
  groupPermissions,
  originOf,
  type PermissionOrigin,
} from "@/features/identity/rbac/utils/groupPermissions";

type UserPermissionsDialogProps = {
  member: Member | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
};

function OriginBadge({ origin }: { origin: PermissionOrigin }) {
  if (origin === "allow") {
    return <Badge className="bg-green-100 text-green-700">Adicionada</Badge>;
  }
  if (origin === "deny") {
    return <Badge className="bg-red-100 text-red-700">Removida</Badge>;
  }
  if (origin === "role") {
    return <Badge variant="secondary">Pelo perfil</Badge>;
  }
  return <Badge variant="outline">Sem acesso</Badge>;
}

/**
 * Sprint 20 (Fase 2): mostra perfis, permissões efetivas e overrides
 * individuais (ALLOW/DENY) de um usuário. INHERIT = sem override.
 */
export function UserPermissionsDialog({ member, open, onOpenChange }: UserPermissionsDialogProps) {
  const queryClient = useQueryClient();
  const [saving, setSaving] = useState<string | null>(null);
  const [search, setSearch] = useState("");

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

  const data = permissionsQuery.data;
  const fromRoles = new Set(data?.rolePermissionNames ?? []);
  const overridesByName = new Map(
    (data?.overrides ?? []).map((o: { permissionName: string; effect: string }) => [
      o.permissionName,
      o.effect,
    ]),
  );
  const groups = groupPermissions(allPermissionsQuery.data ?? []);
  const term = search.trim().toLocaleLowerCase("pt-BR");
  const matchesSearch = (permission: { name: string; description: string }) =>
    !term ||
    `${permission.name} ${permission.description} ${permissionLabel(permission)}`
      .toLocaleLowerCase("pt-BR")
      .includes(term);

  const applyEffect = async (permissionId: string, effect: UserPermissionEffect | null) => {
    setSaving(permissionId);
    try {
      if (effect === null) {
        await RbacService.removeUserPermissionOverride(member.userId, permissionId);
        toast.success("Permissão voltou a ser herdada do perfil");
      } else {
        await RbacService.setUserPermissionOverride(member.userId, permissionId, effect);
        toast.success(effect === "ALLOW" ? "Permissão adicionada" : "Permissão removida");
      }
      queryClient.invalidateQueries({ queryKey: ["user-permissions", member.userId] });
      queryClient.invalidateQueries({ queryKey: ["me"] });
    } catch (e) {
      toast.error(e instanceof Error ? e.message : "Erro ao salvar permissão");
    } finally {
      setSaving(null);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>Exceções de permissão — {member.name}</DialogTitle>
          {member.platformAdmin ? (
            <p className="text-sm text-muted-foreground">
              Dono da plataforma: já tem todas as permissões em qualquer empresa. Exceções aqui não
              mudam o acesso dele.
            </p>
          ) : member.hasProfile === false ? (
            <p className="text-sm text-destructive">
              Sem perfil nesta empresa. Atribua um perfil em “Alterar perfil” em vez de liberar
              permissões uma a uma.
            </p>
          ) : (
            <p className="text-sm text-muted-foreground">
              O acesso vem do perfil. Use exceções só para liberar ou bloquear algo pontual.
            </p>
          )}
          <DialogDescription>
            Perfis: {data?.roles.join(", ") || "nenhum"}. Overrides individuais têm precedência
            sobre os perfis (Removida vence Adicionada).
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-2 text-xs">
          <div className="flex flex-wrap gap-2">
            <Badge variant="secondary">Pelo perfil</Badge>
            <Badge className="bg-green-100 text-green-700">Adicionada</Badge>
            <Badge className="bg-red-100 text-red-700">Removida</Badge>
          </div>

          <Input
            aria-label="Buscar permissões"
            placeholder="Buscar por nome ou descrição"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
          />

          {permissionsQuery.isLoading && (
            <p className="py-4 text-center text-muted-foreground">Carregando...</p>
          )}

          <ScrollArea className="h-80 rounded-md border p-3">
            {Object.entries(groups).map(([module, permissions]) => {
              const visible = permissions.filter(matchesSearch);
              if (visible.length === 0) return null;

              return (
                <details key={module} open className="mb-2 rounded-md border border-border p-2">
                  <summary className="cursor-pointer text-sm font-medium">
                    <span>{permissionPrefixLabel(module)}</span>{" "}
                    <span className="text-muted-foreground">
                      {visible.length} de {permissions.length}
                    </span>
                  </summary>
                  <table className="mt-2 w-full text-sm">
                    <thead>
                      <tr className="text-left text-muted-foreground">
                        <th className="pb-2">Permissão</th>
                        <th className="pb-2">Origem</th>
                        <th className="pb-2 text-right">Ações</th>
                      </tr>
                    </thead>
                    <tbody>
                      {visible.map((permission) => {
                        const override = overridesByName.get(permission.name);
                        const origin = originOf(permission.name, fromRoles, overridesByName);
                        return (
                          <tr key={permission.id} className="border-t">
                            <td className="py-1.5">
                              <div className="font-mono text-xs">{permission.name}</div>
                              <div className="text-muted-foreground">
                                {permissionLabel(permission)}
                              </div>
                            </td>
                            <td className="py-1.5">
                              <OriginBadge origin={origin} />
                            </td>
                            <td className="py-1.5 text-right">
                              <div className="flex justify-end gap-1">
                                <Button
                                  size="sm"
                                  variant={override === "ALLOW" ? "default" : "outline"}
                                  disabled={saving === permission.id}
                                  onClick={() =>
                                    applyEffect(
                                      permission.id,
                                      override === "ALLOW" ? null : "ALLOW",
                                    )
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
                </details>
              );
            })}
          </ScrollArea>
        </div>
      </DialogContent>
    </Dialog>
  );
}
