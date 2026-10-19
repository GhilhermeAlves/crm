"use client";

import { Loader2, UserX } from "lucide-react";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { ScrollArea } from "@/components/ui/scroll-area";
import { EmptyState } from "@/components/common/EmptyState";
import type { Conversation } from "../types/omnichannel.types";
import { formatConversationTimestamp } from "../utils/messageDates";

type Props = {
  conversations: Conversation[];
  isLoading: boolean;
  activeId: string | null;
  onSelect: (conversation: Conversation) => void;
  /** Sem ele, a opção de ignorar não aparece (usuário sem permissão). */
  onIgnore?: (conversation: Conversation) => void;
};

export function ConversationList({
  conversations,
  isLoading,
  activeId,
  onSelect,
  onIgnore,
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
    <ScrollArea className="h-full" type="always">
      <div className="divide-y">
        {conversations.map((c) => (
          <div key={c.id} className="relative">
            <Button
              variant="ghost"
              className={cn(
                "h-auto w-full flex-col items-stretch gap-1 rounded-none px-4 py-3",
                onIgnore && "pr-12",
                activeId === c.id && "bg-accent",
              )}
              onClick={() => onSelect(c)}
            >
              <div className="flex w-full items-center justify-between">
                <span className="font-medium">{c.externalPhone}</span>
                {c.lastMessageAt && (
                  <span className="text-xs text-muted-foreground">
                    {formatConversationTimestamp(c.lastMessageAt)}
                  </span>
                )}
              </div>
              <div className="flex w-full items-center justify-between gap-2">
                <p className="line-clamp-1 text-left text-sm text-muted-foreground">
                  {c.lastMessage ?? "Sem mensagens"}
                </p>
                {c.unreadCount > 0 && (
                  <span className="shrink-0 rounded-full bg-primary px-2 py-0.5 text-xs font-medium text-primary-foreground">
                    {c.unreadCount}
                  </span>
                )}
              </div>
            </Button>
            {onIgnore && (
              <Button
                variant="ghost"
                size="icon"
                className="absolute right-2 top-1/2 h-8 w-8 -translate-y-1/2 text-muted-foreground hover:text-destructive"
                aria-label="Ignorar contato"
                title="Ignorar contato"
                onClick={() => onIgnore(c)}
              >
                <UserX className="h-4 w-4" />
              </Button>
            )}
          </div>
        ))}
      </div>
    </ScrollArea>
  );
}
