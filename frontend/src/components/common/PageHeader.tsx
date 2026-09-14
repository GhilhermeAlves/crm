import type { ReactNode } from "react";
import { cn } from "@/lib/utils";
import { PageTitle } from "./PageTitle";

export type PageHeaderProps = {
  title: ReactNode;
  actions?: ReactNode;
  className?: string;
};

export function PageHeader({ title, actions, className }: PageHeaderProps) {
  return (
    <div className={cn("flex flex-col space-y-4 md:flex-row md:items-center md:justify-between", className)}>
      <PageTitle>{title}</PageTitle>
      {actions && <div className="flex items-center space-x-2">{actions}</div>}
    </div>
  );
}