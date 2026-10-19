import { describe, it, expect, vi } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { ConversationList } from "@/features/communication/omnichannel/components/ConversationList";
import type { Conversation } from "@/features/communication/omnichannel/types/omnichannel.types";

const conversation = {
  id: "c1",
  externalPhone: "5511999990001",
  lastMessage: "Oi",
  lastMessageAt: null,
  unreadCount: 0,
} as unknown as Conversation;

describe("ConversationList", () => {
  it("ignora o contato sem abrir a conversa", () => {
    const onSelect = vi.fn();
    const onIgnore = vi.fn();
    render(
      <ConversationList
        conversations={[conversation]}
        isLoading={false}
        activeId={null}
        onSelect={onSelect}
        onIgnore={onIgnore}
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: "Ignorar contato" }));

    expect(onIgnore).toHaveBeenCalledWith(conversation);
    expect(onSelect).not.toHaveBeenCalled();
  });

  it("esconde a opção sem permissão", () => {
    render(
      <ConversationList conversations={[conversation]} isLoading={false} activeId={null} onSelect={vi.fn()} />,
    );

    expect(screen.queryByRole("button", { name: "Ignorar contato" })).toBeNull();
  });
});
