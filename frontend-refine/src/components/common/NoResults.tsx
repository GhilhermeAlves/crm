import { type ReactNode } from "react";
import { cn } from "@/lib/utils";
import { SearchX } from "lucide-react";

type NoResultsProps = {
  title?: string;
  description?: string;
  action?: ReactNode;
  className?: string;
};

export function NoResults({
  title = "Nenhum resultado encontrado",
  description = "Tente ajustar sua pesquisa ou filtros.",
  action,
  className,
}: NoResultsProps) {
  return (
    <div className={cn("flex flex-col items-center justify-center py-12 text-center", className)}>
      <div className="mb-4 rounded-full bg-muted p-4">
        <SearchX className="h-8 w-8 text-muted-foreground" />
      </div>
      <h3 className="text-lg font-semibold">{title}</h3>
      <p className="mt-1 max-w-sm text-sm text-muted-foreground">{description}</p>
      {action && <div className="mt-4">{action}</div>}
    </div>
  );
}
