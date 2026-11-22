"use client";

import { useMemo, useState } from "react";
import { Card } from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
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

type ChannelFilter = "ALL" | ChannelType;

const digits = (phone: string) => phone.replace(/\D/g, "");

export default function InboxPage() {
  const { data: page } = useConversations();
  const [selected, setSelected] = useState<Conversation | null>(null);
  const [toIgnore, setToIgnore] = useState<Conversation | null>(null);
  const [channelFilter, setChannelFilter] = useState<ChannelFilter>("ALL");
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
        (channelFilter === "ALL" || typeByChannel.get(c.channelId) === channelFilter),
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
          <h1 className="text-2xl font-bold tracking-tight">Inbox</h1>
          <p className="text-sm text-muted-foreground">
            Atenda conversas de WhatsApp e Instagram em um só lugar.
          </p>
        </div>
        <Select
          value={channelFilter}
          onValueChange={(v) => {
            setChannelFilter(v as ChannelFilter);
            setSelected(null);
          }}
        >
          <SelectTrigger className="w-44" aria-label="Canal">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">Todos os canais</SelectItem>
            <SelectItem value="WHATSAPP">WhatsApp</SelectItem>
            <SelectItem value="INSTAGRAM">Instagram</SelectItem>
          </SelectContent>
        </Select>
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
