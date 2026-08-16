"use client";

import { Loader2 } from "lucide-react";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { ScrollArea } from "@/components/ui/scroll-area";
import { EmptyState } from "@/components/common/EmptyState";
import type { Conversation } from "../types/omnichannel.types";

type Props = {
  conversations: Conversation[];
  isLoading: boolean;
  activeId: string | null;
  onSelect: (conversation: Conversation) => void;
};

export function ConversationList({
  conversations,
  isLoading,
  activeId,
  onSelect,
}: Props) {
  if (isLoading) {
    return (
      <div className="flex items-center justify-center gap-2 py-16 text-muted-foreground">
        <Loader2 className="h-4 w-4 animate-spin" /> Carregando…
      </div>
    );
  }

  if (conversations.length === 0) {
    return (
      <EmptyState
        title="Nenhuma conversa"
        description="Quando um cliente enviar mensagem, a conversa aparecerá aqui."
      />
    );
  }

  return (
    <ScrollArea className="h-full">
      <div className="divide-y">
        {conversations.map((c) => (
          <Button
            key={c.id}
            variant="ghost"
            className={cn(
              "h-auto w-full flex-col items-stretch gap-1 rounded-none px-4 py-3",
              activeId === c.id && "bg-accent",
            )}
            onClick={() => onSelect(c)}
          >
            <div className="flex w-full items-center justify-between">
              <span className="font-medium">{c.externalPhone}</span>
              {c.unreadCount > 0 && (
                <span className="rounded-full bg-primary px-2 py-0.5 text-xs font-medium text-primary-foreground">
                  {c.unreadCount}
                </span>
              )}
            </div>
            <p className="line-clamp-1 w-full text-left text-sm text-muted-foreground">
              {c.lastMessage ?? "Sem mensagens"}
            </p>
          </Button>
        ))}
      </div>
    </ScrollArea>
  );
}
