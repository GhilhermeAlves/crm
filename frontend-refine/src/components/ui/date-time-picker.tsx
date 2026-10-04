"use client";

import { format, setHours, setMinutes } from "date-fns";
import { ptBR } from "date-fns/locale";
import { CalendarIcon } from "lucide-react";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Calendar } from "@/components/ui/calendar";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

interface DateTimePickerProps {
  value?: string;
  onChange: (value: string) => void;
  dateOnly?: boolean;
  placeholder?: string;
}

function generateTimeOptions() {
  const options: string[] = [];
  for (let h = 0; h < 24; h++) {
    for (let m = 0; m < 60; m += 5) {
      options.push(`${String(h).padStart(2, "0")}:${String(m).padStart(2, "0")}`);
    }
  }
  return options;
}

const TIME_OPTIONS = generateTimeOptions();

export function DateTimePicker({
  value,
  onChange,
  dateOnly = false,
  placeholder = "Selecione a data",
}: DateTimePickerProps) {
  const dateValue = value ? new Date(value) : undefined;
  const timeStr = dateValue
    ? `${String(dateValue.getHours()).padStart(2, "0")}:${String(dateValue.getMinutes()).padStart(2, "0")}`
    : "08:00";

  const handleDateSelect = (day: Date | undefined) => {
    if (!day) return;
    if (dateOnly) {
      onChange(format(day, "yyyy-MM-dd"));
      return;
    }
    const [h, m] = timeStr.split(":").map(Number);
    const dt = setMinutes(setHours(day, h), m);
    onChange(format(dt, "yyyy-MM-dd'T'HH:mm"));
  };

  const handleTimeChange = (time: string) => {
    const base = dateValue ?? new Date();
    const [h, m] = time.split(":").map(Number);
    const dt = setMinutes(setHours(base, h), m);
    onChange(format(dt, "yyyy-MM-dd'T'HH:mm"));
  };

  return (
    <div className="flex gap-2">
      <Popover modal>
        <PopoverTrigger asChild>
          <Button
            variant="outline"
            className={cn(
              "flex-1 justify-start text-left font-normal",
              !dateValue && "text-muted-foreground",
            )}
          >
            <CalendarIcon className="mr-2 h-4 w-4" />
            {dateValue
              ? format(dateValue, dateOnly ? "dd/MM/yyyy" : "dd/MM/yyyy", { locale: ptBR })
              : placeholder}
          </Button>
        </PopoverTrigger>
        <PopoverContent
          className="w-auto p-0"
          align="start"
          onPointerDownOutside={(e) => {
            const target = e.target as HTMLElement | null;
            if (target?.tagName === "OPTION" || target?.tagName === "SELECT") {
              e.preventDefault();
            }
          }}
          onFocusOutside={(e) => e.preventDefault()}
        >
          <Calendar mode="single" captionLayout="dropdown" selected={dateValue} onSelect={handleDateSelect} autoFocus />
        </PopoverContent>
      </Popover>

      {!dateOnly && (
        <Select value={timeStr} onValueChange={handleTimeChange}>
          <SelectTrigger className="w-[100px]">
            <SelectValue />
          </SelectTrigger>
          <SelectContent className="max-h-60">
            {TIME_OPTIONS.map((t) => (
              <SelectItem key={t} value={t}>
                {t}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      )}
    </div>
  );
}
