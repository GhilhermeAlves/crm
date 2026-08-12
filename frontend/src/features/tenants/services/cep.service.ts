import { onlyDigits } from "@/lib/masks";

export type ViaCepAddress = {
  street: string;
  complement: string;
  neighborhood: string;
  city: string;
  state: string;
};

type ViaCepResponse = {
  logradouro?: string;
  complemento?: string;
  bairro?: string;
  localidade?: string;
  uf?: string;
  erro?: boolean;
};

/**
 * Consulta a API pública ViaCEP. Retorna o endereço normalizado quando o CEP
 * é encontrado, `null` quando não existe (erro: true), e lança exceção em
 * falhas de rede/HTTP (para o chamador decidir sem apagar dados preenchidos).
 */
export async function fetchAddressByCep(cep: string): Promise<ViaCepAddress | null> {
  const digits = onlyDigits(cep);
  if (digits.length !== 8) return null;

  const response = await fetch(`https://viacep.com.br/ws/${digits}/json/`);
  if (!response.ok) throw new Error("Falha ao consultar o CEP");

  const data = (await response.json()) as ViaCepResponse;
  if (data.erro) return null;

  return {
    street: data.logradouro ?? "",
    complement: data.complemento ?? "",
    neighborhood: data.bairro ?? "",
    city: data.localidade ?? "",
    state: data.uf ?? "",
  };
}