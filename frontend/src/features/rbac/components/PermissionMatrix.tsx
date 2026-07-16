"use client";

import { useMemo } from "react";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import { Separator } from "@/components/ui/separator";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { PermissionBadge } from "./PermissionBadge";
import type { Permission } from "../types/rbac.types";
import { roleModuleName } from "../schemas/role.schema";

interface PermissionMatrixProps {
  permissions: Permission[];
  selectedPermissionIds: string[];
  onToggle: (permissionId: string) => void;
  readOnly?: boolean;
}

export function PermissionMatrix({
  permissions,
  selectedPermissionIds,
  onToggle,
  readOnly = false,
}: PermissionMatrixProps) {
  const groupedByModule = useMemo(() => {
    const groups: Record<string, Permission[]> = {};
    permissions.forEach((p) => {
      if (!groups[p.module]) groups[p.module] = [];
      groups[p.module].push(p);
    });
    return groups;
  }, [permissions]);

  const selectedCount = selectedPermissionIds.length;

  return (
    <Card>
      <CardHeader className="pb-3">
        <div className="flex items-center justify-between">
          <CardTitle className="text-lg">Permissões</CardTitle>
          <Badge variant="secondary">
            {selectedCount} selecionada(s)
          </Badge>
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        {Object.entries(groupedByModule).map(([module, modulePermissions]) => (
          <div key={module}>
            <div className="flex items-center gap-2 mb-2">
              <h4 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
                {roleModuleName[module] || module}
              </h4>
              <Badge variant="outline" className="text-[10px]">
                {modulePermissions.filter((p) => selectedPermissionIds.includes(p.id)).length}/
                {modulePermissions.length}
              </Badge>
            </div>
            <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-2 pl-2">
              {modulePermissions.map((permission) => (
                <label
                  key={permission.id}
                  className={`flex items-center gap-2 rounded-md border p-2 cursor-pointer transition-colors ${
                    selectedPermissionIds.includes(permission.id)
                      ? "border-primary bg-primary/5"
                      : "border-border hover:bg-muted/50"
                  } ${readOnly ? "cursor-default opacity-70" : ""}`}
                >
                  {!readOnly && (
                    <Checkbox
                      checked={selectedPermissionIds.includes(permission.id)}
                      onCheckedChange={() => onToggle(permission.id)}
                    />
                  )}
                  <div className="flex-1 min-w-0">
                    <p className="text-xs font-medium truncate">{permission.name}</p>
                    <p className="text-[10px] text-muted-foreground truncate">
                      {permission.description}
                    </p>
                  </div>
                </label>
              ))}
            </div>
            <Separator className="mt-3" />
          </div>
        ))}
      </CardContent>
    </Card>
  );
}
