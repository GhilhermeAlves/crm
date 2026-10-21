import { describe, it, expect, vi, beforeEach } from "vitest";

const { redirectMock } = vi.hoisted(() => ({ redirectMock: vi.fn() }));
vi.mock("next/navigation", () => ({ redirect: redirectMock }));

import MembersPage from "@/app/(dashboard)/members/page";
import InvitationsPage from "@/app/(dashboard)/invitations/page";
import SettingsUsersPage from "@/app/(dashboard)/settings/users/page";
import PermissionsPage from "@/app/(dashboard)/settings/permissions/page";
import NewUsersPage from "@/app/(dashboard)/settings/company/users/new/page";

/** Rotas antigas viraram redirects; links salvos precisam cair na tela unificada. */
describe("redirects das rotas antigas", () => {
  beforeEach(() => redirectMock.mockClear());

  it.each([
    ["/members", MembersPage, "/settings/company/users?tab=ativos"],
    ["/invitations", InvitationsPage, "/settings/company/users?tab=convites"],
    ["/settings/users", SettingsUsersPage, "/settings/company/users?tab=ativos"],
    ["/settings/company/users/new", NewUsersPage, "/settings/company/users"],
    ["/settings/permissions", PermissionsPage, "/settings/roles"],
  ])("%s → destino", (_route, Page, target) => {
    Page();
    expect(redirectMock).toHaveBeenCalledWith(target);
  });
});
