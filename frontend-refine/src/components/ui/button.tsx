import * as React from "react";
import { Slot } from "@radix-ui/react-slot";
import { cva, type VariantProps } from "class-variance-authority";

import { cn } from "@/lib/utils";

const buttonVariants = cva(
  "inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-md text-sm font-medium ring-offset-background transition-all duration-crm-default ease-in-out focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 disabled:shadow-none disabled:translate-y-0 [&_svg]:pointer-events-none [&_svg]:size-4 [&_svg]:shrink-0",
  {
    variants: {
      variant: {
        default:
          "bg-gradient-to-r from-crm-primary to-crm-primary-dark text-white shadow-crm-md hover:shadow-crm-lg hover:-translate-y-0.5 active:translate-y-0 active:shadow-crm-sm",
        crm: "bg-crm-primary text-crm-primary-foreground hover:bg-crm-primary-hover focus-visible:ring-crm-primary",
        destructive:
          "bg-gradient-to-r from-destructive to-red-900 text-destructive-foreground shadow-crm-md hover:shadow-crm-lg hover:-translate-y-0.5 active:translate-y-0 active:shadow-crm-sm",
        danger:
          "bg-gradient-to-r from-crm-error to-red-900 text-white shadow-crm-md hover:shadow-crm-lg hover:-translate-y-0.5 active:translate-y-0 active:shadow-crm-sm",
        outline:
          "border border-input bg-background hover:border-crm-primary hover:bg-accent hover:text-accent-foreground",
        secondary:
          "bg-secondary text-secondary-foreground hover:bg-secondary/80 hover:-translate-y-0.5",
        ghost: "hover:bg-accent hover:text-accent-foreground",
        link: "text-primary underline-offset-4 hover:underline",
      },
      size: {
        default: "h-10 px-4 py-2",
        sm: "h-9 rounded-md px-3",
        lg: "h-11 rounded-md px-8",
        icon: "h-10 w-10",
      },
    },
    defaultVariants: {
      variant: "default",
      size: "default",
    },
  },
);

export interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement>, VariantProps<typeof buttonVariants> {
  asChild?: boolean;
}

const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant, size, asChild = false, ...props }, ref) => {
    const Comp = asChild ? Slot : "button";
    return (
      <Comp className={cn(buttonVariants({ variant, size, className }))} ref={ref} {...props} />
    );
  },
);
Button.displayName = "Button";

export { Button, buttonVariants };
