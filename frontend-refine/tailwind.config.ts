import type { Config } from "tailwindcss";

const config: Config = {
  darkMode: ["class"],
  content: [
    "./src/pages/**/*.{ts,tsx}",
    "./src/components/**/*.{ts,tsx}",
    "./src/app/**/*.{ts,tsx}",
    "./src/features/**/*.{ts,tsx}",
  ],
  theme: {
    container: {
      center: true,
      padding: "2rem",
      screens: {
        "2xl": "1400px",
      },
    },
    extend: {
      colors: {
        border: "hsl(var(--border))",
        input: "hsl(var(--input))",
        ring: "hsl(var(--ring))",
        background: "hsl(var(--background))",
        foreground: "hsl(var(--foreground))",
        primary: {
          DEFAULT: "hsl(var(--primary))",
          foreground: "hsl(var(--primary-foreground))",
        },
        secondary: {
          DEFAULT: "hsl(var(--secondary))",
          foreground: "hsl(var(--secondary-foreground))",
        },
        destructive: {
          DEFAULT: "hsl(var(--destructive))",
          foreground: "hsl(var(--destructive-foreground))",
        },
        muted: {
          DEFAULT: "hsl(var(--muted))",
          foreground: "hsl(var(--muted-foreground))",
        },
        accent: {
          DEFAULT: "hsl(var(--accent))",
          foreground: "hsl(var(--accent-foreground))",
        },
        popover: {
          DEFAULT: "hsl(var(--popover))",
          foreground: "hsl(var(--popover-foreground))",
        },
        card: {
          DEFAULT: "hsl(var(--card))",
          foreground: "hsl(var(--card-foreground))",
        },
        crm: {
          primary: "hsl(var(--crm-primary))",
          "primary-hover": "hsl(var(--crm-primary-hover))",
          "primary-active": "hsl(var(--crm-primary-active))",
          "primary-foreground": "hsl(var(--crm-primary-foreground))",
          secondary: "hsl(var(--crm-secondary))",
          text: "hsl(var(--crm-text))",
          "text-secondary": "hsl(var(--crm-text-secondary))",
          border: "hsl(var(--crm-border))",
          background: "hsl(var(--crm-background))",
          surface: "hsl(var(--crm-surface))",
          danger: "hsl(var(--crm-danger))",
          success: "hsl(var(--crm-success))",
        },
        "crm-primary-dark": "hsl(var(--crm-primary-dark) / <alpha-value>)",
        "crm-primary-darker": "hsl(var(--crm-primary-darker) / <alpha-value>)",
        "crm-secondary-purple": "hsl(var(--crm-secondary-purple) / <alpha-value>)",
        "crm-secondary-indigo": "hsl(var(--crm-secondary-indigo) / <alpha-value>)",
        "crm-tertiary-cyan": "hsl(var(--crm-tertiary-cyan) / <alpha-value>)",
        "crm-tertiary-pink": "hsl(var(--crm-tertiary-pink) / <alpha-value>)",
        "crm-tertiary-amber": "hsl(var(--crm-tertiary-amber) / <alpha-value>)",
        "crm-warning": "hsl(var(--crm-warning) / <alpha-value>)",
        "crm-error": "hsl(var(--crm-error) / <alpha-value>)",
        "crm-info": "hsl(var(--crm-info) / <alpha-value>)",
        "crm-surface-bg": "hsl(var(--crm-surface-bg) / <alpha-value>)",
        "crm-surface-light": "hsl(var(--crm-surface-light) / <alpha-value>)",
        "crm-surface-sidebar": "hsl(var(--crm-surface-sidebar) / <alpha-value>)",
        "crm-surface-sidebar-dark": "hsl(var(--crm-surface-sidebar-dark) / <alpha-value>)",
        "crm-border-dark": "hsl(var(--crm-border-dark) / <alpha-value>)",
      },
      borderRadius: {
        lg: "var(--radius)",
        md: "calc(var(--radius) - 2px)",
        sm: "calc(var(--radius) - 4px)",
      },
      backgroundImage: {
        "crm-gradient-primary": "var(--crm-gradient-primary)",
        "crm-gradient-secondary": "var(--crm-gradient-secondary)",
        "crm-gradient-cyan": "var(--crm-gradient-cyan)",
        "crm-gradient-success": "var(--crm-gradient-success)",
        "crm-gradient-warning": "var(--crm-gradient-warning)",
        "crm-gradient-error": "var(--crm-gradient-error)",
        "crm-gradient-info": "var(--crm-gradient-info)",
      },
      boxShadow: {
        "crm-sm": "var(--crm-shadow-sm)",
        "crm-md": "var(--crm-shadow-md)",
        "crm-lg": "var(--crm-shadow-lg)",
        "crm-xl": "var(--crm-shadow-xl)",
      },
      transitionDuration: {
        "crm-fast": "200ms",
        "crm-default": "300ms",
        "crm-slow": "500ms",
      },
      keyframes: {
        "accordion-down": {
          from: { height: "0" },
          to: { height: "var(--radix-accordion-content-height)" },
        },
        "accordion-up": {
          from: { height: "var(--radix-accordion-content-height)" },
          to: { height: "0" },
        },
      },
      animation: {
        "accordion-down": "accordion-down 0.2s ease-out",
        "accordion-up": "accordion-up 0.2s ease-out",
      },
    },
  },
  // eslint-disable-next-line @typescript-eslint/no-require-imports
  plugins: [require("tailwindcss-animate")],
};

export default config;
