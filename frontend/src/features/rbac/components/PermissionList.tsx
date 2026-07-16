"use client";

import { useMemo, useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Search } from "lucide-react";
import { PermissionBadge } from "./PermissionBadge";
import type { Permission } from "../types/rbac.types";
import { roleModuleName, actionName } from "../schemas/role.schema";

interface PermissionListProps {
  permissions: Permission[];
}

export function PermissionList({ permissions }: PermissionListProps) {
  const [search, setSearch] = useState("");

  const filtered = useMemo(() => {
    if (!search) return permissions;
    const q = search.toLowerCase();
    return permissions.filter(
      (p) =>
        p.name.toLowerCase().includes(q) ||
        p.description.toLowerCase().includes(q) ||
        p.module.toLowerCase().includes(q)
    );
  }, [permissions, search]);

  const groupedByModule = useMemo(() => {
    const groups: Record<string, Permission[]> = {};
    filtered.forEach((p) => {
      if (!groups[p.module]) groups[p.module] = [];
      groups[p.module].push(p);
    });
    return groups;
  }, [filtered]);

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-2">
        <div className="relative flex-1 max-w-sm">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            placeholder="Buscar permissões..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="pl-9"
          />
        </div>
        <Badge variant="secondary">{filtered.length} permissão(ões)</Badge>
      </div>

      {Object.entries(groupedByModule).map(([module, modulePermissions]) => (
        <Card key={module}>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
              {roleModuleName[module] || module}
              <Badge variant="outline" className="ml-2 text-[10px]">
                {modulePermissions.length}
              </Badge>
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-2">
              {modulePermissions.map((permission) => (
                <div
                  key={permission.id}
                  className="flex items-start gap-3 rounded-md border p-3"
                >
                  <PermissionBadge name={permission.name} />
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium">{permission.description}</p>
                    <p className="text-xs text-muted-foreground">
                      {actionName[permission.action] || permission.action}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      ))}
    </div>
  );
}
