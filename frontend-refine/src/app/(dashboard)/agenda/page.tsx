"use client";

import { useState, useMemo, useCallback, useEffect } from "react";
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
import {
  ChevronLeft,
  ChevronRight,
  Plus,
  Settings,
  CalendarPlus,
  UserPlus,
  Lock,
  Link2,
} from "lucide-react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { useMembers } from "@/features/identity/members/hooks/useMembers";
import { useContacts } from "@/features/masterdata/contacts/hooks/useContacts";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from "@/components/ui/tooltip";
import { WeeklyCalendar } from "@/features/sales/scheduling/components/WeeklyCalendar";
import { CreateAppointmentDialog } from "@/features/sales/scheduling/components/CreateAppointmentDialog";
import type { CreateBlockFormValues } from "@/features/sales/scheduling/schemas/scheduling.schema";
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
import { cn } from "@/lib/utils";

type CalendarView = "day" | "week";
type AgendaTab = "agenda" | "retornos";

const MIN_HOUR_HEIGHT = 30;
const MAX_HOUR_HEIGHT = 120;
const DEFAULT_HOUR_HEIGHT = 60;
const ZOOM_STEP = 10;

function clampZoom(v: number) {
  return Math.max(MIN_HOUR_HEIGHT, Math.min(MAX_HOUR_HEIGHT, v));
}

export default function AgendaPage() {
  const { user } = useAuth();
  const router = useRouter();
  const companyId = user?.companyId ?? null;
  const perms = useSchedulingPermissions();

  const [currentDate, setCurrentDate] = useState(new Date());
  const [miniMonth, setMiniMonth] = useState(new Date());
  const [view, setView] = useState<CalendarView>("week");
  const [activeTab, setActiveTab] = useState<AgendaTab>("agenda");
  const [createOpen, setCreateOpen] = useState(false);
  const [selectedAppointment, setSelectedAppointment] = useState<Appointment | null>(null);
  const [defaultSlot, setDefaultSlot] = useState<{
    start: string;
    end: string;
  } | null>(null);
  const [selectedMemberIds, setSelectedMemberIds] = useState<Set<string> | null>(null);
  const [hourHeight, setHourHeight] = useState(DEFAULT_HOUR_HEIGHT);

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

  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if (!e.ctrlKey && !e.metaKey) return;
      if (e.key === "=" || e.key === "+") {
        e.preventDefault();
        setHourHeight((h) => clampZoom(h + ZOOM_STEP));
      } else if (e.key === "-") {
        e.preventDefault();
        setHourHeight((h) => clampZoom(h - ZOOM_STEP));
      } else if (e.key === "0") {
        e.preventDefault();
        setHourHeight(DEFAULT_HOUR_HEIGHT);
      }
    };
    window.addEventListener("keydown", handler);
    return () => window.removeEventListener("keydown", handler);
  }, []);

  const handleCalendarZoom = useCallback((delta: number) => {
    setHourHeight((h) => clampZoom(h + delta * ZOOM_STEP));
  }, []);

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
    <div className="flex h-[calc(100vh-4rem)] flex-col">
      {/* ── Toolbar (Capim style) ── */}
      <div className="flex shrink-0 items-center gap-2 border-b px-3 py-2">
        {/* Left: Hoje + nav */}
        <Button variant="outline" size="sm" onClick={goToday}>
          Hoje
        </Button>
        <Button variant="ghost" size="icon" className="h-8 w-8" onClick={goPrev}>
          <ChevronLeft className="h-4 w-4" />
        </Button>
        <Button variant="ghost" size="icon" className="h-8 w-8" onClick={goNext}>
          <ChevronRight className="h-4 w-4" />
        </Button>

        <span className="min-w-[160px] text-center text-sm font-medium tabular-nums">
          {periodLabel}
        </span>

        {/* View selector */}
        <Select value={view} onValueChange={(v) => setView(v as CalendarView)}>
          <SelectTrigger className="h-8 w-[110px]">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="day">Dia</SelectItem>
            <SelectItem value="week">Semana</SelectItem>
          </SelectContent>
        </Select>

        <div className="mx-auto" />

        {/* Right: Agenda/Retornos toggle + settings */}
        <div className="flex items-center rounded-md border">
          <button
            className={cn(
              "px-3 py-1 text-sm font-medium transition-colors",
              activeTab === "agenda"
                ? "bg-primary text-primary-foreground"
                : "text-muted-foreground hover:text-foreground",
            )}
            onClick={() => setActiveTab("agenda")}
          >
            Agenda
          </button>
          <button
            className={cn(
              "px-3 py-1 text-sm font-medium transition-colors",
              activeTab === "retornos"
                ? "bg-primary text-primary-foreground"
                : "text-muted-foreground hover:text-foreground",
            )}
            onClick={() => setActiveTab("retornos")}
          >
            Retornos
          </button>
        </div>

        <TooltipProvider delayDuration={300}>
          <Tooltip>
            <TooltipTrigger asChild>
              <Button
                variant="ghost"
                size="icon"
                className="h-8 w-8"
                onClick={() => router.push("/settings/agenda")}
              >
                <Settings className="h-4 w-4" />
              </Button>
            </TooltipTrigger>
            <TooltipContent>Configurações da agenda</TooltipContent>
          </Tooltip>
        </TooltipProvider>
      </div>

      {/* ── Body ── */}
      <div className="flex flex-1 overflow-hidden">
        {/* Sidebar (Capim style: Ações + Criar + mini cal + members) */}
        <aside className="hidden w-52 shrink-0 flex-col gap-3 overflow-y-auto border-r p-3 lg:flex">
          {/* Action buttons */}
          {perms.canCreate && (
            <div className="flex flex-col gap-2">
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button variant="outline" size="sm" className="w-full justify-start">
                    <Plus className="mr-1.5 h-3.5 w-3.5" />
                    Ações
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="start">
                  <DropdownMenuItem onClick={() => setCreateOpen(true)}>
                    <CalendarPlus className="mr-2 h-4 w-4" />
                    Novo agendamento
                  </DropdownMenuItem>
                  <DropdownMenuItem onClick={() => router.push("/contacts/new")}>
                    <UserPlus className="mr-2 h-4 w-4" />
                    Novo contato
                  </DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>

              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button size="sm" className="w-full justify-start">
                    <Plus className="mr-1.5 h-3.5 w-3.5" />
                    Criar
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="start">
                  <DropdownMenuItem onClick={() => setCreateOpen(true)}>
                    <CalendarPlus className="mr-2 h-4 w-4" />
                    Agendamento
                  </DropdownMenuItem>
                  <DropdownMenuItem onClick={() => setCreateOpen(true)}>
                    <Lock className="mr-2 h-4 w-4" />
                    Bloqueio de agenda
                  </DropdownMenuItem>
                  <DropdownMenuSeparator />
                  <DropdownMenuItem disabled>
                    <Link2 className="mr-2 h-4 w-4" />
                    Link de agendamento
                  </DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
            </div>
          )}

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

        {/* Calendar grid */}
        {activeTab === "agenda" ? (
          <WeeklyCalendar
            currentDate={currentDate}
            view={view}
            appointments={filteredAppointments}
            blocks={filteredBlocks}
            onSlotClick={handleSlotClick}
            onAppointmentClick={handleAppointmentClick}
            selectedAppointmentId={selectedAppointment?.id}
            memberColorMap={memberColorMap}
            hourHeight={hourHeight}
            onZoom={handleCalendarZoom}
          />
        ) : (
          <div className="flex flex-1 items-center justify-center text-muted-foreground">
            <p className="text-sm">Retornos — em breve</p>
          </div>
        )}

        {/* Detail panel */}
        {selectedAppointment && activeTab === "agenda" && (
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

      {/* ── Dialogs ── */}
      <CreateAppointmentDialog
        open={createOpen}
        onOpenChange={setCreateOpen}
        isLoading={createAppointment.isPending}
        onSubmit={handleCreateAppointment}
        onSubmitBlock={(values) =>
          createBlock.mutate(
            {
              ...values,
              startAt: new Date(values.startAt).toISOString(),
              endAt: new Date(values.endAt).toISOString(),
            },
            { onSuccess: () => setCreateOpen(false) },
          )
        }
        isLoadingBlock={createBlock.isPending}
        appointmentTypes={appointmentTypes}
        members={memberOptions}
        contacts={contacts}
        defaultStart={defaultSlot?.start}
        defaultEnd={defaultSlot?.end}
        defaultHostId={user?.id}
      />
    </div>
  );
}
