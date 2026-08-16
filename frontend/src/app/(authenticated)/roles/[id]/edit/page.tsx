"use client";

import { use, useState, useCallback, useMemo } from "react";
import { useRouter } from "next/navigation";
import { ArrowLeft, Save } from "lucide-react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Switch } from "@/components/ui/switch";
import { Separator } from "@/components/ui/separator";
import { useRole, useUpdateRole } from "@/features/rbac/hooks/useRoles";
import { usePermissions } from "@/features/rbac/hooks/useRoles";
import { PermissionMatrix } from "@/features/rbac/components/PermissionMatrix";
import { RoleBadge } from "@/features/rbac/components/RoleBadge";

export default function EditRolePage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const router = useRouter();
  const { data: role, isLoading: roleLoading } = useRole(id);
  const { data: allPermissions } = usePermissions();
  const updateRole = useUpdateRole();

  const [description, setDescription] = useState("");
  const [isActive, setIsActive] = useState(true);
  const [selectedPermissionIds, setSelectedPermissionIds] = useState<string[]>([]);

  const initialized = useMemo(() => {
    if (role) {
      setDescription(role.description || "");
      setIsActive(role.isActive);
      setSelectedPermissionIds(role.permissions.map((p) => p.id));
      return true;
    }
    return false;
  }, [role]);

  const handleTogglePermission = useCallback((permissionId: string) => {
    setSelectedPermissionIds((prev) =>
      prev.includes(permissionId)
        ? prev.filter((id) => id !== permissionId)
        : [...prev, permissionId],
    );
  }, []);

  const handleSubmit = () => {
    updateRole.mutate(
      {
        id,
        data: {
          description,
          isActive,
          permissionIds: selectedPermissionIds,
        },
      },
      {
        onSuccess: () => {
          router.push(`/roles/${id}`);
        },
      },
    );
  };

  if (roleLoading || !initialized) {
    return (
      <div className="flex h-32 items-center justify-center">
        <p className="text-muted-foreground">Carregando role...</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Button variant="ghost" size="icon" asChild>
            <Link href={`/roles/${id}`}>
              <ArrowLeft className="h-4 w-4" />
            </Link>
          </Button>
          <div>
            <h1 className="text-2xl font-bold">Editar Role: {role!.name.replace(/_/g, " ")}</h1>
            <p className="text-muted-foreground">
              <RoleBadge name={role!.name} isSystem={role!.isSystem} />
            </p>
          </div>
        </div>
        <Button onClick={handleSubmit} disabled={updateRole.isPending}>
          <Save className="mr-2 h-4 w-4" />
          {updateRole.isPending ? "Salvando..." : "Salvar Alterações"}
        </Button>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="space-y-4 lg:col-span-1">
          <Card>
            <CardHeader>
              <CardTitle>Informações</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="description">Descrição</Label>
                <Textarea
                  id="description"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="Descreva o propósito desta role..."
                  rows={4}
                />
              </div>
              <Separator />
              <div className="flex items-center justify-between">
                <Label htmlFor="isActive">Ativo</Label>
                <Switch id="isActive" checked={isActive} onCheckedChange={setIsActive} />
              </div>
            </CardContent>
          </Card>
        </div>

        <div className="lg:col-span-2">
          {allPermissions && (
            <PermissionMatrix
              permissions={allPermissions}
              selectedPermissionIds={selectedPermissionIds}
              onToggle={handleTogglePermission}
            />
          )}
        </div>
      </div>
    </div>
  );
}
