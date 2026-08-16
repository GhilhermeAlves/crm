"use client";

import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";

interface PermissionBadgeProps {
  name: string;
  className?: string;
}

const actionColors: Record<string, string> = {
  create: "bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400",
  read: "bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400",
  update: "bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400",
  delete: "bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400",
  view: "bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400",
  invite: "bg-indigo-100 text-indigo-800 dark:bg-indigo-900/30 dark:text-indigo-400",
  assign: "bg-purple-100 text-purple-800 dark:bg-purple-900/30 dark:text-purple-400",
  send: "bg-cyan-100 text-cyan-800 dark:bg-cyan-900/30 dark:text-cyan-400",
  export: "bg-teal-100 text-teal-800 dark:bg-teal-900/30 dark:text-teal-400",
};

export function PermissionBadge({ name, className }: PermissionBadgeProps) {
  const action = name.split(":")[1] || "read";
  const colorClass =
    actionColors[action] || "bg-gray-100 text-gray-800 dark:bg-gray-900/30 dark:text-gray-400";

  return (
    <Badge variant="outline" className={cn("text-[10px] font-medium", colorClass, className)}>
      {name}
    </Badge>
  );
}
