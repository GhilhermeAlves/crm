"use client";

import { useState, useCallback, useRef, useEffect } from "react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { ChevronRight, Camera, Loader2, Search } from "lucide-react";
import Link from "next/link";
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
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { DateTimePicker } from "@/components/ui/date-time-picker";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import {
  useCreateContact,
  useSearchContacts,
} from "@/features/masterdata/contacts/hooks/useContacts";
import type { Contact } from "@/features/masterdata/contacts/types/contact.types";
import { ROUTES } from "@/lib/constants";
import { useContactRules } from "@/features/identity/tenants/hooks/useTenants";
import { formatCpf, validateCpf, formatPhone, formatCep, fetchCep } from "@/lib/format";

const MAX_PHOTO_SIZE = 4 * 1024 * 1024;

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

const UF_OPTIONS = [
  "AC",
  "AL",
  "AP",
  "AM",
  "BA",
  "CE",
  "DF",
  "ES",
  "GO",
  "MA",
  "MT",
  "MS",
  "MG",
  "PA",
  "PB",
  "PR",
  "PE",
  "PI",
  "RJ",
  "RN",
  "RS",
  "RO",
  "RR",
  "SC",
  "SP",
  "SE",
  "TO",
] as const;

const createContactSchema = (requireCpf: boolean) =>
  z
    .object({
      firstName: z.string().min(1, "Nome é obrigatório").max(100),
      lastName: z.string().max(100).optional(),
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
      mobile: z.string().min(1, "Celular é obrigatório").max(20),
      phone: z.string().max(20).optional(),
      email: z.string().min(1, "E-mail é obrigatório").max(255).email("E-mail inválido"),
      cep: z.string().min(1, "CEP é obrigatório").max(10),
      street: z.string().min(1, "Logradouro é obrigatório").max(255),
      addressNumber: z.string().min(1, "Número é obrigatório").max(20),
      complement: z.string().max(100).optional(),
      neighborhood: z.string().min(1, "Bairro é obrigatório").max(100),
      city: z.string().min(1, "Cidade é obrigatória").max(100),
      state: z.string().min(1, "Estado é obrigatório").max(2),
      hasGuardian: z.boolean().optional(),
      guardianName: z.string().max(200).optional(),
      guardianCpf: z.string().optional(),
      guardianPhone: z.string().max(20).optional(),
      notes: z.string().max(500).optional(),
    })
    .superRefine((data, ctx) => {
      // Preferência da empresa (Minha Empresa → Preferências)
      if (requireCpf && (!data.cpf || data.cpf.replace(/\D/g, "").length === 0)) {
        ctx.addIssue({ code: z.ZodIssueCode.custom, message: "CPF é obrigatório", path: ["cpf"] });
      }
      if (data.hasGuardian) {
        if (!data.guardianName || data.guardianName.trim().length === 0) {
          ctx.addIssue({
            code: z.ZodIssueCode.custom,
            message: "Nome do responsável é obrigatório",
            path: ["guardianName"],
          });
        }
        if (!data.guardianPhone || data.guardianPhone.replace(/\D/g, "").length < 10) {
          ctx.addIssue({
            code: z.ZodIssueCode.custom,
            message: "Telefone do responsável é obrigatório",
            path: ["guardianPhone"],
          });
        }
      }
    });

type FormValues = z.infer<ReturnType<typeof createContactSchema>>;

function OptionalLabel({ label }: { label: string }) {
  return (
    <div className="flex items-center justify-between">
      <FormLabel>{label}</FormLabel>
      <span className="text-xs text-muted-foreground">Opcional</span>
    </div>
  );
}

function ContactSearchDialog({
  open,
  onOpenChange,
  companyId,
  onSelect,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  companyId: string | null;
  onSelect: (contact: Contact) => void;
}) {
  const [query, setQuery] = useState("");
  const { data: results, isLoading } = useSearchContacts(companyId, query);

  useEffect(() => {
    if (!open) setQuery("");
  }, [open]);

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle>Buscar contato existente</DialogTitle>
        </DialogHeader>
        <div className="relative">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            placeholder="Digite nome ou CPF..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            className="pl-9"
            autoFocus
          />
        </div>
        <div className="max-h-60 overflow-y-auto">
          {isLoading && (
            <div className="flex items-center justify-center py-4">
              <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
            </div>
          )}
          {results && results.length === 0 && query.length >= 2 && (
            <p className="py-4 text-center text-sm text-muted-foreground">
              Nenhum contato encontrado
            </p>
          )}
          {results?.map((c) => (
            <button
              key={c.id}
              type="button"
              className="flex w-full items-center gap-3 rounded-md px-3 py-2 text-left text-sm hover:bg-accent"
              onClick={() => {
                onSelect(c);
                onOpenChange(false);
              }}
            >
              <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary/10 text-xs font-medium text-primary">
                {(c.firstName?.[0] ?? "").toUpperCase()}
              </div>
              <div className="min-w-0 flex-1">
                <p className="truncate font-medium">
                  {c.firstName} {c.lastName}
                </p>
                <p className="truncate text-xs text-muted-foreground">
                  {c.cpf ? formatCpf(c.cpf) : ""}
                  {c.email ? ` · ${c.email}` : ""}
                </p>
              </div>
            </button>
          ))}
        </div>
      </DialogContent>
    </Dialog>
  );
}

export default function NewContactPage() {
  const router = useRouter();
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;
  const createContact = useCreateContact(companyId);
  const { data: contactRules } = useContactRules(companyId);
  const requireCpf = contactRules?.requireContactCpf ?? false;
  const [avatarPreview, setAvatarPreview] = useState<string | null>(null);
  const [photoError, setPhotoError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [cepLoading, setCepLoading] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);

  const form = useForm<FormValues>({
    resolver: zodResolver(createContactSchema(requireCpf)),
    defaultValues: {
      firstName: "",
      lastName: "",
      birthDate: "",
      cpf: "",
      rg: "",
      rgIssuer: "",
      gender: "",
      maritalStatus: "",
      professionalStatus: "",
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
    },
  });

  const hasGuardian = form.watch("hasGuardian");

  const fillFromContact = (c: Contact) => {
    form.setValue("firstName", c.firstName ?? "");
    form.setValue("lastName", c.lastName ?? "");
    form.setValue("email", c.email ?? "");
    form.setValue("phone", c.phone ? formatPhone(c.phone) : "");
    form.setValue("mobile", c.mobile ? formatPhone(c.mobile) : "");
    form.setValue("cpf", c.cpf ? formatCpf(c.cpf) : "");
    form.setValue("rg", c.rg ?? "");
    form.setValue("rgIssuer", c.rgIssuer ?? "");
    form.setValue("gender", c.gender ?? "");
    form.setValue("maritalStatus", c.maritalStatus ?? "");
    form.setValue("birthDate", c.birthDate ?? "");
    form.setValue("notes", c.notes ?? "");
  };

  const handleAvatarChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (file.size > MAX_PHOTO_SIZE) {
      setPhotoError("A foto deve ter no máximo 4MB");
      return;
    }
    setPhotoError(null);
    const reader = new FileReader();
    reader.onload = () => setAvatarPreview(reader.result as string);
    reader.readAsDataURL(file);
  };

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

  const handleSubmit = (values: FormValues) => {
    createContact.mutate(
      {
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
      },
      {
        onSuccess: () => router.push(ROUTES.CONTACTS),
      },
    );
  };

  return (
    <div className="space-y-6">
      <nav className="flex items-center gap-1 text-sm text-muted-foreground">
        <Link href={ROUTES.CONTACTS} className="hover:text-foreground">
          Contatos
        </Link>
        <ChevronRight className="h-4 w-4" />
        <span className="text-foreground">Novo cadastro</span>
      </nav>

      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold tracking-tight">Novo Contato</h1>
        <Button
          type="button"
          variant="outline"
          size="sm"
          className="gap-1.5"
          onClick={() => setSearchOpen(true)}
        >
          <Search className="h-4 w-4" />
          Buscar existente
        </Button>
      </div>

      <ContactSearchDialog
        open={searchOpen}
        onOpenChange={setSearchOpen}
        companyId={companyId}
        onSelect={fillFromContact}
      />

      <Form {...form}>
        <form onSubmit={form.handleSubmit(handleSubmit)} className="space-y-8">
          {/* Foto */}
          <section className="flex items-center gap-6">
            <button
              type="button"
              onClick={() => fileInputRef.current?.click()}
              className="relative flex h-24 w-24 shrink-0 items-center justify-center overflow-hidden rounded-full border-2 border-dashed border-muted-foreground/30 bg-muted/50 transition-colors hover:border-primary/50"
            >
              {avatarPreview ? (
                <img
                  src={avatarPreview}
                  alt="Foto do contato"
                  className="h-full w-full object-cover"
                />
              ) : (
                <Camera className="h-8 w-8 text-muted-foreground/50" />
              )}
            </button>
            <input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              className="hidden"
              onChange={handleAvatarChange}
            />
            <div className="text-sm text-muted-foreground">
              <p className="font-medium text-foreground">Foto do contato</p>
              <p>Clique para adicionar uma foto (máx. 4MB)</p>
              {photoError && <p className="text-sm text-destructive">{photoError}</p>}
            </div>
          </section>

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
                    <FormLabel>CEP</FormLabel>
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
            <Button type="button" variant="outline" onClick={() => router.push(ROUTES.CONTACTS)}>
              Cancelar
            </Button>
            <Button type="submit" disabled={createContact.isPending}>
              {createContact.isPending ? "Salvando…" : "Salvar"}
            </Button>
          </div>
        </form>
      </Form>
    </div>
  );
}
