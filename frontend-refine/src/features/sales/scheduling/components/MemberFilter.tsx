"use client";

import { Checkbox } from "@/components/ui/checkbox";
import { cn } from "@/lib/utils";

export const MEMBER_COLORS = [
  "#7c3aed", // violet
  "#2563eb", // blue
  "#059669", // emerald
  "#d97706", // amber
  "#dc2626", // red
  "#db2777", // pink
  "#0891b2", // cyan
  "#4f46e5", // indigo
  "#65a30d", // lime
  "#9333ea", // purple
] as const;

export function getMemberColor(index: number): string {
  return MEMBER_COLORS[index % MEMBER_COLORS.length];
}

interface Member {
  id: string;
  name: string;
}

interface Props {
  members: Member[];
  selectedIds: Set<string>;
  onToggle: (id: string) => void;
  onToggleAll: () => void;
}

export function MemberFilter({ members, selectedIds, onToggle, onToggleAll }: Props) {
  const allSelected = members.length > 0 && selectedIds.size === members.length;

  return (
    <div className="space-y-2">
      <label className="flex cursor-pointer items-center gap-2 text-xs font-medium">
        <Checkbox checked={allSelected} onCheckedChange={onToggleAll} className="h-3.5 w-3.5" />
        Selecionar tudo
      </label>

      <div className="space-y-1">
        {members.map((m, i) => {
          const color = getMemberColor(i);
          const checked = selectedIds.has(m.id);
          return (
            <label key={m.id} className="flex cursor-pointer items-center gap-2 text-xs">
              <Checkbox
                checked={checked}
                onCheckedChange={() => onToggle(m.id)}
                className="h-3.5 w-3.5"
              />
              <span
                className="h-2.5 w-2.5 shrink-0 rounded-full"
                style={{ backgroundColor: color }}
              />
              <span className={cn("truncate", !checked && "text-muted-foreground")}>{m.name}</span>
            </label>
          );
        })}
      </div>
    </div>
  );
}
