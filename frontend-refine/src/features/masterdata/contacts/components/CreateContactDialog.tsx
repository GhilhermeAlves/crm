"use client";

import { useMemo } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import type { Contact, UpdateContactRequest } from "../types/contact.types";
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetFooter } from "@/components/ui/sheet";
import { Button } from "@/components/ui/button";
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
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { DateTimePicker } from "@/components/ui/date-time-picker";
import { formatCpf, validateCpf, formatPhone } from "@/lib/format";

const GENDER_OPTIONS = [
  { value: "MASCULINO", label: "Masculino" },
  { value: "FEMININO", label: "Feminino" },
  { value: "OUTRO", label: "Outro" },
] as const;

const MARITAL_STATUS_OPTIONS = [
  { value: "SOLTEIRO", label: "Solteiro(a)" },
  { value: "CASADO", label: "Casado(a)" },
  { value: "DIVORCIADO", label: "Divorciado(a)" },
  { value: "VIUVO", label: "Viúvo(a)" },
  { value: "UNIAO_ESTAVEL", label: "União Estável" },
] as const;

const PROFESSIONAL_STATUS_OPTIONS = [
  { value: "ATIVO", label: "Ativo" },
  { value: "INATIVO", label: "Inativo" },
  { value: "PROSPECTO", label: "Prospecto" },
  { value: "BLOQUEADO", label: "Bloqueado" },
] as const;

const editContactSchema = z.object({
  firstName: z.string().min(1, "Nome é obrigatório").max(100),
  lastName: z.string().max(100).optional(),
  email: z
    .string()
    .max(255)
    .optional()
    .refine(
      (val) => !val || val === "" || /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(val),
      "E-mail inválido",
    ),
  phone: z.string().max(20).optional(),
  mobile: z.string().max(20).optional(),
  notes: z.string().max(500).optional(),
  birthDate: z.string().optional(),
  cpf: z
    .string()
    .optional()
    .refine(
      (val) => !val || val.replace(/\D/g, "").length === 0 || validateCpf(val),
      "CPF inválido",
    ),
  rg: z.string().max(20).optional(),
  rgIssuer: z.string().max(20).optional(),
  gender: z.string().max(20).optional(),
  maritalStatus: z.string().max(30).optional(),
  professionalStatus: z.string().max(20).optional(),
});

type FormValues = z.infer<typeof editContactSchema>;

type Props = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  isLoading?: boolean;
  contact?: Contact | null;
  onSubmit: (values: UpdateContactRequest) => void;
};

const emptyValues: FormValues = {
  firstName: "",
  lastName: "",
  email: "",
  phone: "",
  mobile: "",
  notes: "",
  birthDate: "",
  cpf: "",
  rg: "",
  rgIssuer: "",
  gender: "",
  maritalStatus: "",
  professionalStatus: "",
};

export function CreateContactDialog({ open, onOpenChange, isLoading, contact, onSubmit }: Props) {
  const isEdit = !!contact;

  const formValues = useMemo<FormValues>(
    () =>
      contact
        ? {
            firstName: contact.firstName ?? "",
            lastName: contact.lastName ?? "",
            email: contact.email ?? "",
            phone: contact.phone ? formatPhone(contact.phone) : "",
            mobile: contact.mobile ? formatPhone(contact.mobile) : "",
            notes: contact.notes ?? "",
            birthDate: contact.birthDate ?? "",
            cpf: contact.cpf ? formatCpf(contact.cpf) : "",
            rg: contact.rg ?? "",
            rgIssuer: contact.rgIssuer ?? "",
            gender: contact.gender ?? "",
            maritalStatus: contact.maritalStatus ?? "",
            professionalStatus: contact.professionalStatus ?? "",
          }
        : emptyValues,
    [contact],
  );

  const form = useForm<FormValues>({
    resolver: zodResolver(editContactSchema),
    values: formValues,
  });

  const handleSubmit = (values: FormValues) => {
    onSubmit({
      firstName: values.firstName,
      lastName: values.lastName || undefined,
      email: values.email || undefined,
      phone: values.phone ? values.phone.replace(/\D/g, "") : undefined,
      mobile: values.mobile ? values.mobile.replace(/\D/g, "") : undefined,
      notes: values.notes || undefined,
      birthDate: values.birthDate || undefined,
      cpf: values.cpf ? values.cpf.replace(/\D/g, "") : undefined,
      rg: values.rg || undefined,
      rgIssuer: values.rgIssuer || undefined,
      gender: values.gender || undefined,
      maritalStatus: values.maritalStatus || undefined,
      professionalStatus: values.professionalStatus || undefined,
    });
  };

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent side="right" className="flex flex-col overflow-y-auto sm:max-w-2xl">
        <SheetHeader>
          <SheetTitle>{isEdit ? "Editar contato" : "Novo contato"}</SheetTitle>
        </SheetHeader>
        <Form {...form}>
          <form
            onSubmit={form.handleSubmit(handleSubmit)}
            className="flex flex-1 flex-col gap-4 pt-2"
          >
            <div className="grid grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="firstName"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Nome</FormLabel>
                    <FormControl>
                      <Input placeholder="Nome" {...field} />
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
                    <FormLabel>Sobrenome</FormLabel>
                    <FormControl>
                      <Input placeholder="Sobrenome" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="birthDate"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Data de Nasc.</FormLabel>
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
                    <FormLabel>CPF</FormLabel>
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

            <div className="grid grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="rg"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>RG</FormLabel>
                    <FormControl>
                      <Input placeholder="RG" {...field} />
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
                    <FormLabel>Emissor</FormLabel>
                    <FormControl>
                      <Input placeholder="Órgão emissor" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="gender"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Sexo</FormLabel>
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
                    <FormLabel>Estado Civil</FormLabel>
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
            </div>

            <FormField
              control={form.control}
              name="professionalStatus"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Situação</FormLabel>
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

            <div className="grid grid-cols-2 gap-4">
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
                    <FormLabel>Telefone</FormLabel>
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

            <FormField
              control={form.control}
              name="email"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>E-mail</FormLabel>
                  <FormControl>
                    <Input type="email" placeholder="cliente@empresa.com" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="notes"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Notas</FormLabel>
                  <FormControl>
                    <Textarea placeholder="Observações sobre o contato" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <SheetFooter className="mt-auto border-t pt-4">
              <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
                Cancelar
              </Button>
              <Button type="submit" disabled={isLoading}>
                {isLoading ? "Salvando…" : isEdit ? "Salvar alterações" : "Salvar"}
              </Button>
            </SheetFooter>
          </form>
        </Form>
      </SheetContent>
    </Sheet>
  );
}
