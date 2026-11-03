/** Remove todos os caracteres não numéricos. */
export function onlyDigits(value: string): string {
  return value.replace(/\D/g, "");
}

/** Máscara de CNPJ: 12.345.678/0001-90 — somente dígitos, máx. 14. */
export function maskCnpj(value: string): string {
  const d = onlyDigits(value).slice(0, 14);
  if (d.length <= 2) return d;
  if (d.length <= 5) return `${d.slice(0, 2)}.${d.slice(2)}`;
  if (d.length <= 8) return `${d.slice(0, 2)}.${d.slice(2, 5)}.${d.slice(5)}`;
  if (d.length <= 12) return `${d.slice(0, 2)}.${d.slice(2, 5)}.${d.slice(5, 8)}/${d.slice(8)}`;
  return `${d.slice(0, 2)}.${d.slice(2, 5)}.${d.slice(5, 8)}/${d.slice(8, 12)}-${d.slice(12)}`;
}

/** Máscara de CPF: 123.456.789-09 — somente dígitos, máx. 11. */
export function maskCpf(value: string): string {
  const d = onlyDigits(value).slice(0, 11);
  if (d.length <= 3) return d;
  if (d.length <= 6) return `${d.slice(0, 3)}.${d.slice(3)}`;
  if (d.length <= 9) return `${d.slice(0, 3)}.${d.slice(3, 6)}.${d.slice(6)}`;
  return `${d.slice(0, 3)}.${d.slice(3, 6)}.${d.slice(6, 9)}-${d.slice(9)}`;
}

/** Documento da empresa: até 11 dígitos formata como CPF, acima disso como CNPJ. */
export function maskCpfCnpj(value: string): string {
  return onlyDigits(value).length <= 11 ? maskCpf(value) : maskCnpj(value);
}

/**
 * Máscara de telefone: fixo (11) 9999-9999 ou celular (11) 99999-9999.
 * Somente dígitos, máx. 11.
 */
export function maskPhone(value: string): string {
  const d = onlyDigits(value).slice(0, 11);
  if (d.length <= 2) return d;
  if (d.length <= 6) return `(${d.slice(0, 2)}) ${d.slice(2)}`;
  if (d.length <= 10) return `(${d.slice(0, 2)}) ${d.slice(2, 6)}-${d.slice(6)}`;
  return `(${d.slice(0, 2)}) ${d.slice(2, 7)}-${d.slice(7)}`;
}

/** Máscara de CEP: 01001-000 — somente dígitos, máx. 8. */
export function maskCep(value: string): string {
  const d = onlyDigits(value).slice(0, 8);
  if (d.length <= 5) return d;
  return `${d.slice(0, 5)}-${d.slice(5)}`;
}

/**
 * Validação dos dígitos verificadores de um CNPJ (algoritmo oficial).
 * Aceita valor mascarado ou somente dígitos.
 */
export function isValidCnpj(value: string): boolean {
  const digits = onlyDigits(value);
  if (digits.length !== 14) return false;
  // Sequência de todos os dígitos iguais é inválida (ex.: 00.000.000/0000-00).
  if (/^(\d)\1{13}$/.test(digits)) return false;

  const fator1 = [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];
  let sum = 0;
  for (let i = 0; i < 12; i++) sum += Number(digits[i]) * fator1[i];
  const check1 = sum % 11 < 2 ? 0 : 11 - (sum % 11);

  const fator2 = [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];
  sum = 0;
  for (let i = 0; i < 13; i++) sum += Number(digits[i]) * fator2[i];
  const check2 = sum % 11 < 2 ? 0 : 11 - (sum % 11);

  return Number(digits[12]) === check1 && Number(digits[13]) === check2;
}

/** Validação dos dígitos verificadores de um CPF. Aceita valor mascarado ou só dígitos. */
export function isValidCpf(value: string): boolean {
  const digits = onlyDigits(value);
  if (digits.length !== 11) return false;
  if (/^(\d)\1{10}$/.test(digits)) return false;

  const check = (length: number) => {
    let sum = 0;
    for (let i = 0; i < length; i++) sum += Number(digits[i]) * (length + 1 - i);
    const rest = (sum * 10) % 11;
    return rest === 10 ? 0 : rest;
  };
  return check(9) === Number(digits[9]) && check(10) === Number(digits[10]);
}

/** Documento da empresa: CPF (11 dígitos) ou CNPJ (14 dígitos) válido. */
export function isValidCpfCnpj(value: string): boolean {
  const length = onlyDigits(value).length;
  return length === 11 ? isValidCpf(value) : isValidCnpj(value);
}
