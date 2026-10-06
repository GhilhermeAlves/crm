import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, waitFor } from "@testing-library/react";
import * as React from "react";
import AcceptInvitationPage from "@/app/(invitation)/invitations/accept/page";

const { replaceMock, searchState } = vi.hoisted(() => ({
  replaceMock: vi.fn(),
  searchState: { token: null as string | null },
}));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn(), replace: replaceMock }),
  useSearchParams: () => ({ get: (key: string) => (key === "token" ? searchState.token : null) }),
}));

vi.mock("@/components/layout/LoadingScreen", () => ({
  LoadingScreen: () => <div>loading</div>,
}));

describe("/invitations/accept (link legado)", () => {
  beforeEach(() => {
    replaceMock.mockReset();
  });

  it("redireciona para /convite/{token} sem processar o convite", async () => {
    searchState.token = "ABC123";
    render(<AcceptInvitationPage />);
    await waitFor(() => expect(replaceMock).toHaveBeenCalledWith("/convite/ABC123"));
  });

  it("sem token, volta ao início", async () => {
    searchState.token = null;
    render(<AcceptInvitationPage />);
    await waitFor(() => expect(replaceMock).toHaveBeenCalledWith("/crm"));
  });
});
