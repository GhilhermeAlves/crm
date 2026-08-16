import { cn } from "@/lib/utils";

/**
 * Área de marca da tela de login (Sprint 7.0).
 *
 * <p>Reserva um slot para o logo definitivo do CRM sem depender de um arquivo
 * de logo: enquanto nenhum SVG/PNG for fornecido ({@code logoSrc}), exibe um
 * fallback textual temporário. A troca futura resume-se a fornecer
 * {@code logoSrc} (novo SVG/PNG) — autenticação, layout, providers, rotas e
 * backend permanecem inalterados.
 *
 * <ul>
 *   <li>{@code size} — dimensão da área do logo (sm/md/lg);</li>
 *   <li>{@code variant} — {@code desktop} (marca + wordmark lado a lado) ou
 *       {@code mobile} (empilhado/compacto);</li>
 *   <li>{@code wordmark} — texto temporário exibido quando não há logo.</li>
 * </ul>
 */
export type LoginBrandProps = {
  /** URL do logo definitivo (SVG/PNG futuro). Omita para usar o fallback. */
  logoSrc?: string;
  logoAlt?: string;
  size?: "sm" | "md" | "lg";
  variant?: "desktop" | "mobile";
  wordmark?: string;
  className?: string;
};

const MARK_SIZES = {
  sm: "h-10 w-10 rounded-xl text-lg",
  md: "h-14 w-14 rounded-2xl text-2xl",
  lg: "h-20 w-20 rounded-2xl text-3xl",
} as const;

const SLOT_SIZES = {
  sm: "h-10 w-10",
  md: "h-14 w-14",
  lg: "h-20 w-20",
} as const;

export function LoginBrand({
  logoSrc,
  logoAlt = "Logo CRM",
  size = "md",
  variant = "desktop",
  wordmark = "CRM",
  className,
}: LoginBrandProps) {
  const mark = logoSrc ? (
    // eslint-disable-next-line @next/next/no-img-element
    <img
      src={logoSrc}
      alt={logoAlt}
      className="h-full w-full object-contain"
      data-testid="login-brand-logo"
    />
  ) : (
    <div
      className={cn(
        "flex h-full w-full select-none items-center justify-center bg-crm-primary font-semibold tracking-tight text-crm-primary-foreground",
        MARK_SIZES[size],
      )}
      data-testid="login-brand-fallback"
    >
      {wordmark.slice(0, 2).toUpperCase()}
    </div>
  );

  const wordmarkElement = wordmark && (
    <span className="text-xl font-semibold tracking-tight text-crm-text">
      {wordmark}
    </span>
  );

  return (
    <div
      className={cn(
        "flex items-center justify-center",
        variant === "desktop" ? "gap-3" : "flex-col gap-2",
        className,
      )}
      data-testid="login-brand"
    >
      <div data-logo-slot className={cn("shrink-0", SLOT_SIZES[size])}>
        {mark}
      </div>
      {variant === "desktop" && wordmarkElement}
    </div>
  );
}
