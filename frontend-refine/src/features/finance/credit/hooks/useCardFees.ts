import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import api from "@/lib/api";
import { useMutationDefaults } from "@/lib/query/mutation-utils";
import { MAX_INSTALLMENTS, type FeeRates } from "../utils/creditSimulation";

export type CardFeeRate = { installments: number; ratePercent: number };

export type CardFeePreset = {
  id: string;
  name: string;
  estimated: boolean;
  checkedAt: string | null;
  sourceNote: string | null;
  active: boolean;
  rates: CardFeeRate[];
};

export type CardFeePresetInput = Omit<CardFeePreset, "id">;

const keys = {
  rates: (companyId: string) => ["card-fees", companyId] as const,
  presets: ["card-fee-presets"] as const,
  adminPresets: ["card-fee-presets", "admin"] as const,
};

const ratesUrl = (companyId: string) => `/companies/${companyId}/card-fees`;
const ADMIN_PRESETS = "/admin/card-fee-presets";

export function ratesToMap(list: CardFeeRate[] | undefined): FeeRates {
  const map: FeeRates = {};
  for (const r of list ?? []) map[r.installments] = Number(r.ratePercent);
  return map;
}

/** Converte o mapa em lista 1..12; devolve null se faltar alguma taxa. */
export function mapToRates(map: FeeRates): CardFeeRate[] | null {
  const list: CardFeeRate[] = [];
  for (let n = 1; n <= MAX_INSTALLMENTS; n++) {
    const rate = map[n];
    if (rate == null || Number.isNaN(rate)) return null;
    list.push({ installments: n, ratePercent: rate });
  }
  return list;
}

/** Taxas da maquininha da clínica (lista vazia se nunca cadastrou). */
export function useCardFees(companyId: string | null) {
  return useQuery({
    queryKey: keys.rates(companyId ?? ""),
    queryFn: async () => (await api.get<CardFeeRate[]>(ratesUrl(companyId as string))).data,
    enabled: Boolean(companyId),
  });
}

export function useSaveCardFees(companyId: string | null) {
  const queryClient = useQueryClient();
  const { onError } = useMutationDefaults();
  return useMutation({
    mutationFn: async (rates: CardFeeRate[]) =>
      (await api.put<CardFeeRate[]>(ratesUrl(companyId as string), { rates })).data,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: keys.rates(companyId ?? "") }),
    onError: onError({ errorMessage: "Erro ao salvar as taxas do cartão" }),
  });
}

/** Modelos de taxa ativos (maquininhas do mercado). */
export function useCardFeePresets() {
  return useQuery({
    queryKey: keys.presets,
    queryFn: async () => (await api.get<CardFeePreset[]>("/card-fee-presets")).data,
    staleTime: 5 * 60 * 1000,
  });
}

/** Todos os modelos, inclusive inativos (só dono do CRM). */
export function useAdminCardFeePresets() {
  return useQuery({
    queryKey: keys.adminPresets,
    queryFn: async () => (await api.get<CardFeePreset[]>(ADMIN_PRESETS)).data,
  });
}

export function useSaveCardFeePreset() {
  const queryClient = useQueryClient();
  const { onError } = useMutationDefaults();
  return useMutation({
    mutationFn: async ({ id, input }: { id?: string; input: CardFeePresetInput }) =>
      id
        ? (await api.put<CardFeePreset>(`${ADMIN_PRESETS}/${id}`, input)).data
        : (await api.post<CardFeePreset>(ADMIN_PRESETS, input)).data,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: keys.presets }),
    onError: onError({ errorMessage: "Erro ao salvar o modelo de taxa" }),
  });
}

export function useDeactivateCardFeePreset() {
  const queryClient = useQueryClient();
  const { onError } = useMutationDefaults();
  return useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`${ADMIN_PRESETS}/${id}`);
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: keys.presets }),
    onError: onError({ errorMessage: "Erro ao desativar o modelo de taxa" }),
  });
}
