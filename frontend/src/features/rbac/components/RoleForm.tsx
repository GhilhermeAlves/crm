"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useCreateRole, usePermissions } from "../hooks/useRoles";
import { roleSchema, type RoleFormData } from "../schemas/role.schema";
import { PermissionMatrix } from "./PermissionMatrix";

interface RoleFormProps {
  defaultValues?: Partial<RoleFormData>;
  mode?: "create" | "edit";
}

export function RoleForm({ defaultValues, mode = "create" }: RoleFormProps) {
  const router = useRouter();
  const createRole = useCreateRole();
  const { data: allPermissions } = usePermissions();
  const [selectedPermissionIds, setSelectedPermissionIds] = useState<string[]>(
    defaultValues?.permissionIds ?? []
  );

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<RoleFormData>({
    resolver: zodResolver(roleSchema),
    defaultValues: {
      name: "",
      description: "",
      permissionIds: [],
      ...defaultValues,
    },
  });

  const handleTogglePermission = (permissionId: string) => {
    setSelectedPermissionIds((prev) =>
      prev.includes(permissionId)
        ? prev.filter((id) => id !== permissionId)
        : [...prev, permissionId]
    );
  };

  const onSubmit = async (data: RoleFormData) => {
    createRole.mutate(
      { ...data, permissionIds: selectedPermissionIds },
      {
        onSuccess: () => {
          router.push("/roles");
        },
      }
    );
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>{mode === "create" ? "Nova Role" : "Editar Role"}</CardTitle>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="name">Nome *</Label>
            <Input
              id="name"
              placeholder="Ex: MANAGER"
              {...register("name")}
              disabled={mode === "edit"}
            />
            {errors.name && (
              <p className="text-sm text-destructive">{errors.name.message}</p>
            )}
            <p className="text-xs text-muted-foreground">
              Apenas letras maiúsculas e underscores. Ex: MANAGER, TEAM_LEAD
            </p>
          </div>

          <div className="space-y-2">
            <Label htmlFor="description">Descrição</Label>
            <Textarea
              id="description"
              placeholder="Descreva o propósito desta role..."
              {...register("description")}
              rows={3}
            />
            {errors.description && (
              <p className="text-sm text-destructive">{errors.description.message}</p>
            )}
          </div>

          <div className="space-y-2">
            {allPermissions && (
              <PermissionMatrix
                permissions={allPermissions}
                selectedPermissionIds={selectedPermissionIds}
                onToggle={handleTogglePermission}
              />
            )}
          </div>

          <div className="flex gap-2 justify-end">
            <Button
              type="button"
              variant="outline"
              onClick={() => router.push("/roles")}
            >
              Cancelar
            </Button>
            <Button type="submit" disabled={createRole.isPending}>
              {createRole.isPending ? "Salvando..." : "Salvar"}
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
}
