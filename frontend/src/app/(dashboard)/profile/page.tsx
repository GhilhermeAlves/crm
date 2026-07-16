"use client";

import { useRouter } from "next/navigation";
import { useProfile, useUpdateProfile } from "@/features/users/hooks/useUsers";
import { UserAvatar } from "@/features/users/components/UserAvatar";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { PageTitle } from "@/components/common/PageTitle";
import { SkeletonCard } from "@/components/feedback/SkeletonCard";
import { useForm } from "react-hook-form";
import { useEffect } from "react";

const timezones = [
  { value: "America/Sao_Paulo", label: "Horário de Brasília (GMT-3)" },
  { value: "America/Manaus", label: "Horário de Manaus (GMT-4)" },
  { value: "America/Rio_Branco", label: "Horário do Acre (GMT-5)" },
];

const languages = [
  { value: "pt-BR", label: "Português (Brasil)" },
  { value: "en-US", label: "English (US)" },
  { value: "es", label: "Español" },
];

export default function ProfilePage() {
  const router = useRouter();
  const { data: user, isLoading } = useProfile();
  const updateProfile = useUpdateProfile();

  const { register, handleSubmit, setValue, watch, reset } = useForm({
    defaultValues: {
      firstName: "",
      lastName: "",
      phone: "",
      department: "",
      jobTitle: "",
      language: "pt-BR",
      timezone: "America/Sao_Paulo",
      notes: "",
    },
  });

  useEffect(() => {
    if (user) {
      reset({
        firstName: user.firstName || "",
        lastName: user.lastName || "",
        phone: user.phone || "",
        department: user.department || "",
        jobTitle: user.jobTitle || "",
        language: user.language || "pt-BR",
        timezone: user.timezone || "America/Sao_Paulo",
        notes: user.notes || "",
      });
    }
  }, [user, reset]);

  const watchedLanguage = watch("language");
  const watchedTimezone = watch("timezone");

  const onSubmit = (data: any) => {
    updateProfile.mutate(data);
  };

  if (isLoading) {
    return (
      <div className="space-y-6">
        <SkeletonCard />
      </div>
    );
  }

  if (!user) {
    return (
      <div className="text-center py-12">
        <p className="text-muted-foreground">Perfil não encontrado.</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <PageTitle>Meu Perfil</PageTitle>

      <Card>
        <CardContent className="pt-6">
          <div className="flex items-center gap-4">
            <UserAvatar
              firstName={user.firstName}
              lastName={user.lastName}
              avatarUrl={user.avatarUrl}
              size="lg"
            />
            <div>
              <p className="text-lg font-bold">{user.name}</p>
              <p className="text-muted-foreground">{user.email}</p>
            </div>
          </div>
        </CardContent>
      </Card>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        <Card>
          <CardHeader>
            <CardTitle>Dados Pessoais</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="firstName">Nome</Label>
                <Input id="firstName" {...register("firstName")} />
              </div>
              <div className="space-y-2">
                <Label htmlFor="lastName">Sobrenome</Label>
                <Input id="lastName" {...register("lastName")} />
              </div>
            </div>
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="phone">Telefone</Label>
                <Input id="phone" placeholder="(XX) XXXXX-XXXX" {...register("phone")} />
              </div>
              <div className="space-y-2">
                <Label htmlFor="jobTitle">Cargo</Label>
                <Input id="jobTitle" {...register("jobTitle")} />
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="department">Departamento</Label>
              <Input id="department" {...register("department")} />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Preferências</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label>Idioma</Label>
                <Select
                  value={watchedLanguage}
                  onValueChange={(val) => setValue("language", val)}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {languages.map((lang) => (
                      <SelectItem key={lang.value} value={lang.value}>
                        {lang.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label>Fuso Horário</Label>
                <Select
                  value={watchedTimezone}
                  onValueChange={(val) => setValue("timezone", val)}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {timezones.map((tz) => (
                      <SelectItem key={tz.value} value={tz.value}>
                        {tz.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="notes">Observações</Label>
              <Textarea id="notes" rows={3} {...register("notes")} />
            </div>
          </CardContent>
        </Card>

        <div className="flex justify-end">
          <Button type="submit" disabled={updateProfile.isPending}>
            {updateProfile.isPending ? "Salvando..." : "Salvar Alterações"}
          </Button>
        </div>
      </form>
    </div>
  );
}
