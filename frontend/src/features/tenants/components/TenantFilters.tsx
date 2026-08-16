"use client";

import { SearchInput } from "@/components/common/SearchInput";
import { FilterBar } from "@/components/common/FilterBar";
import { Button } from "@/components/ui/button";
import { RefreshCw } from "lucide-react";

type TenantFiltersProps = {
  search: string;
  onSearchChange: (value: string) => void;
  onRefresh: () => void;
};

export function TenantFilters({
  search,
  onSearchChange,
  onRefresh,
}: TenantFiltersProps) {
  return (
    <FilterBar>
      <SearchInput
        placeholder="Pesquisar empresas..."
        value={search}
        onChange={(e) => onSearchChange(e.target.value)}
        onClear={() => onSearchChange("")}
        className="w-full sm:w-80"
      />
      <Button
        variant="outline"
        size="icon"
        onClick={onRefresh}
        className="shrink-0"
      >
        <RefreshCw className="h-4 w-4" />
      </Button>
    </FilterBar>
  );
}
