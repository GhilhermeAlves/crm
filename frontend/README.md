# CRM SaaS Omnichannel — Frontend

## Tech Stack

- Next.js 14 (App Router)
- React 18
- TypeScript 5
- Tailwind CSS 3
- Shadcn UI
- React Query (TanStack)
- Axios
- React Hook Form
- Zod
- Socket.IO Client

## Architecture

Feature-Based with React Server Components

```
app/            → Next.js App Router pages
components/     → React components (ui, layout, features)
hooks/          → Custom React hooks
lib/            → Utilities and configurations
providers/      → Context providers
types/          → TypeScript types
styles/         → Global styles
```

## Getting Started

### Prerequisites

- Node.js 20+
- npm or yarn

### Development

```bash
# Install dependencies
npm install

# Run development server
npm run dev

# Build for production
npm run build

# Start production server
npm start

# Lint
npm run lint

# Type check
npm run typecheck

# Format
npm run format
```

### Docker

```bash
# Build image
docker build -t crm-frontend:latest .

# Run container
docker run -p 3000:3000 crm-frontend:latest
```

## Project Structure

```
src/
├── app/                    # Next.js App Router
├── components/             # React components
│   ├── ui/                 # Shadcn base components
│   ├── layout/             # Layout components
│   ├── {module}/           # Feature components
│   └── shared/             # Shared components
├── hooks/                  # Custom hooks
├── lib/                    # Utilities
├── providers/              # Context providers
├── types/                  # TypeScript types
└── styles/                 # Global styles
```

## Environment Variables

| Variable | Description | Default |
|---|---|---|
| NEXT_PUBLIC_API_URL | Backend API URL | http://localhost:8080/api/v1 |
| NEXT_PUBLIC_APP_NAME | Application name | CRM SaaS Omnichannel |
| NEXT_PUBLIC_APP_URL | Application URL | http://localhost:3000 |
| NEXT_PUBLIC_WS_URL | WebSocket URL | ws://localhost:8080 |
