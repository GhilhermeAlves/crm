"use client";

import { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Plus, Trash2, Save } from "lucide-react";
import type { Availability, AvailabilityRule } from "../types/scheduling.types";

const WEEKDAY_LABELS = ["Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo"];

const TIMEZONES = [
  "America/Sao_Paulo",
  "America/Manaus",
  "America/Bahia",
  "America/Recife",
  "America/Fortaleza",
  "America/Belem",
  "America/Cuiaba",
  "America/Porto_Velho",
  "America/Rio_Branco",
  "America/Noronha",
];

interface Props {
  title?: string;
  availability: Availability | undefined;
  isLoading: boolean;
  isSaving: boolean;
  onSave: (data: Availability) => void;
}

export function AvailabilityEditor({
  title = "Minha disponibilidade",
  availability,
  isLoading,
  isSaving,
  onSave,
}: Props) {
  const [timezone, setTimezone] = useState("America/Sao_Paulo");
  const [rules, setRules] = useState<AvailabilityRule[]>([]);

  useEffect(() => {
    if (availability) {
      setTimezone(availability.timezone);
      setRules(availability.rules);
    }
  }, [availability]);

  const addRule = (weekday: number) => {
    setRules((prev) => [...prev, { weekday, startTime: "09:00", endTime: "18:00" }]);
  };

  const removeRule = (index: number) => {
    setRules((prev) => prev.filter((_, i) => i !== index));
  };

  const updateRule = (index: number, field: keyof AvailabilityRule, value: string | number) => {
    setRules((prev) => prev.map((r, i) => (i === index ? { ...r, [field]: value } : r)));
  };

  const handleSave = () => {
    onSave({
      timezone,
      rules,
      overrides: availability?.overrides ?? [],
    });
  };

  if (isLoading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="text-base">{title}</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-muted-foreground">Carregando…</p>
        </CardContent>
      </Card>
    );
  }

  const rulesByDay = WEEKDAY_LABELS.map((label, i) => ({
    label,
    weekday: i + 1,
    rules: rules
      .map((r, originalIndex) => ({ ...r, originalIndex }))
      .filter((r) => r.weekday === i + 1),
  }));

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between">
        <CardTitle className="text-base">{title}</CardTitle>
        <Button size="sm" onClick={handleSave} disabled={isSaving}>
          <Save className="mr-2 h-4 w-4" />
          {isSaving ? "Salvando…" : "Salvar"}
        </Button>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="space-y-2">
          <Label>Fuso horário</Label>
          <Select value={timezone} onValueChange={setTimezone}>
            <SelectTrigger className="w-60">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {TIMEZONES.map((tz) => (
                <SelectItem key={tz} value={tz}>
                  {tz.replace("America/", "").replace("_", " ")}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        <div className="space-y-3">
          {rulesByDay.map((day) => (
            <div key={day.weekday} className="space-y-2">
              <div className="flex items-center justify-between">
                <span className="w-20 text-sm font-medium">{day.label}</span>
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  onClick={() => addRule(day.weekday)}
                >
                  <Plus className="mr-1 h-3 w-3" />
                  Janela
                </Button>
              </div>

              {day.rules.length === 0 && (
                <p className="ml-20 text-xs text-muted-foreground">Indisponível</p>
              )}

              {day.rules.map((rule) => (
                <div key={rule.originalIndex} className="ml-20 flex items-center gap-2">
                  <Input
                    type="time"
                    value={rule.startTime}
                    onChange={(e) => updateRule(rule.originalIndex, "startTime", e.target.value)}
                    className="w-28"
                  />
                  <span className="text-sm text-muted-foreground">–</span>
                  <Input
                    type="time"
                    value={rule.endTime}
                    onChange={(e) => updateRule(rule.originalIndex, "endTime", e.target.value)}
                    className="w-28"
                  />
                  <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    onClick={() => removeRule(rule.originalIndex)}
                  >
                    <Trash2 className="h-3 w-3" />
                  </Button>
                </div>
              ))}
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  );
}
