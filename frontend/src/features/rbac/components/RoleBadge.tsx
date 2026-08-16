"use client";

import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";

interface RoleBadgeProps {
  name: string;
  isSystem?: boolean;
  className?: string;
}

const roleColors: Record<string, string> = {
  SUPER_ADMIN: "bg-purple-100 text-purple-800 dark:bg-purple-900/30 dark:text-purple-400",
  ADMIN: "bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400",
  MANAGER: "bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400",
  AGENT: "bg-orange-100 text-orange-800 dark:bg-orange-900/30 dark:text-orange-400",
  VIEWER: "bg-gray-100 text-gray-800 dark:bg-gray-900/30 dark:text-gray-400",
};

export function RoleBadge({ name, isSystem, className }: RoleBadgeProps) {
  const colorClass =
    roleColors[name] || "bg-gray-100 text-gray-800 dark:bg-gray-900/30 dark:text-gray-400";
  const displayName = name.replace(/_/g, " ");

  return (
    <Badge variant="outline" className={cn("font-medium", colorClass, className)}>
      {displayName}
      {isSystem && <span className="ml-1 text-[10px] opacity-60">(S)</span>}
    </Badge>
  );
}
