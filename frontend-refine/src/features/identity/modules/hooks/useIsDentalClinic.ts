"use client";

import { useCompanyId } from "@/features/identity/auth/hooks/useCompanyId";
import { useCompanyModules } from "./useCompanyModules";

/**
 * Odontograma, dentes e faces só existem para clínicas de odontologia.
 * Enquanto o segmento carrega, responde `false` (nada odontológico aparece).
 */
export function useIsDentalClinic(): boolean {
  const companyId = useCompanyId();
  const { data } = useCompanyModules(companyId);
  return data?.segment === "DENTAL";
}
