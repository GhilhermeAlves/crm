"use client";

import { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { ALL_BLOCKS, BUDGET_BLOCKS, parseBlocks, type BudgetBlock } from "../utils/blocks";

const STORAGE_KEY = "budget-view-blocks";

function loadBlocks(): BudgetBlock[] {
  try {
    return parseBlocks(window.localStorage.getItem(STORAGE_KEY));
  } catch {
    return ALL_BLOCKS;
  }
}

function saveBlocks(blocks: BudgetBlock[]) {
  try {
    window.localStorage.setItem(STORAGE_KEY, blocks.join(","));
  } catch {
    // armazenamento indisponível: a escolha simplesmente não é lembrada
  }
}

type Props = {
  contactId: string;
  budgetId: string | null;
  onOpenChange: (open: boolean) => void;
};

export function BudgetViewDialog({ contactId, budgetId, onOpenChange }: Props) {
  const [blocks, setBlocks] = useState<BudgetBlock[]>(ALL_BLOCKS);

  useEffect(() => {
    if (budgetId) setBlocks(loadBlocks());
  }, [budgetId]);

  const toggle = (key: BudgetBlock, checked: boolean) => {
    const next = ALL_BLOCKS.filter((b) => (b === key ? checked : blocks.includes(b)));
    setBlocks(next);
    saveBlocks(next);
  };

  const openPrint = () => {
    if (!budgetId || typeof window === "undefined") return;
    window.open(
      `/patients/${contactId}/orcamentos/${budgetId}/imprimir?blocos=${blocks.join(",")}`,
    );
  };

  return (
    <Dialog open={budgetId != null} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Visualizar orçamento</DialogTitle>
          <DialogDescription>Escolha o que será exibido na impressão.</DialogDescription>
        </DialogHeader>
        <div className="space-y-2">
          {BUDGET_BLOCKS.map(({ key, label }) => (
            <div key={key} className="flex items-center gap-2">
              <Checkbox
                id={`budget-block-${key}`}
                checked={blocks.includes(key)}
                onCheckedChange={(checked) => toggle(key, checked === true)}
              />
              <Label htmlFor={`budget-block-${key}`}>{label}</Label>
            </div>
          ))}
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Fechar
          </Button>
          <Button onClick={openPrint}>Abrir para impressão</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
