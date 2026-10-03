"use client";

import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { ChevronRight } from "lucide-react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { useCreateContact } from "@/features/masterdata/contacts/hooks/useContacts";
import { ROUTES } from "@/lib/constants";

const createContactSchema = z.object({
  firstName: z.string().min(1, "Nome é obrigatório").max(100),
  lastName: z.string().max(100).optional(),
  email: z.string().email("E-mail inválido").max(255).optional().or(z.literal("")),
  phone: z.string().max(20).optional(),
  notes: z.string().max(500).optional(),
});

type FormValues = z.infer<typeof createContactSchema>;

export default function NewContactPage() {
  const router = useRouter();
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;
  const createContact = useCreateContact(companyId);

  const form = useForm<FormValues>({
    resolver: zodResolver(createContactSchema),
    defaultValues: {
      firstName: "",
      lastName: "",
      email: "",
      phone: "",
      notes: "",
    },
  });

  const handleSubmit = (values: FormValues) => {
    createContact.mutate(
      {
        firstName: values.firstName,
        lastName: values.lastName || undefined,
        email: values.email || undefined,
        phone: values.phone || undefined,
        notes: values.notes || undefined,
      },
      {
        onSuccess: () => router.push(ROUTES.CONTACTS),
      },
    );
  };

  return (
    <div className="space-y-6">
      {/* Breadcrumb */}
      <nav className="flex items-center gap-1 text-sm text-muted-foreground">
        <Link href={ROUTES.CONTACTS} className="hover:text-foreground">
          Contatos
        </Link>
        <ChevronRight className="h-4 w-4" />
        <span className="text-foreground">Novo cadastro</span>
      </nav>

      <h1 className="text-2xl font-semibold">Novo cadastro</h1>

      <Form {...form}>
        <form onSubmit={form.handleSubmit(handleSubmit)} className="space-y-8">
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
                    <div className="flex items-center justify-between">
                      <FormLabel>Sobrenome</FormLabel>
                      <span className="text-xs text-muted-foreground">Opcional</span>
                    </div>
                    <FormControl>
                      <Input placeholder="Digite o sobrenome" {...field} />
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
                name="phone"
                render={({ field }) => (
                  <FormItem>
                    <div className="flex items-center justify-between">
                      <FormLabel>Telefone</FormLabel>
                      <span className="text-xs text-muted-foreground">Opcional</span>
                    </div>
                    <FormControl>
                      <Input placeholder="(11) 99999-9999" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="email"
                render={({ field }) => (
                  <FormItem>
                    <div className="flex items-center justify-between">
                      <FormLabel>E-mail</FormLabel>
                      <span className="text-xs text-muted-foreground">Opcional</span>
                    </div>
                    <FormControl>
                      <Input type="email" placeholder="cliente@empresa.com" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>
          </section>

          {/* Observações */}
          <section className="space-y-4 border-t pt-6">
            <h2 className="text-lg font-medium">Observações</h2>
            <FormField
              control={form.control}
              name="notes"
              render={({ field }) => (
                <FormItem>
                  <div className="flex items-center justify-between">
                    <FormLabel>Notas</FormLabel>
                    <span className="text-xs text-muted-foreground">Opcional</span>
                  </div>
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
          <div className="flex justify-end gap-3 border-t pt-6">
            <Button
              type="button"
              variant="outline"
              onClick={() => router.push(ROUTES.CONTACTS)}
            >
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
