import { QueryClient, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { toHttpError } from "@/lib/http-error";

export interface MutationOptions<TData = unknown, TVariables = unknown> {
  successMessage?: string | ((data: TData, variables: TVariables) => string);
  invalidateKeys?: unknown[][];
  extraInvalidate?: (queryClient: QueryClient, data: TData, variables: TVariables) => void;
  errorMessage?: string;
  silentError?: boolean;
}

export function useMutationDefaults<TData = unknown, TVariables = unknown, TError = Error>() {
  const queryClient = useQueryClient();
  return {
    queryClient,
    onSuccess:
      (opts: MutationOptions<TData, TVariables>) => (data: TData, variables: TVariables) => {
        opts.invalidateKeys?.forEach((key) => queryClient.invalidateQueries({ queryKey: key }));
        opts.extraInvalidate?.(queryClient, data, variables);
        const message =
          typeof opts.successMessage === "function"
            ? opts.successMessage(data, variables)
            : opts.successMessage;
        if (message) toast.success(message);
      },
    onError: (opts: MutationOptions<TData, TVariables>) => (error: TError) => {
      if (opts.silentError) return;
      toast.error(toHttpError(error, opts.errorMessage).message);
    },
  };
}
