import { type ReactNode } from "react";
import { cn } from "@/lib/utils";

type PageTitleProps = {
  children: ReactNode;
  className?: string;
  as?: "h1" | "h2";
};

export function PageTitle({ children, className, as: Component = "h1" }: PageTitleProps) {
  return (
    <Component
      className={cn(
        "bg-gradient-to-r from-crm-secondary-purple via-crm-secondary-indigo to-crm-tertiary-cyan bg-clip-text text-2xl font-bold tracking-tight text-transparent lg:text-3xl",
        className,
      )}
    >
      {children}
    </Component>
  );
}
