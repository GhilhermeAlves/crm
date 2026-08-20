"use client";

import { useCallback, useEffect, useRef, useState, type FormEvent } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { BrainCircuit, Loader2, Plus, Send, Sparkles } from "lucide-react";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Card, CardContent } from "@/components/ui/card";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { useAuth } from "@/features/auth/hooks/useAuth";
import {
  useAiAnalyze,
  useAiChat,
  useAiConversationActions,
  useAiConversationMessages,
  useAiConversations,
  useAiPermissions,
} from "../hooks/useAi";
import { useAiContext } from "../hooks/useAiContext";
import { aiAnalysisErrorMessage, aiErrorMessage } from "../services/ai.service";
import { AiActionProposalCard } from "./AiActionProposalCard";
import { AiAnalysisCard } from "./AiAnalysisCard";
import { AiMessageBubble } from "./AiMessageBubble";
import { AiConversationList } from "./AiConversationList";
import type { AiAnalysisResponse, AiChatRole, AiChatState } from "../types/ai.types";

type UiMessage = {
  key: string;
  role: AiChatRole;
  content: string;
  error?: boolean;
  pending?: boolean;
};

/**
 * Assistente de IA (AI-04). Integra o chat com o histórico de conversas, o
 * contexto da página atual e o contrato POST /api/v1/ai/chat do backend.
 * Estados de UX: idle → sending → processing → success/error (AI-04 §12-13).
 * Não permite múltiplos envios simultâneos enquanto a requisição processa.
 */
export function AiChatAssistant() {
  const { user } = useAuth();
  const { canChat } = useAiPermissions();
  const context = useAiContext();

  const [conversationId, setConversationId] = useState<string | null>(null);
  const [selectedConversationId, setSelectedConversationId] = useState<string | null>(null);
  const [uiMessages, setUiMessages] = useState<UiMessage[]>([]);
  const [input, setInput] = useState("");
  const [chatState, setChatState] = useState<AiChatState>("idle");
  const bottomRef = useRef<HTMLDivElement>(null);
  const lastSeededRef = useRef<string | null>(null);

  const enabled = !!user?.companyId && canChat;
  const chatMutation = useAiChat();
  const analyzeMutation = useAiAnalyze();
  const queryClient = useQueryClient();
  const processing = chatMutation.isPending;
  const analyzing = analyzeMutation.isPending;

  const [analysis, setAnalysis] = useState<AiAnalysisResponse | null>(null);
  const [analysisError, setAnalysisError] = useState<string | null>(null);

  const { data: conversations = [], isLoading: conversationsLoading } = useAiConversations(enabled);
  const { data: historyMessages, isLoading: historyLoading } = useAiConversationMessages(
    selectedConversationId,
    enabled,
  );
  const { data: conversationActions = [] } = useAiConversationActions(
    selectedConversationId,
    enabled,
  );

  useEffect(() => {
    if (selectedConversationId === null) {
      lastSeededRef.current = null;
      return;
    }
    if (historyLoading) {
      return;
    }
    if (historyMessages && lastSeededRef.current !== selectedConversationId) {
      lastSeededRef.current = selectedConversationId;
      setUiMessages(historyMessages.map((m) => ({ key: m.id, role: m.role, content: m.content })));
    }
  }, [selectedConversationId, historyLoading, historyMessages]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView?.({ behavior: "smooth" });
  }, [uiMessages]);

  const startNewConversation = useCallback(() => {
    setConversationId(null);
    setSelectedConversationId(null);
    lastSeededRef.current = null;
    setUiMessages([]);
    setChatState("idle");
  }, []);

  const selectConversation = useCallback((id: string) => {
    setSelectedConversationId(id);
    setConversationId(id);
    setChatState("idle");
  }, []);

  const sendMessage = useCallback(
    (event: FormEvent) => {
      event.preventDefault();
      const trimmed = input.trim();
      if (!trimmed || processing) {
        return;
      }
      const userKey = `user-${Date.now()}`;
      const pendingKey = `assistant-${Date.now() + 1}`;
      setUiMessages((prev) => [
        ...prev,
        { key: userKey, role: "user", content: trimmed },
        { key: pendingKey, role: "assistant", content: "", pending: true },
      ]);
      setInput("");
      setChatState("processing");

      chatMutation.mutate(
        { message: trimmed, conversationId, context },
        {
          onSuccess: (res) => {
            setConversationId(res.conversationId);
            setSelectedConversationId(res.conversationId);
            setChatState("success");
            queryClient.invalidateQueries({
              queryKey: ["ai", "conversations", res.conversationId, "actions"],
            });
            setUiMessages((prev) =>
              prev.map((m) =>
                m.key === pendingKey
                  ? {
                      key: `answer-${res.conversationId}-${Date.now()}`,
                      role: "assistant",
                      content: res.message,
                    }
                  : m,
              ),
            );
          },
          onError: (error: Error) => {
            setChatState("error");
            setUiMessages((prev) =>
              prev.map((m) =>
                m.key === pendingKey
                  ? {
                      key: `error-${Date.now()}`,
                      role: "assistant",
                      content: aiErrorMessage(error),
                      error: true,
                    }
                  : m,
              ),
            );
          },
        },
      );
    },
    [chatMutation, context, conversationId, input, processing, queryClient],
  );

  const runAnalysis = useCallback(() => {
    if (analyzing || !context) {
      return;
    }
    setAnalysis(null);
    setAnalysisError(null);
    analyzeMutation.mutate(
      { question: "Analise este registro e sugira próximas ações.", context },
      {
        onSuccess: (res) => {
          setAnalysis(res);
          setAnalysisError(null);
        },
        onError: (err: Error) => {
          setAnalysis(null);
          setAnalysisError(aiAnalysisErrorMessage(err));
        },
      },
    );
  }, [analyzing, analyzeMutation, context]);

  if (!enabled) {
    return (
      <Card className="mx-auto mt-10 max-w-lg">
        <CardContent className="flex flex-col items-center gap-3 py-12 text-center">
          <Sparkles className="h-10 w-10 text-muted-foreground" />
          <p className="font-semibold">Assistente de IA indisponível</p>
          <p className="text-sm text-muted-foreground">
            Você não tem permissão para usar o assistente de IA ({`ai:chat`}). Contate um
            administrador se acredita que isso é um erro.
          </p>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="flex h-[calc(100vh-8rem)] flex-col gap-4 lg:flex-row">
      {/* Histórico - desktop */}
      <aside className="hidden w-72 shrink-0 flex-col overflow-hidden rounded-lg border bg-card lg:flex">
        <AiConversationList
          conversations={conversations}
          selectedId={selectedConversationId}
          loading={conversationsLoading}
          onSelect={selectConversation}
          onNewConversation={startNewConversation}
        />
      </aside>

      {/* Histórico - mobile */}
      <div className="flex flex-col gap-2 lg:hidden">
        <div className="flex items-center justify-between gap-2">
          <p className="text-sm font-semibold">Conversas</p>
          <Button variant="outline" size="sm" onClick={startNewConversation}>
            <Plus className="mr-1 h-4 w-4" />
            Nova conversa
          </Button>
        </div>
        <ScrollArea className="max-h-28">
          <div className="flex gap-2 pb-1">
            {conversations.map((conversation) => {
              const active = conversation.id === selectedConversationId;
              return (
                <button
                  key={conversation.id}
                  type="button"
                  onClick={() => selectConversation(conversation.id)}
                  className={cn(
                    "shrink-0 rounded-full border px-3 py-1 text-xs transition-colors",
                    active
                      ? "border-primary bg-primary text-primary-foreground"
                      : "bg-card hover:bg-accent",
                  )}
                >
                  {conversation.title || "Conversa"}
                </button>
              );
            })}
          </div>
        </ScrollArea>
      </div>

      {/* Área do chat */}
      <Card className="flex min-h-0 flex-1 flex-col overflow-hidden">
        <div className="flex items-center justify-between gap-3 border-b px-4 py-3">
          <div className="flex items-center gap-2">
            <Sparkles className="h-5 w-5 text-primary" />
            <div>
              <p className="font-semibold leading-tight">Assistente de IA</p>
              <p className="text-xs text-muted-foreground">
                {context
                  ? `${context.screen} • ${context.route}`
                  : "Converse com seus dados do CRM"}
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <TooltipProvider delayDuration={200}>
              <Tooltip>
                <TooltipTrigger asChild>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={runAnalysis}
                    disabled={analyzing || !context}
                    aria-label={
                      context
                        ? "Analisar o registro atual"
                        : "Analisar o registro atual (nenhum registro em foco)"
                    }
                  >
                    {analyzing ? (
                      <Loader2 className="mr-1 h-4 w-4 animate-spin" />
                    ) : (
                      <BrainCircuit className="mr-1 h-4 w-4" />
                    )}
                    Analisar
                  </Button>
                </TooltipTrigger>
                {!context && (
                  <TooltipContent>
                    Nenhum registro em foco. Abra uma tela de registro para analisar.
                  </TooltipContent>
                )}
              </Tooltip>
            </TooltipProvider>
            <Button variant="outline" size="sm" onClick={startNewConversation}>
              <Plus className="mr-1 h-4 w-4" />
              Nova conversa
            </Button>
          </div>
        </div>

        <ScrollArea className="min-h-0 flex-1">
          <div className="space-y-3 p-4">
            {uiMessages.length === 0 && (
              <p className="py-10 text-center text-sm text-muted-foreground">
                {selectedConversationId
                  ? "Carregando conversa..."
                  : "Olá! Pergunte sobre seus clientes, oportunidades, contatos e atividades do CRM."}
              </p>
            )}
            {uiMessages.map((message) => (
              <AiMessageBubble
                key={message.key}
                role={message.role}
                content={message.content}
                error={message.error}
                pending={message.pending}
              />
            ))}
            {conversationActions.length > 0 && (
              <div className="space-y-2 pt-1" aria-label="Acoes do assistente">
                {conversationActions.map((action) => (
                  <AiActionProposalCard key={action.id} action={action} />
                ))}
              </div>
            )}
            {(analyzing || analysis || analysisError) && (
              <div className="pt-1">
                <AiAnalysisCard
                  analysis={analysis}
                  loading={analyzing}
                  error={analysisError}
                />
              </div>
            )}
            <div ref={bottomRef} />
          </div>
        </ScrollArea>

        <form onSubmit={sendMessage} className="border-t p-3">
          <div className="flex items-end gap-2">
            <Textarea
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder="Digite sua mensagem para o assistente..."
              className="max-h-40 min-h-10 flex-1 resize-none"
              onKeyDown={(e) => {
                if (e.key === "Enter" && !e.shiftKey) {
                  e.preventDefault();
                  sendMessage(e);
                }
              }}
              disabled={processing}
              aria-label="Mensagem para o assistente"
            />
            <Button
              type="submit"
              size="icon"
              disabled={processing || !input.trim()}
              aria-label="Enviar mensagem"
            >
              {processing ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <Send className="h-4 w-4" />
              )}
            </Button>
          </div>
          {processing && (
            <p className="mt-2 flex items-center gap-2 text-xs text-muted-foreground">
              <Sparkles className="h-3.5 w-3.5 animate-pulse" />
              IA está analisando... Consultando dados do CRM quando necessário.
            </p>
          )}
        </form>
      </Card>
    </div>
  );
}
