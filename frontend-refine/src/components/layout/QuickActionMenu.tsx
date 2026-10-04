"use client";

import { useRouter } from "next/navigation";
import { Plus, UserPlus, CalendarPlus, ListTodo, Phone } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { ROUTES } from "@/lib/constants";

const QUICK_ACTIONS = [
  { label: "Novo contato", icon: UserPlus, href: "/contacts/new" },
  { label: "Novo agendamento", icon: CalendarPlus, href: ROUTES.AGENDA },
  { label: "Nova tarefa", icon: ListTodo, href: ROUTES.TASKS },
  { label: "Nova atividade", icon: Phone, href: ROUTES.ACTIVITIES },
] as const;

export function QuickActionMenu() {
  const router = useRouter();

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button size="sm" className="gap-1.5">
          <Plus className="h-4 w-4" />
          <span className="hidden sm:inline">Ação</span>
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-48">
        {QUICK_ACTIONS.map((action) => (
          <DropdownMenuItem
            key={action.href}
            onClick={() => router.push(action.href)}
            className="cursor-pointer gap-2"
          >
            <action.icon className="h-4 w-4" />
            {action.label}
          </DropdownMenuItem>
        ))}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
