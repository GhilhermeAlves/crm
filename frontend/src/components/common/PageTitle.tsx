import { type ReactNode } from "react";
import { cn } from "@/lib/utils";

type PageTitleProps = {
  children: ReactNode;
  className?: string;
};

export function PageTitle({ children, className }: PageTitleProps) {
  return (
    <h1
      className={cn("text-2xl font-bold tracking-tight lg:text-3xl", className)}
    >
      {children}
    </h1>
  );
}
