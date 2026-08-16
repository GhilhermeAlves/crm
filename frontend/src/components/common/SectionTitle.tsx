import { type ReactNode } from "react";
import { cn } from "@/lib/utils";

type SectionTitleProps = {
  children: ReactNode;
  description?: string;
  className?: string;
  action?: ReactNode;
};

export function SectionTitle({ children, description, className, action }: SectionTitleProps) {
  return (
    <div className={cn("flex items-center justify-between", className)}>
      <div className="space-y-1">
        <h2 className="text-lg font-semibold tracking-tight">{children}</h2>
        {description && <p className="text-sm text-muted-foreground">{description}</p>}
      </div>
      {action && <div>{action}</div>}
    </div>
  );
}
