"use client";

import Link from "next/link";
import {
  Building2,
  Mail,
  Phone,
  Globe,
  MapPin,
  Users,
  HardDrive,
  Contact,
  Calendar,
  Pencil,
} from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import { ROUTES } from "@/lib/constants";
import type { Tenant } from "../types/tenant.types";
import { TenantStatusBadge } from "./TenantStatusBadge";
import { TenantPlanBadge } from "./TenantPlanBadge";

type TenantDetailsProps = {
  tenant: Tenant;
};

export function TenantDetails({ tenant }: TenantDetailsProps) {
  const formatDate = (dateStr: string) =>
    new Date(dateStr).toLocaleDateString("pt-BR", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
    });

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-4">
          <div className="flex h-16 w-16 items-center justify-center rounded-xl bg-muted">
            {tenant.logoUrl ? (
              <img
                src={tenant.logoUrl}
                alt={tenant.tradingName}
                className="h-16 w-16 rounded-xl object-cover"
              />
            ) : (
              <Building2 className="h-8 w-8 text-muted-foreground" />
            )}
          </div>
          <div>
            <h2 className="text-2xl font-bold">{tenant.tradingName}</h2>
            <p className="text-sm text-muted-foreground">{tenant.legalName}</p>
            <div className="mt-1 flex items-center gap-2">
              <TenantStatusBadge status={tenant.status} />
              <TenantPlanBadge plan={tenant.plan} />
            </div>
          </div>
        </div>
        <Button asChild>
          <Link href={`${ROUTES.TENANTS}/${tenant.id}/edit`}>
            <Pencil className="mr-2 h-4 w-4" />
            Editar
          </Link>
        </Button>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        {/* Dados da Empresa */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Dados da Empresa</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <InfoRow label="CNPJ" value={tenant.cnpj} />
            {tenant.stateRegistration && (
              <InfoRow label="Inscrição Estadual" value={tenant.stateRegistration} />
            )}
            {tenant.municipalRegistration && (
              <InfoRow label="Inscrição Municipal" value={tenant.municipalRegistration} />
            )}
            <Separator />
            <InfoRow icon={<Mail className="h-4 w-4" />} label="E-mail" value={tenant.email} />
            <InfoRow icon={<Phone className="h-4 w-4" />} label="Telefone" value={tenant.phone} />
            {tenant.website && (
              <InfoRow icon={<Globe className="h-4 w-4" />} label="Website" value={tenant.website} />
            )}
          </CardContent>
        </Card>

        {/* Endereço */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Endereço</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <InfoRow
              icon={<MapPin className="h-4 w-4" />}
              label="Endereço"
              value={`${tenant.address.street}, ${tenant.address.number}${
                tenant.address.complement ? ` - ${tenant.address.complement}` : ""
              }`}
            />
            <InfoRow label="Bairro" value={tenant.address.neighborhood} />
            <InfoRow
              label="Cidade/Estado"
              value={`${tenant.address.city} - ${tenant.address.state}`}
            />
            <InfoRow label="CEP" value={tenant.address.zipCode} />
            <InfoRow label="País" value={tenant.address.country} />
          </CardContent>
        </Card>

        {/* Configurações */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Configurações</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <InfoRow
              icon={<Users className="h-4 w-4" />}
              label="Limite de Usuários"
              value={`${tenant.maxUsers}`}
            />
            <InfoRow
              icon={<HardDrive className="h-4 w-4" />}
              label="Armazenamento"
              value={`${tenant.maxStorageMb >= 1024 ? `${(tenant.maxStorageMb / 1024).toFixed(0)} GB` : `${tenant.maxStorageMb} MB`}`}
            />
            <InfoRow
              icon={<Contact className="h-4 w-4" />}
              label="Limite de Contatos"
              value={`${tenant.maxContacts}`}
            />
            {tenant.notes && (
              <>
                <Separator />
                <div>
                  <p className="text-xs font-medium text-muted-foreground">Observações</p>
                  <p className="mt-1 text-sm">{tenant.notes}</p>
                </div>
              </>
            )}
          </CardContent>
        </Card>

        {/* Metadados */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Metadados</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <InfoRow
              icon={<Calendar className="h-4 w-4" />}
              label="Criado em"
              value={formatDate(tenant.createdAt)}
            />
            <InfoRow label="Atualizado em" value={formatDate(tenant.updatedAt)} />
            <InfoRow label="ID" value={tenant.id} className="font-mono text-xs" />
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

function InfoRow({
  icon,
  label,
  value,
  className,
}: {
  icon?: React.ReactNode;
  label: string;
  value: string;
  className?: string;
}) {
  return (
    <div className="flex items-start gap-2">
      {icon && <span className="mt-0.5 text-muted-foreground">{icon}</span>}
      <div className="min-w-0 flex-1">
        <p className="text-xs font-medium text-muted-foreground">{label}</p>
        <p className={`text-sm ${className || ""}`}>{value}</p>
      </div>
    </div>
  );
}
