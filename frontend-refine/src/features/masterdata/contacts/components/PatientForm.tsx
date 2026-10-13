"use client";

import { useCallback, useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { DateTimePicker } from "@/components/ui/date-time-picker";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { useContactRules } from "@/features/identity/tenants/hooks/useTenants";
import { formatCpf, formatPhone, formatCep, fetchCep } from "@/lib/format";
import {
  GENDER_OPTIONS,
  MARITAL_STATUS_OPTIONS,
  PROFESSIONAL_STATUS_OPTIONS,
  UF_OPTIONS,
  createPatientSchema,
  type PatientFormValues,
} from "../schemas/patient.schema";

const EMPTY_VALUES: PatientFormValues = {
  firstName: "",
  lastName: "",
  birthDate: "",
  cpf: "",
  rg: "",
  rgIssuer: "",
  gender: "",
  maritalStatus: "",
  professionalStatus: "",
  healthPlan: "",
  healthPlanNumber: "",
  recordNumber: "",
  referralSource: "",
  mobile: "",
  phone: "",
  email: "",
  cep: "",
  street: "",
  addressNumber: "",
  complement: "",
  neighborhood: "",
  city: "",
  state: "",
  hasGuardian: false,
  guardianName: "",
  guardianCpf: "",
  guardianPhone: "",
  notes: "",
};

export type PatientFormProps = {
  defaultValues?: Partial<PatientFormValues>;
  onSubmit: (values: PatientFormValues) => void;
  isLoading?: boolean;
  submitLabel?: string;
  onCancel?: () => void;
  serverError?: { field: keyof PatientFormValues; message: string } | null;
};

function OptionalLabel({ label }: { label: string }) {
  return (
    <div className="flex items-center justify-between">
      <FormLabel>{label}</FormLabel>
      <span className="text-xs text-muted-foreground">Opcional</span>
    </div>
  );
}

export function PatientForm({
  defaultValues,
  onSubmit,
  isLoading,
  submitLabel = "Salvar",
  onCancel,
  serverError,
}: PatientFormProps) {
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;
  const { data: contactRules } = useContactRules(companyId);
  const requireCpf = contactRules?.requireContactCpf ?? false;
  const [cepLoading, setCepLoading] = useState(false);

  const form = useForm<PatientFormValues>({
    resolver: zodResolver(createPatientSchema({ requireCpf })),
    defaultValues: { ...EMPTY_VALUES, ...defaultValues },
  });

  const hasGuardian = form.watch("hasGuardian");

  useEffect(() => {
    if (serverError) {
      form.setError(serverError.field, { type: "server", message: serverError.message });
    }
  }, [serverError, form]);

  const handleCepBlur = useCallback(
    async (cepValue: string) => {
      const digits = cepValue.replace(/\D/g, "");
      if (digits.length !== 8) return;
      setCepLoading(true);
      const result = await fetchCep(cepValue);
      setCepLoading(false);
      if (result) {
        form.setValue("street", result.logradouro);
        form.setValue("neighborhood", result.bairro);
        form.setValue("city", result.localidade);
        form.setValue("state", result.uf);
      }
    },
    [form],
  );

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-8">
        {/* Dados do contato */}
        <section className="space-y-4">
          <h2 className="text-lg font-medium">Dados do contato</h2>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <FormField
              control={form.control}
              name="firstName"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Nome</FormLabel>
                  <FormControl>
                    <Input placeholder="Digite o nome do contato" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="lastName"
              render={({ field }) => (
                <FormItem>
                  <OptionalLabel label="Sobrenome" />
                  <FormControl>
                    <Input placeholder="Digite o sobrenome" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <FormField
              control={form.control}
              name="birthDate"
              render={({ field }) => (
                <FormItem>
                  <OptionalLabel label="Data de Nasc." />
                  <FormControl>
                    <DateTimePicker
                      dateOnly
                      value={field.value}
                      onChange={field.onChange}
                      placeholder="Selecione a data"
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="cpf"
              render={({ field }) => (
                <FormItem>
                  {requireCpf ? <FormLabel>CPF</FormLabel> : <OptionalLabel label="CPF" />}
                  <FormControl>
                    <Input
                      placeholder="000.000.000-00"
                      value={field.value}
                      onChange={(e) => field.onChange(formatCpf(e.target.value))}
                      onBlur={() => form.trigger("cpf")}
                      maxLength={14}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <FormField
              control={form.control}
              name="rg"
              render={({ field }) => (
                <FormItem>
                  <OptionalLabel label="RG" />
                  <FormControl>
                    <Input placeholder="Digite o RG" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="rgIssuer"
              render={({ field }) => (
                <FormItem>
                  <OptionalLabel label="Emissor" />
                  <FormControl>
                    <Input placeholder="Órgão emissor" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <FormField
              control={form.control}
              name="gender"
              render={({ field }) => (
                <FormItem>
                  <OptionalLabel label="Sexo" />
                  <Select onValueChange={field.onChange} value={field.value}>
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder="Selecione" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      {GENDER_OPTIONS.map((opt) => (
                        <SelectItem key={opt.value} value={opt.value}>
                          {opt.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="maritalStatus"
              render={({ field }) => (
                <FormItem>
                  <OptionalLabel label="Estado Civil" />
                  <Select onValueChange={field.onChange} value={field.value}>
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder="Selecione" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      {MARITAL_STATUS_OPTIONS.map((opt) => (
                        <SelectItem key={opt.value} value={opt.value}>
                          {opt.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="professionalStatus"
              render={({ field }) => (
                <FormItem>
                  <OptionalLabel label="Situação" />
                  <Select onValueChange={field.onChange} value={field.value}>
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder="Selecione" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      {PROFESSIONAL_STATUS_OPTIONS.map((opt) => (
                        <SelectItem key={opt.value} value={opt.value}>
                          {opt.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <FormField
              control={form.control}
              name="healthPlan"
              render={({ field }) => (
                <FormItem>
                  <OptionalLabel label="Plano de saúde" />
                  <FormControl>
                    <Input placeholder="Nome do plano" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="healthPlanNumber"
              render={({ field }) => (
                <FormItem>
                  <OptionalLabel label="Nº do plano" />
                  <FormControl>
                    <Input placeholder="Carteirinha" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="recordNumber"
              render={({ field }) => (
                <FormItem>
                  <OptionalLabel label="Nº do prontuário" />
                  <FormControl>
                    <Input placeholder="Ex.: 000123" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="referralSource"
              render={({ field }) => (
                <FormItem>
                  <OptionalLabel label="Como conheceu" />
                  <FormControl>
                    <Input placeholder="Indicação, redes sociais…" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </div>
        </section>

        {/* Contato */}
        <section className="space-y-4 border-t pt-6">
          <h2 className="text-lg font-medium">Contato</h2>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <FormField
              control={form.control}
              name="mobile"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Celular</FormLabel>
                  <FormControl>
                    <Input
                      placeholder="(00) 99999-9999"
                      value={field.value}
                      onChange={(e) => field.onChange(formatPhone(e.target.value))}
                      maxLength={15}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="phone"
              render={({ field }) => (
                <FormItem>
                  <OptionalLabel label="Telefone fixo" />
                  <FormControl>
                    <Input
                      placeholder="(00) 3333-4444"
                      value={field.value}
                      onChange={(e) => field.onChange(formatPhone(e.target.value))}
                      maxLength={15}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </div>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <FormField
              control={form.control}
              name="email"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>E-mail</FormLabel>
                  <FormControl>
                    <Input
                      type="email"
                      placeholder="cliente@empresa.com"
                      {...field}
                      onBlur={() => form.trigger("email")}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </div>
        </section>

        {/* Endereço */}
        <section className="space-y-4 border-t pt-6">
          <h2 className="text-lg font-medium">Endereço</h2>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <FormField
              control={form.control}
              name="cep"
              render={({ field }) => (
                <FormItem>
                  <OptionalLabel label="CEP" />
                  <FormControl>
                    <div className="relative">
                      <Input
                        placeholder="00000-000"
                        value={field.value}
                        onChange={(e) => field.onChange(formatCep(e.target.value))}
                        onBlur={() => {
                          form.trigger("cep");
                          handleCepBlur(field.value ?? "");
                        }}
                        maxLength={9}
                      />
                      {cepLoading && (
                        <Loader2 className="absolute right-3 top-1/2 h-4 w-4 -translate-y-1/2 animate-spin text-muted-foreground" />
                      )}
                    </div>
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="street"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Logradouro</FormLabel>
                  <FormControl>
                    <Input placeholder="Rua, Avenida..." {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </div>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <FormField
              control={form.control}
              name="addressNumber"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Número</FormLabel>
                  <FormControl>
                    <Input placeholder="Nº" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="complement"
              render={({ field }) => (
                <FormItem>
                  <OptionalLabel label="Complemento" />
                  <FormControl>
                    <Input placeholder="Apto, Bloco..." {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </div>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <FormField
              control={form.control}
              name="neighborhood"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Bairro</FormLabel>
                  <FormControl>
                    <Input {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="city"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Cidade</FormLabel>
                  <FormControl>
                    <Input {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="state"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Estado</FormLabel>
                  <Select onValueChange={field.onChange} value={field.value}>
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder="UF" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      {UF_OPTIONS.map((uf) => (
                        <SelectItem key={uf} value={uf}>
                          {uf}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />
          </div>
        </section>

        {/* Contato tem responsável */}
        <section className="space-y-4 border-t pt-6">
          <h2 className="text-lg font-medium">Contato tem responsável</h2>
          <FormField
            control={form.control}
            name="hasGuardian"
            render={({ field }) => (
              <FormItem className="flex items-center gap-3">
                <FormControl>
                  <label className="flex cursor-pointer items-center gap-2">
                    <Checkbox
                      checked={field.value === true}
                      onCheckedChange={(checked) => field.onChange(checked === true)}
                    />
                    <span className="text-sm">Sim</span>
                  </label>
                </FormControl>
              </FormItem>
            )}
          />

          {hasGuardian && (
            <div className="space-y-4 rounded-lg border p-4">
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <FormField
                  control={form.control}
                  name="guardianName"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Nome do responsável</FormLabel>
                      <FormControl>
                        <Input placeholder="Nome completo" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="guardianCpf"
                  render={({ field }) => (
                    <FormItem>
                      <OptionalLabel label="CPF do responsável" />
                      <FormControl>
                        <Input
                          placeholder="000.000.000-00"
                          value={field.value}
                          onChange={(e) => field.onChange(formatCpf(e.target.value))}
                          maxLength={14}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <FormField
                  control={form.control}
                  name="guardianPhone"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Telefone do responsável</FormLabel>
                      <FormControl>
                        <Input
                          placeholder="(00) 99999-9999"
                          value={field.value}
                          onChange={(e) => field.onChange(formatPhone(e.target.value))}
                          maxLength={15}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>
            </div>
          )}
        </section>

        {/* Observações */}
        <section className="space-y-4 border-t pt-6">
          <h2 className="text-lg font-medium">Observações</h2>
          <FormField
            control={form.control}
            name="notes"
            render={({ field }) => (
              <FormItem>
                <OptionalLabel label="Notas" />
                <FormControl>
                  <Textarea
                    placeholder="Observações sobre o contato"
                    className="min-h-[100px]"
                    {...field}
                  />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
        </section>

        {/* Actions */}
        <div className="flex justify-end gap-3 border-t pb-4 pt-6">
          {onCancel && (
            <Button type="button" variant="outline" onClick={onCancel}>
              Cancelar
            </Button>
          )}
          <Button type="submit" disabled={isLoading}>
            {isLoading ? "Salvando…" : submitLabel}
          </Button>
        </div>
      </form>
    </Form>
  );
}
