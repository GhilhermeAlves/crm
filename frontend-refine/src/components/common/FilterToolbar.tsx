// "use client"
import { ReactNode } from "react";
import { cn } from "@/lib/utils";
import { SearchInput } from "./SearchInput";
import { Button } from "@/components/ui/button";
import { ChevronDown, ChevronUp, SearchX } from "lucide-react";

/**
 * FilterToolbar – reusable component that combines a search input, a toggle
 * button for showing additional filter controls, and a clear‑filters button.
 *
 * It is intentionally generic so that each page can pass its own extra filter
 * UI as `children`. The component does **not** manage the actual filter state –
 * that remains in the consuming page – but it provides the UI glue.
 */
export type FilterToolbarProps = {
  /** Current search value */
  search: string;
  /** Callback when the search input changes */
  onSearchChange: (value: string) => void;
  /** Whether the extra filter panel is open */
  filtersOpen: boolean;
  /** Toggle the filter panel open state */
  setFiltersOpen: (open: boolean) => void;
  /** Whether any active filters (including search) are applied */
  hasActiveFilters: boolean;
  /** Callback to clear all filters */
  onClearFilters: () => void;
  /** Optional extra filter UI rendered when `filtersOpen` is true */
  children?: ReactNode;
};

export function FilterToolbar({
  search,
  onSearchChange,
  filtersOpen,
  setFiltersOpen,
  hasActiveFilters,
  onClearFilters,
  children,
}: FilterToolbarProps) {
  return (
    <div className={cn("flex flex-col gap-3", filtersOpen && "mb-4")}>
      {/* Main controls */}
      <div className="flex flex-wrap items-center gap-2">
        <SearchInput
          value={search}
          onChange={(e) => onSearchChange(e.target.value)}
          placeholder="Pesquisar..."
        />
        <Button variant="outline" size="sm" onClick={() => setFiltersOpen(!filtersOpen)}>
          {filtersOpen ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
          Filtros
        </Button>
        {hasActiveFilters && (
          <Button variant="ghost" size="sm" onClick={onClearFilters} className="gap-1">
            <SearchX className="h-4 w-4" />
            Limpar filtros
          </Button>
        )}
      </div>
      {/* Extra filter UI */}
      {filtersOpen && children}
    </div>
  );
}
