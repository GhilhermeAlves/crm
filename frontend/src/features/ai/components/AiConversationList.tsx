"use client";

import { format } from "date-fns";
import { ptBR } from "date-fns/locale";
import { MessageSquare, Plus } from "lucide-react";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Skeleton } from "@/components/ui/skeleton";
import type { AiConversation } from "../types/ai.types";

type Props = {
  conversations: AiConversation[];
  selectedId: string | null;
  loading: boolean;
  onSelect: (conversationId: string) => void;
  onNewConversation: () => void;
};

function formatUpdatedAt(value: string): string {
  try {
    return format(new Date(value), "dd/MM/yyyy HH:mm", { locale: ptBR });
  } catch {
    return "";
  }
}

/**
 * Lista de conversas do Assistente de IA (AI-04 §16). Sempre scoped ao usuário
 * autenticado - o backend rejeita conversas de outro usuário/empresa (404).
 */
export function AiConversationList({
  conversations,
  selectedId,
  loading,
  onSelect,
  onNewConversation,
}: Props) {
  return (
    <div className="flex h-full flex-col">
      <div className="border-b p-3">
        <Button
          variant="outline"
          size="sm"
          className="w-full justify-start"
          onClick={onNewConversation}
        >
          <Plus className="mr-2 h-4 w-4" />
          Nova conversa
        </Button>
      </div>
      <ScrollArea className="flex-1">
        <div className="space-y-1 p-2">
          {loading &&
            Array.from({ length: 4 }).map((_, i) => (
              <div key={i} className="space-y-2 rounded-md p-2">
                <Skeleton className="h-3.5 w-3/4" />
                <Skeleton className="h-3 w-1/3" />
              </div>
            ))}

          {!loading && conversations.length === 0 && (
            <p className="px-3 py-4 text-center text-sm text-muted-foreground">
              Nenhuma conversa ainda. Inicie perguntando ao assistente.
            </p>
          )}

          {!loading &&
            conversations.map((conversation) => {
              const active = conversation.id === selectedId;
              return (
                <button
                  key={conversation.id}
                  type="button"
                  onClick={() => onSelect(conversation.id)}
                  className={cn(
                    "flex w-full flex-col gap-1 rounded-md px-3 py-2 text-left transition-colors",
                    "hover:bg-accent hover:text-accent-foreground",
                    active && "bg-accent text-accent-foreground",
                  )}
                >
                  <span className="flex items-center gap-2 text-sm font-medium">
                    <MessageSquare className="h-3.5 w-3.5 shrink-0 text-muted-foreground" />
                    <span className="truncate">{conversation.title || "Conversa"}</span>
                  </span>
                  <span className="pl-5.5 text-xs text-muted-foreground">
                    {formatUpdatedAt(conversation.updatedAt)}
                  </span>
                </button>
              );
            })}
        </div>
      </ScrollArea>
    </div>
  );
}
