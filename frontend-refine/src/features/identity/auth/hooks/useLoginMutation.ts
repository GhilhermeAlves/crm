import { useMutation } from "@tanstack/react-query";
import { loginWithCredentials } from "@/lib/auth-api";

interface LoginMutationVariables {
  email: string;
  password: string;
  redirect?: string;
}

export function useLoginMutation() {
  return useMutation({
    mutationFn: async ({ email, password }: LoginMutationVariables) => {
      return loginWithCredentials(email, password);
    },
    onSuccess: (data) => {
      // Token is returned but session is managed server-side via cookies
      // Frontend just needs to trigger a page refresh or router redirect
      // to complete the login flow
      window.location.href = "/dashboard";
    },
    onError: (error: Error) => {
      // Error handling done by component
      console.error("Login error:", error.message);
    },
  });
}
