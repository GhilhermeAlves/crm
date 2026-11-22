"use client";

import { useMemo, useState } from "react";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { ConversationList } from "@/features/communication/omnichannel/components/ConversationList";
import { ChatThread } from "@/features/communication/omnichannel/components/ChatThread";
import {
  useAddIgnoredContact,
  useChannels,
  useConversation,
  useConversations,
  useIgnoredContacts,
  useMarkRead,
  useOmnichannelPermissions,
  useSendMessage,
} from "@/features/communication/omnichannel/hooks/useOmnichannel";
import type {
  ChannelType,
  Conversation,
} from "@/features/communication/omnichannel/types/omnichannel.types";

const CHANNEL_LABEL: Record<ChannelType, string> = { WHATSAPP: "WhatsApp", INSTAGRAM: "Instagram" };

const digits = (phone: string) => phone.replace(/\D/g, "");

export default function InboxPage() {
  const { data: page } = useConversations();
  const [selected, setSelected] = useState<Conversation | null>(null);
  const [toIgnore, setToIgnore] = useState<Conversation | null>(null);
  const [channelFilter, setChannelFilter] = useState<ChannelType>("WHATSAPP");
  const { data: channels = [] } = useChannels();

  const conversation = useConversation(selected?.id ?? null);
  const sendMessage = useSendMessage();
  const markRead = useMarkRead();
  const { canSend, canUpdate } = useOmnichannelPermissions();
  const { data: ignored = [] } = useIgnoredContacts();
  const addIgnored = useAddIgnoredContact();

  // Contatos ignorados saem da lista; voltam ao remover em Canais → Contatos ignorados.
  const conversations = useMemo(() => {
    const ignoredPhones = new Set(ignored.map((c) => digits(c.phone)));
    const typeByChannel = new Map(channels.map((ch) => [ch.id, ch.type]));
    return (page?.content ?? []).filter(
      (c) =>
        !ignoredPhones.has(digits(c.externalPhone)) &&
        typeByChannel.get(c.channelId) === channelFilter,
    );
  }, [page, ignored, channels, channelFilter]);

  const handleSelect = (c: Conversation) => {
    setSelected(c);
    if (c.unreadCount > 0) {
      markRead.mutate(c.id);
    }
  };

  const handleConfirmIgnore = () => {
    if (!toIgnore) return;
    addIgnored.mutate(
      { phone: toIgnore.externalPhone },
      {
        onSuccess: () => {
          if (selected?.id === toIgnore.id) setSelected(null);
          setToIgnore(null);
        },
      },
    );
  };

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">
            Inbox — {CHANNEL_LABEL[channelFilter]}
          </h1>
          <p className="text-sm text-muted-foreground">
            Conversas do {CHANNEL_LABEL[channelFilter]}.
          </p>
        </div>
        <Button
          variant="outline"
          onClick={() => {
            setChannelFilter(channelFilter === "WHATSAPP" ? "INSTAGRAM" : "WHATSAPP");
            setSelected(null);
          }}
        >
          Ir para {CHANNEL_LABEL[channelFilter === "WHATSAPP" ? "INSTAGRAM" : "WHATSAPP"]}
        </Button>
      </div>

      <Card className="h-[calc(100dvh-220px)] overflow-hidden">
        <div className="grid h-full grid-cols-1 grid-rows-2 md:grid-cols-[300px_1fr] md:grid-rows-1">
          <div className="min-h-0 overflow-hidden border-r">
            <ConversationList
              conversations={conversations}
              isLoading={!page}
              activeId={selected?.id ?? null}
              onSelect={handleSelect}
              onIgnore={canUpdate ? setToIgnore : undefined}
            />
          </div>
          <div className="min-h-0 overflow-hidden">
            <ChatThread
              detail={conversation.data}
              isLoading={conversation.isLoading}
              canSend={canSend}
              sending={sendMessage.isPending}
              onIgnore={canUpdate && selected ? () => setToIgnore(selected) : undefined}
              onSend={(body) => {
                if (selected) sendMessage.mutate({ conversationId: selected.id, body });
              }}
            />
          </div>
        </div>
      </Card>

      <ConfirmDialog
        open={toIgnore !== null}
        onOpenChange={(open) => !open && setToIgnore(null)}
        title="Ignorar contato?"
        description={`A IA deixa de responder ${toIgnore?.externalPhone ?? ""} e as novas mensagens dele não serão gravadas. A conversa sai do Inbox. Para desfazer, remova o número em Canais → Contatos ignorados.`}
        confirmLabel="Ignorar"
        variant="destructive"
        onConfirm={handleConfirmIgnore}
        isLoading={addIgnored.isPending}
      />
    </div>
  );
}
