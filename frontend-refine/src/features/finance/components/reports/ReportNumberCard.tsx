import { Card } from "@/components/ui/card";
import { cn } from "@/lib/utils";

type ReportNumberCardProps = {
  label: string;
  value: string;
  className?: string;
};

export function ReportNumberCard({ label, value, className }: ReportNumberCardProps) {
  return (
    <Card className={cn("p-4", className)}>
      <p className="truncate text-xs text-muted-foreground" title={label}>
        {label}
      </p>
      <p className="mt-3 text-center text-2xl font-bold tracking-tight">{value}</p>
    </Card>
  );
}
