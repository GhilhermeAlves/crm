"use client";

import { useEffect, useRef, useState, type FormEvent } from "react";
import { Bot, CalendarClock, Loader2, Send, Sparkles, UserCheck } from "lucide-react";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Badge } from "@/components/ui/badge";
import { EmptyState } from "@/components/common/EmptyState";
import { useSuggestReply, useAiPermissions } from "@/features/ai/hooks/useAi";
import {
  useOmnichannelPermissions,
  useTakeoverConversation,
  useReleaseConversation,
  useConversationFollowUps,
  useCreateFollowUp,
  useCancelFollowUp,
} from "../hooks/useOmnichannel";
import {
  CONVERSATION_MODE_LABELS,
  FOLLOW_UP_STATUS_LABELS,
  MESSAGE_STATUS_LABELS,
  type ConversationDetail,
} from "../types/omnichannel.types";
import { FollowUpDialog } from "./FollowUpDialog";

function formatFollowUpDate(value: string): string {
  return new Date(value).toLocaleString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

type Props = {
  detail: ConversationDetail | undefined;
  isLoading: boolean;
  canSend: boolean;
  onSend: (body: string) => void;
  sending: boolean;
};

export function ChatThread({ detail, isLoading, canSend, onSend, sending }: Props) {
  const [body, setBody] = useState("");
  const [followUpDialogOpen, setFollowUpDialogOpen] = useState(false);
  const bottomRef = useRef<HTMLDivElement>(null);
  const suggestReply = useSuggestReply();
  const { canSuggest } = useAiPermissions();
  const { canTakeover, canFollowUpRead, canFollowUpManage } = useOmnichannelPermissions();
  const takeover = useTakeoverConversation();
  const release = useReleaseConversation();
  const followUps = useConversationFollowUps(detail ? detail.id : null);
  const createFollowUp = useCreateFollowUp(detail ? detail.id : null);
  const cancelFollowUp = useCancelFollowUp(detail ? detail.id : null);

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

  const humanMode = detail.mode === "HUMAN";
  const handoffPending = takeover.isPending || release.isPending;
  const showFollowUps = canFollowUpRead || canFollowUpManage;
  const nextFollowUp = followUps.data?.content.find((f) => f.status === "PENDING");

  return (
    <div className="flex h-full flex-col">
      <div className="flex items-center justify-between gap-2 border-b px-4 py-3">
        <div>
          <p className="font-semibold">{detail.externalPhone}</p>
          <div className="flex items-center gap-2">
            <p className="text-xs text-muted-foreground">
              {detail.contactId ? "Contato vinculado" : "Contato não vinculado"}
            </p>
            {humanMode && <Badge variant="destructive">{CONVERSATION_MODE_LABELS.HUMAN}</Badge>}
          </div>
        </div>
        <div className="flex items-center gap-2">
          <Badge variant="secondary">
            {detail.unreadCount > 0 ? `${detail.unreadCount} não lida(s)` : "Em dia"}
          </Badge>
          {canTakeover && (
            <Button
              type="button"
              variant={humanMode ? "outline" : "default"}
              size="sm"
              disabled={handoffPending}
              onClick={() => {
                if (humanMode) {
                  release.mutate(detail.id);
                } else {
                  takeover.mutate(detail.id);
                }
              }}
            >
              {handoffPending ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : humanMode ? (
                <Bot className="h-4 w-4" />
              ) : (
                <UserCheck className="h-4 w-4" />
              )}
              {humanMode ? "Retomar IA" : "Assumir manualmente"}
            </Button>
          )}
        </div>
      </div>

      {showFollowUps && (
        <div className="flex flex-wrap items-center justify-between gap-2 border-b px-4 py-2">
          <div className="flex flex-wrap items-center gap-2 text-sm">
            <CalendarClock className="h-4 w-4 text-muted-foreground" />
            {nextFollowUp ? (
              <>
                <span className="font-medium">Próximo follow-up</span>
                <span>{formatFollowUpDate(nextFollowUp.executeAt)}</span>
                <Badge variant="secondary">{FOLLOW_UP_STATUS_LABELS[nextFollowUp.status]}</Badge>
                <span className="max-w-[40ch] truncate text-muted-foreground">
                  {nextFollowUp.actionContent || "Mensagem"}
                </span>
                {canFollowUpManage && (
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    disabled={cancelFollowUp.isPending}
                    onClick={() => cancelFollowUp.mutate(nextFollowUp.id)}
                  >
                    Cancelar
                  </Button>
                )}
              </>
            ) : (
              <span className="text-muted-foreground">Nenhum follow-up agendado.</span>
            )}
          </div>
          {canFollowUpManage && (
            <Button
              type="button"
              variant="outline"
              size="sm"
              disabled={humanMode || createFollowUp.isPending}
              title={
                humanMode
                  ? "Follow-ups automáticos ficam suspensos em atendimento humano"
                  : undefined
              }
              onClick={() => setFollowUpDialogOpen(true)}
            >
              <CalendarClock className="h-4 w-4" />
              Agendar follow-up
            </Button>
          )}
        </div>
      )}

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
              <div key={m.id} className={cn("flex", outbound ? "justify-end" : "justify-start")}>
                <div
                  className={cn(
                    "max-w-[75%] rounded-2xl px-4 py-2 text-sm shadow-sm",
                    outbound ? "bg-primary text-primary-foreground" : "bg-muted",
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
        {canSuggest && (
          <Button
            type="button"
            variant="outline"
            size="icon"
            className="shrink-0"
            aria-label="Sugerir resposta com IA"
            title="Sugerir resposta com IA"
            disabled={suggestReply.isPending || !detail}
            onClick={() => {
              suggestReply.mutate(detail.id, {
                onSuccess: (res) => setBody(res.suggestion),
              });
            }}
          >
            {suggestReply.isPending ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <Sparkles className="h-4 w-4" />
            )}
          </Button>
        )}
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

      <FollowUpDialog
        open={followUpDialogOpen}
        onOpenChange={setFollowUpDialogOpen}
        conversationId={detail.id}
        isSubmitting={createFollowUp.isPending}
        onSubmit={(request) => {
          createFollowUp.mutate(request, {
            onSuccess: () => setFollowUpDialogOpen(false),
          });
        }}
      />
    </div>
  );
}
