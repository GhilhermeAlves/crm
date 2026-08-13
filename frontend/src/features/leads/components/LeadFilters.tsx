"use client";

import { Button } from "@/components/ui/button";
import { RefreshCw } from "lucide-react";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  leadStatusLabels,
  leadSourceLabels,
  leadClassificationLabels,
} from "../schemas/lead.schema";

interface LeadFiltersProps {
  status: string;
  source: string;
  classification: string;
  onStatusChange: (value: string) => void;
  onSourceChange: (value: string) => void;
  onClassificationChange: (value: string) => void;
  onRefresh?: () => void;
}

export function LeadFilters({
  status,
  source,
  classification,
  onStatusChange,
  onSourceChange,
  onClassificationChange,
  onRefresh,
}: LeadFiltersProps) {
  return (
    <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
      <Select value={status} onValueChange={onStatusChange}>
        <SelectTrigger className="w-full sm:w-44">
          <SelectValue placeholder="Status" />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="all">Todos</SelectItem>
          {Object.entries(leadStatusLabels).map(([value, label]) => (
            <SelectItem key={value} value={value}>
              {label}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
      <Select value={source} onValueChange={onSourceChange}>
        <SelectTrigger className="w-full sm:w-44">
          <SelectValue placeholder="Origem" />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="all">Todas</SelectItem>
          {Object.entries(leadSourceLabels).map(([value, label]) => (
            <SelectItem key={value} value={value}>
              {label}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
      <Select value={classification} onValueChange={onClassificationChange}>
        <SelectTrigger className="w-full sm:w-44">
          <SelectValue placeholder="Classificação" />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="all">Todas</SelectItem>
          {Object.entries(leadClassificationLabels).map(([value, label]) => (
            <SelectItem key={value} value={value}>
              {label}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
      {onRefresh && (
        <Button variant="outline" size="icon" onClick={onRefresh}>
          <RefreshCw className="h-4 w-4" />
        </Button>
      )}
    </div>
  );
}