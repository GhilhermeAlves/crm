import { forwardRef } from "react";
import { cn } from "@/lib/utils";
import { Button, type ButtonProps } from "@/components/ui/button";

export const ActionButton = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, ...props }, ref) => {
    return <Button ref={ref} className={cn("gap-2", className)} {...props} />;
  },
);
ActionButton.displayName = "ActionButton";
