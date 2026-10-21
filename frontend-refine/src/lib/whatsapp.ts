export function buildWhatsAppUrl(phone?: string | null, message?: string): string {
  if (!phone) {
    return "#";
  }
  const cleaned = phone.replace(/[^0-9]/g, "");
  if (!cleaned) {
    return "#";
  }
  let url = `https://wa.me/${cleaned}`;
  if (message && message.trim()) {
    const encoded = encodeURIComponent(message.trim());
    url += `?text=${encoded}`;
  }
  return url;
}

export const whatsappUrl = buildWhatsAppUrl;
