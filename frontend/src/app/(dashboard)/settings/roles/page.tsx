"use client";

import { useMemo, useState } from "react";
import { useAuthorization } from "@/features/auth/hooks/useAuthorization";
import {
  useRoles,
  usePermissions,
  useAssignPermission,
  useRemovePermission,
  useCreateRole,
} from "@/features/rbac/hooks/useRoles";
import { RoleBadge } from "@/features/rbac/components/RoleBadge";
import { PermissionMatrix } from "@/features/rbac/components/PermissionMatrix";
import { PageTitle } from "@/components/common/PageTitle";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog";
import { cn } from "@/lib/utils";

/**
 * Sprint 9 — Perfis de acesso & permissões da EMPRESA ATIVA.
 *
 * Visualiza o desenho dos papéis (ADMIN, MANAGER, AGENT, VIEWER, ...) e, para
 * quem possui `role:manage`, permite atribuir/remover permissões. Não cria
 * roles duplicadas — reutiliza o RBAC existente e preserva as roles canônicas.
 */
export default function SettingsRolesPage() {
  const { can } = useAuthorization();

  const rolesQuery = useRoles();
  const permissionsQuery = usePermissions();
  const assignPermission = useAssignPermission();
  const removePermission = useRemovePermission();
  const createRole = useCreateRole();

  const [selectedRoleId, setSelectedRoleId] = useState<string | null>(null);
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [newRoleName, setNewRoleName] = useState("");
  const [newRoleDescription, setNewRoleDescription] = useState("");

  const roles = rolesQuery.data ?? [];
  const activeRole = roles.find((r) => r.id === selectedRoleId) ?? roles[0] ?? null;

  const managePermissions = can("role:manage");

  const activePermissionIds = useMemo(
    () => activeRole?.permissions.map((p) => p.id) ?? [],
    [activeRole],
  );

  function handleToggle(permissionId: string) {
    if (!activeRole) return;
    if (activePermissionIds.includes(permissionId)) {
      removePermission.mutate({ roleId: activeRole.id, permissionId });
    } else {
      assignPermission.mutate({
        roleId: activeRole.id,
        data: { permissionId },
      });
    }
  }

  function handleAssignAllUnselected() {
    if (!activeRole) return;
    const missing = (permissionsQuery.data ?? []).filter(
      (p) => !activePermissionIds.includes(p.id),
    );
    for (const permission of missing) {
      assignPermission.mutate({
        roleId: activeRole.id,
        data: { permissionId: permission.id },
      });
    }
  }

  function handleCreateRole() {
    if (!newRoleName.trim()) return;
    createRole.mutate(
      {
        name: newRoleName.trim(),
        description: newRoleDescription.trim() || undefined,
      },
      {
        onSuccess: (created) => {
          setCreateDialogOpen(false);
          setNewRoleName("");
          setNewRoleDescription("");
          setSelectedRoleId(created.id);
        },
      },
    );
  }

  if (rolesQuery.isLoading) return <Skeleton className="h-64 w-full" />;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <PageTitle>Perfis de acesso</PageTitle>
          <p className="text-sm text-muted-foreground">
            Papéis e permissões desta empresa. A empresa ativa determina o que é exibido.
          </p>
        </div>
        <div className="flex items-center gap-2">
          {managePermissions && (
            <Button variant="default" size="sm" onClick={() => setCreateDialogOpen(true)}>
              Criar Perfil
            </Button>
          )}
          {managePermissions && (
            <Button
              variant="outline"
              size="sm"
              onClick={handleAssignAllUnselected}
              disabled={assignPermission.isPending}
            >
              Atribuir permissões em falta
            </Button>
          )}
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-[280px_1fr]">
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-base">Papéis</CardTitle>
          </CardHeader>
          <CardContent>
            <ScrollArea className="h-[calc(100vh-16rem)]">
              <div className="space-y-1">
                {roles.map((role) => (
                  <button
                    key={role.id}
                    onClick={() => setSelectedRoleId(role.id)}
                    className={cn(
                      "flex w-full items-center justify-between rounded-md px-3 py-2 text-left text-sm transition-colors",
                      activeRole?.id === role.id ? "bg-primary/10 text-primary" : "hover:bg-muted",
                    )}
                  >
                    <RoleBadge name={role.name} isSystem={role.isSystem} />
                    <span className="text-xs text-muted-foreground">{role.permissions.length}</span>
                  </button>
                ))}
              </div>
            </ScrollArea>
          </CardContent>
        </Card>

        <div className="space-y-4">
          {activeRole ? (
            <>
              <div>
                <h2 className="text-2xl font-bold">{activeRole.name.replace(/_/g, " ")}</h2>
                <p className="text-muted-foreground">{activeRole.description || "Sem descrição"}</p>
              </div>
              <PermissionMatrix
                permissions={permissionsQuery.data ?? []}
                selectedPermissionIds={activePermissionIds}
                onToggle={handleToggle}
                readOnly={!managePermissions}
              />
            </>
          ) : (
            <p className="text-muted-foreground">Nenhum papel disponível para esta empresa.</p>
          )}
        </div>
      </div>

      <Dialog open={createDialogOpen} onOpenChange={setCreateDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Criar Novo Perfil</DialogTitle>
            <DialogDescription>
              Crie um perfil customizado e atribua permissões específicas.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="role-name">Nome</Label>
              <Input
                id="role-name"
                placeholder="Ex: VENDEDOR"
                value={newRoleName}
                onChange={(e) => setNewRoleName(e.target.value.toUpperCase())}
                maxLength={50}
              />
              <p className="text-xs text-muted-foreground">
                Apenas letras maiúsculas e underscores. Máximo 50 caracteres.
              </p>
            </div>
            <div className="space-y-2">
              <Label htmlFor="role-description">Descrição (opcional)</Label>
              <Input
                id="role-description"
                placeholder="Descrição do perfil"
                value={newRoleDescription}
                onChange={(e) => setNewRoleDescription(e.target.value)}
                maxLength={500}
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setCreateDialogOpen(false)}>
              Cancelar
            </Button>
            <Button
              onClick={handleCreateRole}
              disabled={!newRoleName.trim() || createRole.isPending}
            >
              {createRole.isPending ? "Criando..." : "Criar"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
