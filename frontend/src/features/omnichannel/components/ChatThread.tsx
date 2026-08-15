"use client";

import { useEffect, useRef, useState, type FormEvent } from "react";
import { Loader2, Send } from "lucide-react";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Badge } from "@/components/ui/badge";
import { EmptyState } from "@/components/common/EmptyState";
import {
  MESSAGE_STATUS_LABELS,
  type ConversationDetail,
} from "../types/omnichannel.types";

type Props = {
  detail: ConversationDetail | undefined;
  isLoading: boolean;
  canSend: boolean;
  onSend: (body: string) => void;
  sending: boolean;
};

export function ChatThread({ detail, isLoading, canSend, onSend, sending }: Props) {
  const [body, setBody] = useState("");
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [detail?.messages.content.length]);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center gap-2 text-muted-foreground">
        <Loader2 className="h-4 w-4 animate-spin" /> Carregando conversa…
      </div>
    );
  }

  if (!detail) {
    return (
      <EmptyState
        title="Selecione uma conversa"
        description="Escolha uma conversa à esquerda para visualizar e responder."
      />
    );
  }

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    const trimmed = body.trim();
    if (!trimmed) return;
    onSend(trimmed);
    setBody("");
  };

  return (
    <div className="flex h-full flex-col">
      <div className="flex items-center justify-between border-b px-4 py-3">
        <div>
          <p className="font-semibold">{detail.externalPhone}</p>
          <p className="text-xs text-muted-foreground">
            {detail.contactId ? "Contato vinculado" : "Contato não vinculado"}
          </p>
        </div>
        <Badge variant="secondary">
          {detail.unreadCount > 0 ? `${detail.unreadCount} não lida(s)` : "Em dia"}
        </Badge>
      </div>

      <ScrollArea className="flex-1">
        <div className="space-y-2 p-4">
          {detail.messages.content.length === 0 && (
            <p className="text-center text-sm text-muted-foreground">
              Nenhuma mensagem nesta conversa.
            </p>
          )}
          {detail.messages.content.map((m) => {
            const outbound = m.direction === "OUTBOUND";
            return (
              <div
                key={m.id}
                className={cn(
                  "flex",
                  outbound ? "justify-end" : "justify-start",
                )}
              >
                <div
                  className={cn(
                    "max-w-[75%] rounded-2xl px-4 py-2 text-sm shadow-sm",
                    outbound
                      ? "bg-primary text-primary-foreground"
                      : "bg-muted",
                  )}
                >
                  <p className="whitespace-pre-wrap break-words">{m.body}</p>
                  <div
                    className={cn(
                      "mt-1 flex items-center gap-2 text-[11px]",
                      outbound ? "text-primary-foreground/70" : "text-muted-foreground",
                    )}
                  >
                    <span>{MESSAGE_STATUS_LABELS[m.status]}</span>
                    {m.status === "FAILED" && m.providerError && <span>• {m.providerError}</span>}
                  </div>
                </div>
              </div>
            );
          })}
          <div ref={bottomRef} />
        </div>
      </ScrollArea>

      <form onSubmit={handleSubmit} className="flex items-center gap-2 border-t p-3">
        <Input
          value={body}
          onChange={(e) => setBody(e.target.value)}
          placeholder="Digite sua mensagem…"
          disabled={!canSend || sending}
        />
        <Button type="submit" size="icon" disabled={!canSend || sending || !body.trim()}>
          <Send className="h-4 w-4" />
        </Button>
      </form>
    </div>
  );
}
