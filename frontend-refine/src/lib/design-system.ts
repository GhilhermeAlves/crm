// src/lib/design-system.ts
export const designTokens = {
  colors: {
    primary: {
      base: "#3b82f6",
      dark: "#1e40af",
      darker: "#1e3a8a",
    },
    secondary: {
      purple: "#a855f7",
      indigo: "#6366f1",
    },
    tertiary: {
      cyan: "#06b6d4",
      pink: "#ec4899",
      amber: "#f59e0b",
    },
    semantic: {
      success: "#10b981",
      warning: "#ea580c",
      error: "#ef4444",
      info: "#14b8a6",
    },
  },
  gradients: {
    primary: "linear-gradient(135deg, #3b82f6 0%, #1e40af 100%)",
    secondary: "linear-gradient(135deg, #a855f7 0%, #6d28d9 100%)",
    cyan: "linear-gradient(135deg, #06b6d4 0%, #0369a1 100%)",
    success: "linear-gradient(135deg, #10b981 0%, #065f46 100%)",
    warning: "linear-gradient(135deg, #ea580c 0%, #9a3412 100%)",
    error: "linear-gradient(135deg, #ef4444 0%, #991b1b 100%)",
    info: "linear-gradient(135deg, #14b8a6 0%, #0d9488 100%)",
  },
  shadows: {
    sm: "0 1px 2px rgba(0, 0, 0, 0.05)",
    md: "0 4px 6px rgba(0, 0, 0, 0.1)",
    lg: "0 10px 15px rgba(0, 0, 0, 0.1)",
    xl: "0 20px 25px rgba(0, 0, 0, 0.1)",
  },
  transitions: {
    fast: "all 0.2s ease",
    default: "all 0.3s ease",
    slow: "all 0.5s ease",
  },
} as const;
