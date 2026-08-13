"use client";

import {
  createContext,
  useContext,
  useCallback,
  type ReactNode,
} from "react";
import { usePathname } from "next/navigation";
import { useMe } from "./useAuthMutations";
import { loginWithGateway, logoutWithGateway } from "@/lib/gateway-auth";
import { isPublicPathname } from "@/lib/middleware-auth";
import type { User } from "../types/auth.types";

type AuthContextType = {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  logout: () => void;
  loginKeycloak: (redirectPath?: string) => Promise<void>;
  /** Roles OIDC (identidade) — não carregadas no browser (BFF). UX apenas. */
  roles: string[];
  /**
   * Permissões de negócio (CRM) para UX (menus/botões). Vindas do CurrentUser
   * da aplicação via /auth/me (Sprint 9), re-derivadas a cada Company Switcher.
   * Autorização real é sempre do backend (@PreAuthorize + RLS).
   */
  permissions: string[];
};

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const pathname = usePathname();

  // `/auth/me` apenas em páginas não-públicas — evita chamada/redirecionamento
  // desnecessário na tela de login (a sessão é validada pelo gateway).
  const enabled = !isPublicPathname(pathname);
  const { data, isLoading: isMeLoading } = useMe(enabled);

  // Deriva a identidade DIRETAMENTE do resultado da query (`data ?? null`).
  // Antes, `user` era estado copiado num `useEffect` que rodava DEPOIS do
  // render em que a query concluía: nesse render intermediário `isLoading`
  // já era false mas `isAuthenticated` ainda era false — e o ProtectedRoute
  // (efeito de componente filho, que roda antes) via `!isLoading &&
  // !isAuthenticated` e disparava `router.push("/login")` logo após um
  // `/auth/me` BEM-SUCEDIDO. Isso fazia todo login cair em /login (com o
  // Dashboard acessível via Voltar). Agora não existe essa janela: quando a
  // query resolve com sucesso, `isAuthenticated` já é true no MESMO render.
  const user = data ?? null;
  const isAuthenticated = !!user;

  const logout = useCallback(() => {
    logoutWithGateway();
  }, []);

  const loginKeycloak = useCallback(async (redirectPath?: string) => {
    // Sempre via Access Gateway (`/auth/authorize`) — o fluxo OIDC é 100% no
    // servidor; nunca montar URL de autorização manualmente no browser.
    loginWithGateway(redirectPath);
  }, []);

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: !!user,
        isLoading: isMeLoading,
        logout,
        loginKeycloak,
        roles: user?.roles ?? [],
        permissions: user?.permissions ?? [],
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
