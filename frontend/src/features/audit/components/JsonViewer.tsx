"use client";

import { useState } from "react";
import { ChevronDown, ChevronRight } from "lucide-react";
import { cn } from "@/lib/utils";

interface JsonViewerProps {
  data: Record<string, unknown>;
  className?: string;
}

export function JsonViewer({ data, className }: JsonViewerProps) {
  const [expanded, setExpanded] = useState(true);

  if (!data || Object.keys(data).length === 0) {
    return <p className="text-sm text-muted-foreground">Sem dados</p>;
  }

  return (
    <div className={cn("rounded-md border bg-muted/30 p-3", className)}>
      <button
        onClick={() => setExpanded(!expanded)}
        className="flex items-center gap-1 text-sm font-medium text-muted-foreground hover:text-foreground mb-2"
      >
        {expanded ? <ChevronDown className="h-3 w-3" /> : <ChevronRight className="h-3 w-3" />}
        {expanded ? "Recolher" : "Expandir"}
      </button>
      {expanded && (
        <pre className="text-xs font-mono whitespace-pre-wrap break-all">
          {JSON.stringify(data, null, 2)}
        </pre>
      )}
    </div>
  );
}
