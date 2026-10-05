"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";

import { useInviteMember } from "@/features/identity/members/hooks/useMembers";
import { usePermissions } from "@/features/identity/rbac/hooks/useRoles";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { Switch } from "@/components/ui/switch";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { maskPhone } from "@/lib/masks";

const CARGOS = [
  { value: "profissional", label: "Profissional" },
  { value: "recepcionista", label: "Recepcionista" },
  { value: "gerente", label: "Gerente" },
  { value: "auxiliar", label: "Auxiliar" },
  { value: "outro", label: "Outro" },
];

const createUserSchema = z.object({
  fullName: z.string().min(1, "Nome completo é obrigatório"),
  phone: z.string().optional().default(""),
  email: z.string().min(1, "E-mail é obrigatório").email("E-mail inválido"),
  cargo: z.string().min(1, "Selecione um cargo"),
});

type CreateUserFormData = z.infer<typeof createUserSchema>;

const PERMISSION_GROUPS = [
  {
    module: "Agenda",
    permissions: [
      {
        id: "appointment:write",
        label: "Edição de agendamentos",
        description:
          "Permite a criação, alteração e deleção de eventos das agendas de todos os profissionais",
      },
      {
        id: "appointment:read",
        label: "Visualização da agenda",
        description:
          "Permite a visualização das agendas de todos os profissionais",
      },
    ],
  },
  {
    module: "Financeiro",
    permissions: [
      {
        id: "finance:debits",
        label: "Aba Débitos de Contatos",
        description:
          "Permite acesso completo a todos os lançamentos da aba Débitos no perfil de qualquer contato",
      },
      {
        id: "finance:own-only",
        label: "Acesso apenas aos lançamentos do usuário",
        description:
          "Criar, editar e excluir apenas os lançamentos criados pelo próprio usuário",
      },
      {
        id: "finance:full",
        label: "Acesso completo à Gestão Financeira",
        description:
          "Usuário tem acesso completo a todas as funcionalidades do Controle Financeiro, incluindo Relatórios",
      },
    ],
  },
  {
    module: "Contatos",
    permissions: [
      {
        id: "contact:page:view",
        label: "Visualizar contatos",
        description: "Permite visualizar a lista e detalhes dos contatos",
      },
      {
        id: "contact:write",
        label: "Criar e editar contatos",
        description: "Permite criar novos contatos e editar os existentes",
      },
    ],
  },
  {
    module: "Pipeline",
    permissions: [
      {
        id: "pipeline:page:view",
        label: "Visualizar pipeline",
        description: "Permite visualizar o pipeline de vendas",
      },
      {
        id: "pipeline:write",
        label: "Gerenciar oportunidades",
        description: "Permite criar, editar e mover oportunidades no pipeline",
      },
    ],
  },
  {
    module: "Comunicação",
    permissions: [
      {
        id: "omnichannel:page:view",
        label: "Acesso ao Inbox",
        description: "Permite acessar e responder mensagens no inbox",
      },
      {
        id: "campaign:page:view",
        label: "Campanhas",
        description: "Permite criar e gerenciar campanhas de comunicação",
      },
    ],
  },
];

export default function CreateUserPage() {
  const router = useRouter();
  const inviteMember = useInviteMember();
  const [isAdmin, setIsAdmin] = useState(false);
  const [enabledPermissions, setEnabledPermissions] = useState<Set<string>>(
    new Set(),
  );

  const {
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<CreateUserFormData>({
    resolver: zodResolver(createUserSchema),
    defaultValues: {
      fullName: "",
      phone: "",
      email: "",
      cargo: "",
    },
  });

  const togglePermission = (id: string) => {
    setEnabledPermissions((prev) => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  };

  const onSubmit = (data: CreateUserFormData) => {
    const nameParts = data.fullName.trim().split(/\s+/);
    const firstName = nameParts[0];
    const lastName = nameParts.slice(1).join(" ") || firstName;

    inviteMember.mutate(
      {
        firstName,
        lastName,
        email: data.email,
        jobTitle: data.cargo,
      },
      {
        onSuccess: () => {
          router.push("/settings/company/users");
        },
      },
    );
  };

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div>
        <h2 className="text-xl font-semibold">Criar novo usuário</h2>
        <p className="mt-1 text-sm text-muted-foreground">
          Ao preencher os dados abaixo, será criado um novo usuário e um e-mail
          convite será enviado à pessoa cadastrada.
        </p>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        <section className="space-y-4 rounded-lg border bg-card p-5">
          <div className="grid gap-4 sm:grid-cols-1">
            <div className="space-y-1.5">
              <Label>Nome completo *</Label>
              <Controller
                name="fullName"
                control={control}
                render={({ field }) => (
                  <Input placeholder="Maria Conceição de Freitas" {...field} />
                )}
              />
              {errors.fullName && (
                <p className="text-xs text-destructive">
                  {errors.fullName.message}
                </p>
              )}
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-1.5">
              <Label>Celular</Label>
              <Controller
                name="phone"
                control={control}
                render={({ field }) => (
                  <Input
                    inputMode="numeric"
                    placeholder="(11) 99555-5555"
                    value={field.value}
                    onChange={(e) => field.onChange(maskPhone(e.target.value))}
                    ref={field.ref}
                  />
                )}
              />
            </div>
            <div className="space-y-1.5">
              <Label>E-mail *</Label>
              <Controller
                name="email"
                control={control}
                render={({ field }) => (
                  <Input
                    type="email"
                    placeholder="maria.freitas@clinica.com"
                    {...field}
                  />
                )}
              />
              {errors.email && (
                <p className="text-xs text-destructive">
                  {errors.email.message}
                </p>
              )}
            </div>
          </div>

          <div className="space-y-1.5 sm:w-1/2">
            <Label>Cargo *</Label>
            <Controller
              name="cargo"
              control={control}
              render={({ field }) => (
                <Select value={field.value} onValueChange={field.onChange}>
                  <SelectTrigger>
                    <SelectValue placeholder="Selecione" />
                  </SelectTrigger>
                  <SelectContent>
                    {CARGOS.map((c) => (
                      <SelectItem key={c.value} value={c.value}>
                        {c.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            />
            {errors.cargo && (
              <p className="text-xs text-destructive">
                {errors.cargo.message}
              </p>
            )}
          </div>
        </section>

        <section className="space-y-4 rounded-lg border bg-card p-5">
          <h3 className="text-sm font-semibold">Permissões</h3>

          <div className="flex items-center gap-2">
            <Checkbox
              id="admin"
              checked={isAdmin}
              onCheckedChange={(checked) => setIsAdmin(checked === true)}
            />
            <Label htmlFor="admin" className="font-medium">
              Administrador(a)
            </Label>
          </div>
          <p className="text-xs text-muted-foreground">
            Perfis administradores possuem acesso completo a todos os módulos do
            sistema, inclusive a gestão de usuários.
          </p>

          {!isAdmin &&
            PERMISSION_GROUPS.map((group) => (
              <div key={group.module} className="space-y-2">
                <h4 className="text-sm font-semibold text-primary">
                  {group.module}
                </h4>
                {group.permissions.map((perm) => (
                  <div
                    key={perm.id}
                    className="flex items-start gap-3 rounded-md p-2 transition-colors hover:bg-muted/50"
                  >
                    <Switch
                      checked={enabledPermissions.has(perm.id)}
                      onCheckedChange={() => togglePermission(perm.id)}
                      className="mt-0.5"
                    />
                    <div className="space-y-0.5">
                      <p className="text-sm font-medium leading-tight">
                        {perm.label}:
                      </p>
                      <p className="text-xs text-muted-foreground">
                        {perm.description}
                      </p>
                    </div>
                  </div>
                ))}
              </div>
            ))}
        </section>

        <div className="flex justify-end gap-2">
          <Button
            type="button"
            variant="outline"
            onClick={() => router.push("/settings/company/users")}
          >
            Cancelar
          </Button>
          <Button type="submit" disabled={inviteMember.isPending}>
            {inviteMember.isPending ? "Criando..." : "Criar usuário"}
          </Button>
        </div>
      </form>
    </div>
  );
}
