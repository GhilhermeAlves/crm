"use client";

import { useState, useMemo, useCallback } from "react";
import {
  format,
  startOfWeek,
  endOfWeek,
  startOfMonth,
  endOfMonth,
  eachDayOfInterval,
  startOfDay,
  endOfDay,
  addWeeks,
  subWeeks,
  addMonths,
  subMonths,
  addDays,
  subDays,
  isSameDay,
  isSameMonth,
  parseISO,
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
  ExternalLink,
  SlidersHorizontal,
} from "lucide-react";
import Link from "next/link";
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
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { Input } from "@/components/ui/input";
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
import { getHolidayForDate } from "@/lib/holidays";
import { isWeekend } from "date-fns";

type CalendarView = "day" | "week" | "month" | "list" | "professionals";
type AgendaTab = "agenda" | "retornos";

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
  const [showHolidays, setShowHolidays] = useState(true);
  const [showWeekends, setShowWeekends] = useState(true);
  const [showBirthdays, setShowBirthdays] = useState(true);
  const [workHoursStart, setWorkHoursStart] = useState("08:00");
  const [workHoursEnd, setWorkHoursEnd] = useState("18:00");

  const range = useMemo(() => {
    if (view === "day") {
      return {
        from: startOfDay(currentDate).toISOString(),
        to: endOfDay(currentDate).toISOString(),
      };
    }
    if (view === "month" || view === "list") {
      return {
        from: startOfDay(startOfMonth(currentDate)).toISOString(),
        to: endOfDay(endOfMonth(currentDate)).toISOString(),
      };
    }
    if (view === "professionals") {
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
    if (user?.id) return new Set([user.id]);
    return new Set(memberOptions.map((m) => m.id));
  }, [selectedMemberIds, memberOptions, user?.id]);

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
    setCurrentDate((d) => {
      if (view === "month" || view === "list") return subMonths(d, 1);
      if (view === "week") return subWeeks(d, 1);
      return subDays(d, 1);
    });
  };

  const goNext = () => {
    setCurrentDate((d) => {
      if (view === "month" || view === "list") return addMonths(d, 1);
      if (view === "week") return addWeeks(d, 1);
      return addDays(d, 1);
    });
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
    if (view === "day" || view === "professionals")
      return format(currentDate, "dd 'de' MMMM yyyy", { locale: ptBR });
    if (view === "month" || view === "list")
      return format(currentDate, "MMMM 'de' yyyy", { locale: ptBR });
    const ws = startOfWeek(currentDate, { weekStartsOn: 1 });
    const we = endOfWeek(currentDate, { weekStartsOn: 1 });
    return `${format(ws, "dd MMM", { locale: ptBR })} – ${format(we, "dd MMM yyyy", { locale: ptBR })}`;
  }, [currentDate, view]);

  return (
    <div className="-m-4 flex h-[calc(100%+2rem)] flex-col lg:-m-6 lg:h-[calc(100%+3rem)]">
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

        {/* View selector + options */}
        <Popover>
          <PopoverTrigger asChild>
            <Button variant="outline" size="sm" className="h-8 gap-1.5">
              <SlidersHorizontal className="h-3.5 w-3.5" />
              {view === "day"
                ? "Dia"
                : view === "week"
                  ? "Semana"
                  : view === "month"
                    ? "Mês"
                    : view === "list"
                      ? "Lista"
                      : "Profissionais"}
            </Button>
          </PopoverTrigger>
          <PopoverContent align="start" className="w-64 space-y-4 p-4">
            <div className="space-y-1">
              {(
                [
                  ["day", "Dia"],
                  ["week", "Semana"],
                  ["month", "Mês"],
                  ["list", "Lista"],
                  ["professionals", "Profissionais"],
                ] as const
              ).map(([value, label]) => (
                <button
                  key={value}
                  className={cn(
                    "flex w-full items-center gap-2 rounded-md px-2 py-1.5 text-sm transition-colors",
                    view === value ? "bg-primary text-primary-foreground" : "hover:bg-muted",
                  )}
                  onClick={() => setView(value)}
                >
                  <span
                    className={cn(
                      "h-3 w-3 rounded-full border-2",
                      view === value
                        ? "border-primary-foreground bg-primary-foreground"
                        : "border-muted-foreground",
                    )}
                  />
                  {label}
                </button>
              ))}
            </div>

            <div className="border-t pt-3">
              <Label className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                Horário de trabalho
              </Label>
              <div className="mt-2 flex items-center gap-2">
                <Input
                  type="time"
                  value={workHoursStart}
                  onChange={(e) => setWorkHoursStart(e.target.value)}
                  className="h-8 text-xs"
                />
                <span className="text-xs text-muted-foreground">até</span>
                <Input
                  type="time"
                  value={workHoursEnd}
                  onChange={(e) => setWorkHoursEnd(e.target.value)}
                  className="h-8 text-xs"
                />
              </div>
            </div>

            <div className="space-y-3 border-t pt-3">
              <div className="flex items-center justify-between">
                <Label htmlFor="toggle-birthdays" className="text-sm">
                  Aniversários
                </Label>
                <Switch
                  id="toggle-birthdays"
                  checked={showBirthdays}
                  onCheckedChange={setShowBirthdays}
                />
              </div>
              <div className="flex items-center justify-between">
                <Label htmlFor="toggle-holidays" className="text-sm">
                  Feriados
                </Label>
                <Switch
                  id="toggle-holidays"
                  checked={showHolidays}
                  onCheckedChange={setShowHolidays}
                />
              </div>
              <div className="flex items-center justify-between">
                <Label htmlFor="toggle-weekends" className="text-sm">
                  Finais de semanas
                </Label>
                <Switch
                  id="toggle-weekends"
                  checked={showWeekends}
                  onCheckedChange={setShowWeekends}
                />
              </div>
            </div>
          </PopoverContent>
        </Popover>

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
          {/* Action button */}
          {perms.canCreate && (
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
          )}

          <MiniCalendar
            selected={currentDate}
            onSelect={handleMiniSelect}
            month={miniMonth}
            onMonthChange={setMiniMonth}
          />

          <div className="border-t pt-3">
            <div className="mb-2 flex items-center justify-between">
              <span className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                Profissionais
              </span>
              <Link
                href="/settings/users"
                className="text-muted-foreground transition-colors hover:text-foreground"
              >
                <ExternalLink className="h-3.5 w-3.5" />
              </Link>
            </div>
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
          view === "month" ? (
            <MonthView
              currentDate={currentDate}
              appointments={filteredAppointments}
              onDayClick={(d) => {
                setCurrentDate(d);
                setView("day");
              }}
              onAppointmentClick={handleAppointmentClick}
              memberColorMap={memberColorMap}
              showHolidays={showHolidays}
              showWeekends={showWeekends}
            />
          ) : view === "list" ? (
            <ListView
              appointments={filteredAppointments}
              onAppointmentClick={handleAppointmentClick}
              memberColorMap={memberColorMap}
              memberOptions={memberOptions}
            />
          ) : view === "professionals" ? (
            <div className="flex flex-1 items-center justify-center text-muted-foreground">
              <p className="text-sm">Visão por profissional — em breve</p>
            </div>
          ) : (
            <WeeklyCalendar
              currentDate={currentDate}
              view={view as "day" | "week"}
              appointments={filteredAppointments}
              blocks={filteredBlocks}
              onSlotClick={handleSlotClick}
              onAppointmentClick={handleAppointmentClick}
              selectedAppointmentId={selectedAppointment?.id}
              memberColorMap={memberColorMap}
              startHour={parseInt(workHoursStart.split(":")[0], 10)}
              endHour={parseInt(workHoursEnd.split(":")[0], 10) || 24}
              showWeekends={showWeekends}
            />
          )
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

/* ── MonthView ── */

function MonthView({
  currentDate,
  appointments,
  onDayClick,
  onAppointmentClick,
  memberColorMap,
  showHolidays,
  showWeekends,
}: {
  currentDate: Date;
  appointments: Appointment[];
  onDayClick: (d: Date) => void;
  onAppointmentClick: (a: Appointment) => void;
  memberColorMap: Map<string, string>;
  showHolidays: boolean;
  showWeekends: boolean;
}) {
  const monthStart = startOfMonth(currentDate);
  const monthEnd = endOfMonth(currentDate);

  const calStart = startOfWeek(monthStart, { weekStartsOn: 1 });
  const calEnd = endOfWeek(monthEnd, { weekStartsOn: 1 });
  const allDays = eachDayOfInterval({ start: calStart, end: calEnd });

  const days = showWeekends ? allDays : allDays.filter((d) => !isWeekend(d));
  const weekDayLabels = showWeekends
    ? ["Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom"]
    : ["Seg", "Ter", "Qua", "Qui", "Sex"];

  return (
    <div className="flex flex-1 flex-col overflow-hidden rounded-lg border bg-card">
      <div
        className={cn("grid border-b bg-muted/30", showWeekends ? "grid-cols-7" : "grid-cols-5")}
      >
        {weekDayLabels.map((wd) => (
          <div
            key={wd}
            className="border-r py-2 text-center text-xs font-medium text-muted-foreground last:border-r-0"
          >
            {wd}
          </div>
        ))}
      </div>
      <div
        className={cn("grid flex-1 overflow-hidden", showWeekends ? "grid-cols-7" : "grid-cols-5")}
      >
        {days.map((day) => {
          const isCurrentMonth = isSameMonth(day, currentDate);
          const today = isSameDay(day, new Date());
          const dayAppts = appointments.filter((a) => isSameDay(parseISO(a.startAt), day));
          const holiday = showHolidays ? getHolidayForDate(day) : undefined;

          return (
            <div
              key={day.toISOString()}
              className={cn(
                "min-h-[80px] cursor-pointer border-b border-r p-1 transition-colors last:border-r-0 hover:bg-muted/20",
                !isCurrentMonth && "bg-muted/10 text-muted-foreground/50",
                holiday && "bg-red-50 dark:bg-red-950/20",
              )}
              onClick={() => onDayClick(day)}
            >
              <div className="flex items-center gap-1">
                <span
                  className={cn(
                    "inline-flex h-6 w-6 items-center justify-center rounded-full text-xs font-medium",
                    today && "bg-primary text-primary-foreground",
                    holiday && !today && "text-red-600 dark:text-red-400",
                  )}
                >
                  {format(day, "d")}
                </span>
                {holiday && (
                  <span className="truncate text-[9px] font-medium text-red-600 dark:text-red-400">
                    {holiday.name}
                  </span>
                )}
              </div>
              <div className="mt-0.5 space-y-0.5">
                {dayAppts.slice(0, 3).map((appt) => {
                  const color = memberColorMap.get(appt.hostId);
                  return (
                    <button
                      key={appt.id}
                      className="flex w-full items-center gap-1 truncate rounded px-1 py-0.5 text-left text-[10px] leading-tight hover:bg-muted/40"
                      onClick={(e) => {
                        e.stopPropagation();
                        onAppointmentClick(appt);
                      }}
                    >
                      <span
                        className="inline-block h-1.5 w-1.5 shrink-0 rounded-full"
                        style={{ backgroundColor: color ?? "hsl(var(--primary))" }}
                      />
                      <span className="truncate">
                        {format(parseISO(appt.startAt), "HH:mm")} {appt.title}
                      </span>
                    </button>
                  );
                })}
                {dayAppts.length > 3 && (
                  <span className="block px-1 text-[10px] text-muted-foreground">
                    +{dayAppts.length - 3} mais
                  </span>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

/* ── ListView ── */

function ListView({
  appointments,
  onAppointmentClick,
  memberColorMap,
  memberOptions,
}: {
  appointments: Appointment[];
  onAppointmentClick: (a: Appointment) => void;
  memberColorMap: Map<string, string>;
  memberOptions: { id: string; name: string }[];
}) {
  const sorted = useMemo(
    () => [...appointments].sort((a, b) => a.startAt.localeCompare(b.startAt)),
    [appointments],
  );

  const grouped = useMemo(() => {
    const map = new Map<string, Appointment[]>();
    sorted.forEach((appt) => {
      const key = format(parseISO(appt.startAt), "yyyy-MM-dd");
      const list = map.get(key) ?? [];
      list.push(appt);
      map.set(key, list);
    });
    return map;
  }, [sorted]);

  const memberNameMap = useMemo(() => {
    const m = new Map<string, string>();
    memberOptions.forEach((mo) => m.set(mo.id, mo.name));
    return m;
  }, [memberOptions]);

  if (sorted.length === 0) {
    return (
      <div className="flex flex-1 items-center justify-center text-muted-foreground">
        <p className="text-sm">Nenhum agendamento neste mês.</p>
      </div>
    );
  }

  return (
    <div className="flex-1 overflow-y-auto rounded-lg border bg-card p-4 [-ms-overflow-style:none] [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
      <div className="space-y-4">
        {Array.from(grouped.entries()).map(([dateKey, appts]) => (
          <div key={dateKey}>
            <h3 className="mb-2 text-sm font-semibold text-muted-foreground">
              {format(parseISO(dateKey), "EEEE, dd 'de' MMMM", { locale: ptBR })}
            </h3>
            <div className="space-y-1">
              {appts.map((appt) => {
                const color = memberColorMap.get(appt.hostId);
                return (
                  <button
                    key={appt.id}
                    className="flex w-full items-center gap-3 rounded-md border px-3 py-2 text-left text-sm transition-colors hover:bg-muted/40"
                    onClick={() => onAppointmentClick(appt)}
                  >
                    <span
                      className="inline-block h-3 w-3 shrink-0 rounded-full"
                      style={{ backgroundColor: color ?? "hsl(var(--primary))" }}
                    />
                    <span className="w-24 shrink-0 tabular-nums text-muted-foreground">
                      {format(parseISO(appt.startAt), "HH:mm")} –{" "}
                      {format(parseISO(appt.endAt), "HH:mm")}
                    </span>
                    <span className="flex-1 truncate font-medium">{appt.title}</span>
                    <span className="shrink-0 text-xs text-muted-foreground">
                      {memberNameMap.get(appt.hostId) ?? ""}
                    </span>
                  </button>
                );
              })}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
