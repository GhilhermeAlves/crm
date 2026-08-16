"use client";

import Link from "next/link";
import { ArrowLeft, Pencil, Shield, ShieldOff } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { RoleBadge } from "./RoleBadge";
import { PermissionBadge } from "./PermissionBadge";
import type { Role } from "../types/rbac.types";
import { roleModuleName } from "../schemas/role.schema";

interface RoleDetailsProps {
  role: Role;
}

export function RoleDetails({ role }: RoleDetailsProps) {
  const permissionsByModule = role.permissions.reduce<Record<string, typeof role.permissions>>(
    (acc, perm) => {
      if (!acc[perm.module]) acc[perm.module] = [];
      acc[perm.module].push(perm);
      return acc;
    },
    {},
  );

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Button variant="ghost" size="icon" asChild>
            <Link href="/roles">
              <ArrowLeft className="h-4 w-4" />
            </Link>
          </Button>
          <div>
            <h2 className="text-2xl font-bold">{role.name.replace(/_/g, " ")}</h2>
            <p className="text-muted-foreground">{role.description || "Sem descrição"}</p>
          </div>
        </div>
        {!role.isSystem && (
          <Button asChild>
            <Link href={`/roles/${role.id}/edit`}>
              <Pencil className="mr-2 h-4 w-4" />
              Editar
            </Link>
          </Button>
        )}
      </div>

      <div className="grid gap-4 md:grid-cols-3">
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">Status</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex items-center gap-2">
              {role.isActive ? (
                <Shield className="h-5 w-5 text-green-600" />
              ) : (
                <ShieldOff className="h-5 w-5 text-gray-500" />
              )}
              <span className="text-lg font-semibold">{role.isActive ? "Ativo" : "Inativo"}</span>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">Tipo</CardTitle>
          </CardHeader>
          <CardContent>
            <RoleBadge name={role.name} isSystem={role.isSystem} />
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">Permissões</CardTitle>
          </CardHeader>
          <CardContent>
            <span className="text-2xl font-bold">{role.permissions.length}</span>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Permissões por Módulo</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {Object.entries(permissionsByModule).map(([module, permissions]) => (
            <div key={module}>
              <h4 className="mb-2 text-sm font-semibold uppercase tracking-wider text-muted-foreground">
                {roleModuleName[module] || module}
              </h4>
              <div className="flex flex-wrap gap-1.5">
                {permissions.map((perm) => (
                  <PermissionBadge key={perm.id} name={perm.name} />
                ))}
              </div>
              <Separator className="mt-3" />
            </div>
          ))}
          {role.permissions.length === 0 && (
            <p className="text-sm text-muted-foreground">Nenhuma permissão atribuída.</p>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
