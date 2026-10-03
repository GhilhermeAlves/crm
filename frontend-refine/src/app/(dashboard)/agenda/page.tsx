"use client";

import { useState, useMemo, useCallback } from "react";
import {
  format,
  startOfWeek,
  endOfWeek,
  startOfDay,
  endOfDay,
  addWeeks,
  subWeeks,
  addDays,
  subDays,
} from "date-fns";
import { ptBR } from "date-fns/locale";
import { ChevronLeft, ChevronRight, Plus, Settings } from "lucide-react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { useMembers } from "@/features/identity/members/hooks/useMembers";
import { useContacts } from "@/features/masterdata/contacts/hooks/useContacts";
import { PageTitle } from "@/components/common/PageTitle";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from "@/components/ui/tooltip";
import { WeeklyCalendar } from "@/features/sales/scheduling/components/WeeklyCalendar";
import { CreateAppointmentDialog } from "@/features/sales/scheduling/components/CreateAppointmentDialog";
import { CreateBlockDialog } from "@/features/sales/scheduling/components/CreateBlockDialog";
import { AppointmentDetailPanel } from "@/features/sales/scheduling/components/AppointmentDetailPanel";
import { MiniCalendar } from "@/features/sales/scheduling/components/MiniCalendar";
import { MemberFilter, getMemberColor } from "@/features/sales/scheduling/components/MemberFilter";
import {
  useAppointments,
  useAppointmentTypes,
  useBlocks,
  useCreateAppointment,
  useCreateBlock,
  useChangeAppointmentStatus,
  useDeleteAppointment,
} from "@/features/sales/scheduling/hooks/useScheduling";
import { useSchedulingPermissions } from "@/features/sales/scheduling/schemas/scheduling.schema";
import type {
  Appointment,
  AppointmentStatus,
} from "@/features/sales/scheduling/types/scheduling.types";
import type { CreateAppointmentFormValues } from "@/features/sales/scheduling/schemas/scheduling.schema";

type CalendarView = "day" | "week";

export default function AgendaPage() {
  const { user } = useAuth();
  const router = useRouter();
  const companyId = user?.companyId ?? null;
  const perms = useSchedulingPermissions();

  const [currentDate, setCurrentDate] = useState(new Date());
  const [miniMonth, setMiniMonth] = useState(new Date());
  const [view, setView] = useState<CalendarView>("week");
  const [createOpen, setCreateOpen] = useState(false);
  const [blockOpen, setBlockOpen] = useState(false);
  const [selectedAppointment, setSelectedAppointment] = useState<Appointment | null>(null);
  const [defaultSlot, setDefaultSlot] = useState<{ start: string; end: string } | null>(null);
  const [selectedMemberIds, setSelectedMemberIds] = useState<Set<string> | null>(null);

  const range = useMemo(() => {
    if (view === "day") {
      return {
        from: startOfDay(currentDate).toISOString(),
        to: endOfDay(currentDate).toISOString(),
      };
    }
    const weekStart = startOfWeek(currentDate, { weekStartsOn: 1 });
    return {
      from: startOfDay(weekStart).toISOString(),
      to: endOfDay(endOfWeek(currentDate, { weekStartsOn: 1 })).toISOString(),
    };
  }, [currentDate, view]);

  const { data: appointments = [] } = useAppointments(companyId, range.from, range.to);
  const { data: blocks = [] } = useBlocks(companyId, range.from, range.to);
  const { data: appointmentTypes = [] } = useAppointmentTypes(companyId);
  const { data: members = [] } = useMembers(companyId);
  const { data: contacts = [] } = useContacts(companyId);

  const memberOptions = useMemo(
    () => members.map((m) => ({ id: m.userId, name: m.name })),
    [members],
  );

  const effectiveSelectedIds = useMemo(() => {
    if (selectedMemberIds !== null) return selectedMemberIds;
    return new Set(memberOptions.map((m) => m.id));
  }, [selectedMemberIds, memberOptions]);

  const memberColorMap = useMemo(() => {
    const map = new Map<string, string>();
    memberOptions.forEach((m, i) => map.set(m.id, getMemberColor(i)));
    return map;
  }, [memberOptions]);

  const filteredAppointments = useMemo(
    () => appointments.filter((a) => effectiveSelectedIds.has(a.hostId)),
    [appointments, effectiveSelectedIds],
  );

  const filteredBlocks = useMemo(
    () => blocks.filter((b) => effectiveSelectedIds.has(b.hostId)),
    [blocks, effectiveSelectedIds],
  );

  const createAppointment = useCreateAppointment(companyId);
  const createBlock = useCreateBlock(companyId);
  const changeStatus = useChangeAppointmentStatus(companyId);
  const deleteAppointment = useDeleteAppointment(companyId);

  const goToday = () => {
    const now = new Date();
    setCurrentDate(now);
    setMiniMonth(now);
  };

  const goPrev = () => {
    setCurrentDate((d) => (view === "week" ? subWeeks(d, 1) : subDays(d, 1)));
  };

  const goNext = () => {
    setCurrentDate((d) => (view === "week" ? addWeeks(d, 1) : addDays(d, 1)));
  };

  const handleMiniSelect = useCallback((date: Date) => {
    setCurrentDate(date);
  }, []);

  const handleToggleMember = useCallback(
    (id: string) => {
      setSelectedMemberIds((prev) => {
        const current = prev ?? new Set(memberOptions.map((m) => m.id));
        const next = new Set(current);
        if (next.has(id)) next.delete(id);
        else next.add(id);
        return next;
      });
    },
    [memberOptions],
  );

  const handleToggleAllMembers = useCallback(() => {
    setSelectedMemberIds((prev) => {
      const allIds = new Set(memberOptions.map((m) => m.id));
      if (prev !== null && prev.size === allIds.size) return new Set<string>();
      return allIds;
    });
  }, [memberOptions]);

  const handleSlotClick = useCallback(
    (start: Date, end: Date) => {
      if (!perms.canCreate) return;
      setDefaultSlot({
        start: format(start, "yyyy-MM-dd'T'HH:mm"),
        end: format(end, "yyyy-MM-dd'T'HH:mm"),
      });
      setCreateOpen(true);
    },
    [perms.canCreate],
  );

  const handleAppointmentClick = useCallback((appt: Appointment) => {
    setSelectedAppointment(appt);
  }, []);

  const handleChangeStatus = (id: string, status: AppointmentStatus) => {
    changeStatus.mutate({ id, status });
    setSelectedAppointment(null);
  };

  const handleDelete = (id: string) => {
    deleteAppointment.mutate(id);
    setSelectedAppointment(null);
  };

  const handleCreateAppointment = (values: CreateAppointmentFormValues) => {
    createAppointment.mutate(
      {
        ...values,
        startAt: new Date(values.startAt).toISOString(),
        endAt: new Date(values.endAt).toISOString(),
      },
      { onSuccess: () => setCreateOpen(false) },
    );
  };

  const selectedContact = useMemo(() => {
    if (!selectedAppointment?.contactId) return null;
    return contacts.find((c) => c.id === selectedAppointment.contactId) ?? null;
  }, [selectedAppointment, contacts]);

  const selectedHostName = useMemo(() => {
    if (!selectedAppointment) return undefined;
    return memberOptions.find((m) => m.id === selectedAppointment.hostId)?.name;
  }, [selectedAppointment, memberOptions]);

  const periodLabel = useMemo(() => {
    if (view === "day") return format(currentDate, "dd 'de' MMMM yyyy", { locale: ptBR });
    const ws = startOfWeek(currentDate, { weekStartsOn: 1 });
    const we = endOfWeek(currentDate, { weekStartsOn: 1 });
    return `${format(ws, "dd MMM", { locale: ptBR })} – ${format(we, "dd MMM yyyy", { locale: ptBR })}`;
  }, [currentDate, view]);

  return (
    <div className="flex h-[calc(100vh-4rem)] flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <PageTitle>Agenda</PageTitle>

        <div className="flex items-center gap-2">
          <Button variant="outline" size="sm" onClick={goToday}>
            Hoje
          </Button>
          <Button variant="ghost" size="icon" onClick={goPrev}>
            <ChevronLeft className="h-4 w-4" />
          </Button>
          <span className="min-w-[180px] text-center text-sm font-medium">{periodLabel}</span>
          <Button variant="ghost" size="icon" onClick={goNext}>
            <ChevronRight className="h-4 w-4" />
          </Button>

          <div className="flex rounded-md border">
            <Button
              variant={view === "day" ? "default" : "ghost"}
              size="sm"
              className="rounded-r-none"
              onClick={() => setView("day")}
            >
              Dia
            </Button>
            <Button
              variant={view === "week" ? "default" : "ghost"}
              size="sm"
              className="rounded-l-none"
              onClick={() => setView("week")}
            >
              Semana
            </Button>
          </div>

          <TooltipProvider>
            <Tooltip>
              <TooltipTrigger asChild>
                <Button variant="ghost" size="icon" onClick={() => router.push("/settings/agenda")}>
                  <Settings className="h-4 w-4" />
                </Button>
              </TooltipTrigger>
              <TooltipContent>Configurações da agenda</TooltipContent>
            </Tooltip>
          </TooltipProvider>

          {perms.canCreate && (
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button>
                  <Plus className="mr-2 h-4 w-4" />
                  Novo
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end">
                <DropdownMenuItem onClick={() => setCreateOpen(true)}>Agendamento</DropdownMenuItem>
                <DropdownMenuItem onClick={() => setBlockOpen(true)}>Bloqueio</DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          )}
        </div>
      </div>

      <div className="flex flex-1 overflow-hidden">
        <aside className="hidden w-52 shrink-0 flex-col gap-4 overflow-y-auto border-r pr-3 lg:flex">
          <MiniCalendar
            selected={currentDate}
            onSelect={handleMiniSelect}
            month={miniMonth}
            onMonthChange={setMiniMonth}
          />
          <div className="border-t pt-3">
            <MemberFilter
              members={memberOptions}
              selectedIds={effectiveSelectedIds}
              onToggle={handleToggleMember}
              onToggleAll={handleToggleAllMembers}
            />
          </div>
        </aside>

        <WeeklyCalendar
          currentDate={currentDate}
          view={view}
          appointments={filteredAppointments}
          blocks={filteredBlocks}
          onSlotClick={handleSlotClick}
          onAppointmentClick={handleAppointmentClick}
          selectedAppointmentId={selectedAppointment?.id}
          memberColorMap={memberColorMap}
        />

        {selectedAppointment && (
          <AppointmentDetailPanel
            appointment={selectedAppointment}
            contact={selectedContact}
            hostName={selectedHostName}
            onClose={() => setSelectedAppointment(null)}
            onChangeStatus={handleChangeStatus}
            onDelete={handleDelete}
            canUpdate={perms.canUpdate}
            canDelete={perms.canDelete}
          />
        )}
      </div>

      <CreateAppointmentDialog
        open={createOpen}
        onOpenChange={setCreateOpen}
        isLoading={createAppointment.isPending}
        onSubmit={handleCreateAppointment}
        appointmentTypes={appointmentTypes}
        members={memberOptions}
        contacts={contacts}
        defaultStart={defaultSlot?.start}
        defaultEnd={defaultSlot?.end}
        defaultHostId={user?.id}
      />

      <CreateBlockDialog
        open={blockOpen}
        onOpenChange={setBlockOpen}
        isLoading={createBlock.isPending}
        onSubmit={(values) =>
          createBlock.mutate(
            {
              ...values,
              startAt: new Date(values.startAt).toISOString(),
              endAt: new Date(values.endAt).toISOString(),
            },
            { onSuccess: () => setBlockOpen(false) },
          )
        }
        members={memberOptions}
      />
    </div>
  );
}
