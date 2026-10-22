export { whatsappUrl } from "@/lib/whatsapp";

export function formatDateOnly(isoDate: string): string {
  const [year, month, day] = isoDate.split("-");
  return `${day}/${month}/${year}`;
}

export function todayIsoDate(): string {
  const today = new Date();
  const month = String(today.getMonth() + 1).padStart(2, "0");
  const day = String(today.getDate()).padStart(2, "0");
  return `${today.getFullYear()}-${month}-${day}`;
}

export function isOverdue(dueDate: string): boolean {
  return dueDate < todayIsoDate();
}
