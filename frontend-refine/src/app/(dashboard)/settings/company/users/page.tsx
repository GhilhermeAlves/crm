"use client";

import { Suspense, useMemo, useState } from "react";
import { useSearchParams } from "next/navigation";
import { format, parseISO } from "date-fns";
import { ptBR } from "date-fns/locale";
import { UserRoundPlus, UsersRound } from "lucide-react";

import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { usePermission } from "@/features/identity/auth/hooks/useAuthorization";
import { useMembers } from "@/features/identity/members/hooks/useMembers";
import { useInvitations } from "@/features/identity/invitations/hooks/useInvitations";
import { CreateInvitationDialog } from "@/features/identity/invitations/components/CreateInvitationDialog";
import { InvitationRowActions } from "@/features/identity/invitations/components/InvitationRowActions";
import { MemberRowActions } from "@/features/identity/members/components/MemberRowActions";
import { invitationStatusMeta } from "@/features/identity/invitations/lib/invitation-status";
import { memberStatusLabel, roleLabel } from "@/features/identity/members/lib/labels";
import type { Member } from "@/features/identity/members/types/member.types";
import type { Invitation } from "@/features/identity/invitations/types/invitation.types";
import { PageTitle } from "@/components/common/PageTitle";
import { EmptyState } from "@/components/common/EmptyState";
import { ErrorCard } from "@/components/common/ErrorCard";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";

type MemberRow = { kind: "member"; id: string; member: Member };
type InvitationRow = { kind: "invitation"; id: string; invitation: Invitation };
type Entry = MemberRow | InvitationRow;

const VALID_TABS = new Set(["todos", "ativos", "convites", "inativos"]);

export default function UsersPage() {
  return (
    <Suspense
      fallback={
        <div className="space-y-6">
          <Skeleton className="h-8 w-64" />
          <Skeleton className="h-96 w-full" />
        </div>
      }
    >
      <UsersManagement />
    </Suspense>
  );
}

/*
 * A aba inicial vem de ?tab=todos|ativos|convites|inativos.
 * `/members` e `/invitations` (agora redirects) apontam para cá.
 */
function UsersManagement() {
  const searchParams = useSearchParams();
  const requestedTab = searchParams.get("tab");

  const { user } = useAuth();
  const companyId = user?.companyId ?? null;

  const can = usePermission();
  const canView = can("user:read");
  const canInvite = can("user:invite");

  const membersQuery = useMembers(companyId, "ACTIVE");
  const removedQuery = useMembers(companyId, "REMOVED");
  const invitationsQuery = useInvitations(companyId);

  const [tab, setTab] = useState(() =>
    requestedTab && VALID_TABS.has(requestedTab) ? requestedTab : "todos",
  );

  const members = membersQuery.data ?? [];
  const removedMembers = removedQuery.data ?? [];
  const invitations = invitationsQuery.data ?? [];

  const pendingInvitations = invitations.filter((i) => i.status === "PENDING");
  const inactiveInvitations = invitations.filter(
    (i) => i.status === "REVOKED" || i.status === "EXPIRED",
  );

  const allEntries = useMemo(
    () =>
      sortByDateDesc([
        ...members.map((m): MemberRow => ({ kind: "member", id: `m-${m.userId}`, member: m })),
        ...pendingInvitations.map((i): InvitationRow => ({
          kind: "invitation",
          id: `i-${i.id}`,
          invitation: i,
        })),
      ]),
    [members, pendingInvitations],
  );

  const inactiveEntries = useMemo(
    () =>
      sortByDateDesc([
        ...removedMembers.map((m): MemberRow => ({
          kind: "member",
          id: `m-${m.userId}`,
          member: m,
        })),
        ...inactiveInvitations.map((i): InvitationRow => ({
          kind: "invitation",
          id: `i-${i.id}`,
          invitation: i,
        })),
      ]),
    [removedMembers, inactiveInvitations],
  );

  if (!companyId) {
    return (
      <EmptyState
        icon={<UsersRound />}
        title="Empresa não identificada"
        description="Selecione uma empresa para gerenciar os usuários."
      />
    );
  }

  if (!canView) {
    return (
      <EmptyState
        icon={<UsersRound />}
        title="Sem permissão"
        description="Você não tem permissão para gerenciar usuários desta empresa."
      />
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between gap-4">
        <div>
          <PageTitle>Usuários</PageTitle>
          <p className="text-sm text-muted-foreground">
            Gerencie quem tem acesso à empresa: papéis, convites e acesso.
          </p>
        </div>
        {canInvite && (
          <CreateInvitationDialog
            companyId={companyId}
            title="Novo usuário"
            description="Envie um convite por e-mail. O novo usuário define a senha ao aceitar o convite."
            submitLabel="Enviar convite"
            trigger={
              <Button size="sm">
                <UserRoundPlus className="mr-1.5 h-4 w-4" />
                Novo usuário
              </Button>
            }
          />
        )}
      </div>

      <Tabs value={tab} onValueChange={setTab}>
        <TabsList>
          <TabsTrigger value="todos">Todos ({allEntries.length})</TabsTrigger>
          <TabsTrigger value="ativos">Ativos ({members.length})</TabsTrigger>
          <TabsTrigger value="convites">
            Convites pendentes ({pendingInvitations.length})
          </TabsTrigger>
          <TabsTrigger value="inativos">Inativos ({inactiveEntries.length})</TabsTrigger>
        </TabsList>

        <TabsContent value="todos">
          <CombinedTable
            entries={allEntries}
            activeMembers={members}
            companyId={companyId}
            loading={membersQuery.isLoading || invitationsQuery.isLoading}
            error={membersQuery.error ?? invitationsQuery.error}
            onRetryAll={() => {
              void membersQuery.refetch();
              void invitationsQuery.refetch();
            }}
          />
        </TabsContent>

        <TabsContent value="ativos">
          <ActiveMembersTable
            members={members}
            companyId={companyId}
            loading={membersQuery.isLoading}
            error={membersQuery.error}
            onRetry={() => void membersQuery.refetch()}
          />
        </TabsContent>

        <TabsContent value="convites">
          <PendingInvitationsTable
            invitations={pendingInvitations}
            companyId={companyId}
            loading={invitationsQuery.isLoading}
            error={invitationsQuery.error}
            onRetry={() => void invitationsQuery.refetch()}
          />
        </TabsContent>

        <TabsContent value="inativos">
          <CombinedTable
            entries={inactiveEntries}
            activeMembers={members}
            companyId={companyId}
            loading={removedQuery.isLoading || invitationsQuery.isLoading}
            error={removedQuery.error ?? invitationsQuery.error}
            onRetryAll={() => {
              void removedQuery.refetch();
              void invitationsQuery.refetch();
            }}
          />
        </TabsContent>
      </Tabs>
    </div>
  );
}

function formatDate(value: string | null): string {
  if (!value) return "—";
  return format(parseISO(value), "dd/MM/yyyy", { locale: ptBR });
}

function entryDate(entry: Entry): string {
  return entry.kind === "member" ? (entry.member.joinedAt ?? "") : entry.invitation.createdAt;
}

function sortByDateDesc(entries: Entry[]): Entry[] {
  return [...entries].sort((a, b) => entryDate(b).localeCompare(entryDate(a)));
}

function memberStatusMeta(status: Member["status"]): {
  label: string;
  variant: "default" | "secondary" | "outline";
} {
  if (status === "ACTIVE") return { label: "Ativo", variant: "default" };
  return { label: memberStatusLabel(status), variant: "secondary" };
}

type CombinedTableProps = {
  entries: Entry[];
  activeMembers: Member[];
  companyId: string;
  loading: boolean;
  error: unknown;
  onRetryAll: () => void;
};

function CombinedTable({
  entries,
  activeMembers,
  companyId,
  loading,
  error,
  onRetryAll,
}: CombinedTableProps) {
  if (loading) return <TableSkeleton rows={5} />;
  if (error) {
    return <ErrorCard message="Não foi possível carregar os usuários." onRetry={onRetryAll} />;
  }
  if (entries.length === 0) {
    return (
      <EmptyState
        icon={<UsersRound />}
        title="Nada por aqui"
        description="Esta lista está vazia."
      />
    );
  }
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Usuário</TableHead>
          <TableHead>Papel</TableHead>
          <TableHead>Status</TableHead>
          <TableHead>Entrada</TableHead>
          <TableHead className="w-12" />
        </TableRow>
      </TableHeader>
      <TableBody>
        {entries.map((entry) =>
          entry.kind === "member" ? (
            <MemberTableRow
              key={entry.id}
              member={entry.member}
              activeMembers={activeMembers}
              companyId={companyId}
            />
          ) : (
            <InvitationTableRow
              key={entry.id}
              invitation={entry.invitation}
              companyId={companyId}
            />
          ),
        )}
      </TableBody>
    </Table>
  );
}

function MemberTableRow({
  member,
  activeMembers,
  companyId,
}: {
  member: Member;
  activeMembers: Member[];
  companyId: string;
}) {
  const status = memberStatusMeta(member.status);
  return (
    <TableRow>
      <TableCell>
        <div className="flex flex-col">
          <span className="text-sm font-medium">{member.name}</span>
          <span className="text-xs text-muted-foreground">{member.email}</span>
        </div>
      </TableCell>
      <TableCell>
        <Badge variant="outline">{roleLabel(member.role)}</Badge>
      </TableCell>
      <TableCell>
        <Badge variant={status.variant}>{status.label}</Badge>
      </TableCell>
      <TableCell className="text-sm text-muted-foreground">{formatDate(member.joinedAt)}</TableCell>
      <TableCell>
        <MemberRowActions companyId={companyId} member={member} activeMembers={activeMembers} />
      </TableCell>
    </TableRow>
  );
}

function InvitationTableRow({
  invitation,
  companyId,
}: {
  invitation: Invitation;
  companyId: string;
}) {
  const status = invitationStatusMeta(invitation.status);
  return (
    <TableRow>
      <TableCell>
        <div className="flex flex-col">
          <span className="text-sm font-medium">{invitation.inviteeName || invitation.email}</span>
          <span className="text-xs text-muted-foreground">{invitation.email}</span>
        </div>
      </TableCell>
      <TableCell>
        <Badge variant="outline">{roleLabel(invitation.role)}</Badge>
      </TableCell>
      <TableCell>
        <Badge variant={status.variant}>{status.label}</Badge>
      </TableCell>
      <TableCell className="text-sm text-muted-foreground">
        {formatDate(invitation.createdAt)}
      </TableCell>
      <TableCell>
        <InvitationRowActions companyId={companyId} invitation={invitation} />
      </TableCell>
    </TableRow>
  );
}

function ActiveMembersTable({
  members,
  companyId,
  loading,
  error,
  onRetry,
}: {
  members: Member[];
  companyId: string;
  loading: boolean;
  error: unknown;
  onRetry: () => void;
}) {
  if (loading) return <TableSkeleton rows={5} />;
  if (error) {
    return <ErrorCard message="Não foi possível carregar os membros ativos." onRetry={onRetry} />;
  }
  if (members.length === 0) {
    return (
      <EmptyState
        icon={<UsersRound />}
        title="Nenhum membro ativo"
        description="Convide o primeiro membro para a empresa."
        action={
          <CreateInvitationDialog
            companyId={companyId}
            submitLabel="Enviar convite"
            trigger={<Button size="sm">Convidar membro</Button>}
          />
        }
      />
    );
  }
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Usuário</TableHead>
          <TableHead>Papel</TableHead>
          <TableHead>Entrada</TableHead>
          <TableHead className="w-12" />
        </TableRow>
      </TableHeader>
      <TableBody>
        {members.map((member) => (
          <TableRow key={member.userId}>
            <TableCell>
              <div className="flex flex-col">
                <span className="text-sm font-medium">{member.name}</span>
                <span className="text-xs text-muted-foreground">{member.email}</span>
              </div>
            </TableCell>
            <TableCell>
              <Badge variant="outline">{roleLabel(member.role)}</Badge>
            </TableCell>
            <TableCell className="text-sm text-muted-foreground">
              {formatDate(member.joinedAt)}
            </TableCell>
            <TableCell>
              <MemberRowActions companyId={companyId} member={member} activeMembers={members} />
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}

function PendingInvitationsTable({
  invitations,
  companyId,
  loading,
  error,
  onRetry,
}: {
  invitations: Invitation[];
  companyId: string;
  loading: boolean;
  error: unknown;
  onRetry: () => void;
}) {
  if (loading) return <TableSkeleton rows={5} />;
  if (error) {
    return <ErrorCard message="Não foi possível carregar os convites." onRetry={onRetry} />;
  }
  if (invitations.length === 0) {
    return (
      <EmptyState
        icon={<UsersRound />}
        title="Nenhum convite pendente"
        description="Os convites enviados aguardando aceite aparecem aqui."
      />
    );
  }
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>E-mail</TableHead>
          <TableHead>Papel</TableHead>
          <TableHead>Expira em</TableHead>
          <TableHead className="w-12" />
        </TableRow>
      </TableHeader>
      <TableBody>
        {invitations.map((invitation) => (
          <TableRow key={invitation.id}>
            <TableCell>
              <div className="flex flex-col">
                <span className="text-sm font-medium">
                  {invitation.inviteeName || invitation.email}
                </span>
                <span className="text-xs text-muted-foreground">{invitation.email}</span>
              </div>
            </TableCell>
            <TableCell>
              <Badge variant="outline">{roleLabel(invitation.role)}</Badge>
            </TableCell>
            <TableCell className="text-sm text-muted-foreground">
              {format(parseISO(invitation.expiresAt), "dd/MM/yyyy HH:mm", { locale: ptBR })}
            </TableCell>
            <TableCell>
              <InvitationRowActions companyId={companyId} invitation={invitation} />
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}

function TableSkeleton({ rows }: { rows: number }) {
  return (
    <div className="space-y-3 p-4">
      {Array.from({ length: rows }).map((_, index) => (
        <Skeleton key={index} className="h-10 w-full" />
      ))}
    </div>
  );
}
