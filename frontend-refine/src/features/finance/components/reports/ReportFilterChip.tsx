"use client";

import { useState, type ReactNode } from "react";
import { ChevronDown, X, type LucideIcon } from "lucide-react";

import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { cn } from "@/lib/utils";

type ReportFilterChipProps = {
  icon: LucideIcon;
  label: string;
  value?: string | null;
  onClear?: () => void;
  children: ReactNode;
  contentClassName?: string;
  align?: "start" | "center" | "end";
  open?: boolean;
  onOpenChange?: (open: boolean) => void;
};

export function ReportFilterChip({
  icon: Icon,
  label,
  value,
  onClear,
  children,
  contentClassName,
  align = "start",
  open,
  onOpenChange,
}: ReportFilterChipProps) {
  const [internalOpen, setInternalOpen] = useState(false);
  const isControlled = open !== undefined;
  const isOpen = isControlled ? open : internalOpen;

  const handleOpenChange = (next: boolean) => {
    if (!isControlled) setInternalOpen(next);
    onOpenChange?.(next);
  };

  const hasValue = Boolean(value);

  return (
    <div
      className={cn(
        "inline-flex h-8 items-center rounded-md border bg-background text-xs transition-colors",
        isOpen && "border-primary/50",
      )}
    >
      <Popover open={isOpen} onOpenChange={handleOpenChange}>
        <PopoverTrigger asChild>
          <button
            type="button"
            className="inline-flex h-full items-center gap-1.5 rounded-md px-2.5 hover:bg-muted"
          >
            <Icon className="h-3.5 w-3.5 text-muted-foreground" />
            <span className="font-medium text-muted-foreground">{label}</span>
            {hasValue && (
              <>
                <span className="text-muted-foreground">:</span>
                <span className="font-medium text-primary">{value}</span>
              </>
            )}
            {(!hasValue || !onClear) && (
              <ChevronDown className="h-3.5 w-3.5 text-muted-foreground" />
            )}
          </button>
        </PopoverTrigger>
        <PopoverContent align={align} className={cn("p-0", contentClassName)}>
          {children}
        </PopoverContent>
      </Popover>

      {hasValue && onClear && (
        <button
          type="button"
          aria-label={`Limpar ${label}`}
          onClick={onClear}
          className="mr-1 inline-flex h-5 w-5 items-center justify-center rounded text-muted-foreground hover:bg-muted hover:text-foreground"
        >
          <X className="h-3.5 w-3.5" />
        </button>
      )}
    </div>
  );
}
