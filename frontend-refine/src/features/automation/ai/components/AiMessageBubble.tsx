"use client";

import { Sparkles, UserRound } from "lucide-react";
import { cn } from "@/lib/utils";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import type { AiChatRole } from "../types/ai.types";

type Props = {
  role: AiChatRole;
  content: string;
  /** Mensagem de erro amigável - nunca stack trace (AI-04 §22). */
  error?: boolean;
  /** Indicador de processamento da IA (AI-04 §13). */
  pending?: boolean;
};

/**
 * Bolha de mensagem do Assistente de IA. Diferencia USER (direita, cor
 * primária) de ASSISTANT (esquerda, neutra). Renderiza texto de forma segura
 * (whitespace-pre-wrap + break-words) - nunca executa HTML/markdown do LLM.
 */
export function AiMessageBubble({ role, content, error = false, pending = false }: Props) {
  const isUser = role === "user";

  return (
    <div className={cn("flex items-end gap-2", isUser ? "justify-end" : "justify-start")}>
      {!isUser && (
        <Avatar className="h-7 w-7">
          <AvatarFallback className="bg-primary/10 text-primary">
            {pending ? (
              <Sparkles className="h-3.5 w-3.5 animate-pulse" />
            ) : (
              <Sparkles className="h-3.5 w-3.5" />
            )}
          </AvatarFallback>
        </Avatar>
      )}
      <div
        className={cn(
          "max-w-[85%] rounded-2xl px-4 py-2.5 text-sm shadow-sm sm:max-w-[75%]",
          isUser
            ? "bg-primary text-primary-foreground"
            : error
              ? "border border-destructive/30 bg-destructive/5 text-destructive"
              : "bg-muted",
        )}
      >
        {pending ? (
          <span className="flex items-center gap-2 text-muted-foreground">
            <Sparkles className="h-4 w-4 animate-pulse" />
            IA está analisando...
          </span>
        ) : (
          <p className="whitespace-pre-wrap break-words">{content}</p>
        )}
      </div>
      {isUser && (
        <Avatar className="h-7 w-7">
          <AvatarFallback className="bg-primary text-primary-foreground">
            <UserRound className="h-3.5 w-3.5" />
          </AvatarFallback>
        </Avatar>
      )}
    </div>
  );
}
