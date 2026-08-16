"use client";

import { useState } from "react";
import { Card } from "@/components/ui/card";
import { ConversationList } from "@/features/omnichannel/components/ConversationList";
import { ChatThread } from "@/features/omnichannel/components/ChatThread";
import {
  useConversation,
  useConversations,
  useMarkRead,
  useOmnichannelPermissions,
  useSendMessage,
} from "@/features/omnichannel/hooks/useOmnichannel";
import type { Conversation } from "@/features/omnichannel/types/omnichannel.types";

export default function InboxPage() {
  const { data: page } = useConversations();
  const [selected, setSelected] = useState<Conversation | null>(null);

  const conversation = useConversation(selected?.id ?? null);
  const sendMessage = useSendMessage();
  const markRead = useMarkRead();
  const { canSend } = useOmnichannelPermissions();

  const handleSelect = (c: Conversation) => {
    setSelected(c);
    if (c.unreadCount > 0) {
      markRead.mutate(c.id);
    }
  };

  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Inbox</h1>
        <p className="text-sm text-muted-foreground">
          Atenda conversas de WhatsApp em um só lugar.
        </p>
      </div>

      <Card className="h-[calc(100vh-220px)] overflow-hidden">
        <div className="grid h-full grid-cols-1 md:grid-cols-[300px_1fr]">
          <div className="border-r">
            <ConversationList
              conversations={page?.content ?? []}
              isLoading={!page}
              activeId={selected?.id ?? null}
              onSelect={handleSelect}
            />
          </div>
          <div>
            <ChatThread
              detail={conversation.data}
              isLoading={conversation.isLoading}
              canSend={canSend}
              sending={sendMessage.isPending}
              onSend={(body) => {
                if (selected)
                  sendMessage.mutate({ conversationId: selected.id, body });
              }}
            />
          </div>
        </div>
      </Card>
    </div>
  );
}
