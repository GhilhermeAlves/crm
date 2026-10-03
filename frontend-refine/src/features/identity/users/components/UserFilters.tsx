"use client";

import { SearchInput } from "@/components/common/SearchInput";
import { Button } from "@/components/ui/button";
import { RefreshCw } from "lucide-react";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

interface UserFiltersProps {
  search: string;
  status: string;
  onSearchChange: (value: string) => void;
  onStatusChange: (value: string) => void;
  onRefresh?: () => void;
}

export function UserFilters({
  search,
  status,
  onSearchChange,
  onStatusChange,
  onRefresh,
}: UserFiltersProps) {
  return (
    <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
      <SearchInput
        placeholder="Buscar por nome, email..."
        value={search}
        onChange={(e) => onSearchChange(e.target.value)}
        className="w-full sm:w-72"
      />
      <Select value={status} onValueChange={onStatusChange}>
        <SelectTrigger className="w-full sm:w-40">
          <SelectValue placeholder="Status" />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="all">Todos</SelectItem>
          <SelectItem value="active">Ativo</SelectItem>
          <SelectItem value="inactive">Inativo</SelectItem>
          <SelectItem value="locked">Bloqueado</SelectItem>
          <SelectItem value="pending">Pendente</SelectItem>
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
