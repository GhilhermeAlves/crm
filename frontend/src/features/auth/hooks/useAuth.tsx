"use client";

import { createContext, useContext, useCallback, useState, useEffect, type ReactNode } from "react";
import { useMe, useLogout } from "./useAuthMutations";
import { TokenManager } from "@/store/token-manager";
import type { User } from "../types/auth.types";

type AuthContextType = {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  logout: () => void;
  roles: string[];
  permissions: string[];
};

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const { data, isLoading: isMeLoading, isError } = useMe();
  const logoutMutation = useLogout();

  useEffect(() => {
    if (data) {
      setUser(data);
    } else if (!isMeLoading && (isError || !TokenManager.hasTokens())) {
      TokenManager.clearTokens();
      setUser(null);
    }
  }, [data, isMeLoading, isError]);

  const logout = useCallback(() => {
    logoutMutation.mutate();
  }, [logoutMutation]);

  const roles = TokenManager.getRoles();
  const permissions = TokenManager.getPermissions();

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: !!user,
        isLoading: isMeLoading,
        logout,
        roles,
        permissions,
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
