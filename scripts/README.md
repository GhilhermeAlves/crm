# Scripts

Utility scripts for the CRM SaaS Omnichannel project.

## Scripts

| Script | Description |
|---|---|
| setup.sh | Set up development environment |
| seed.sh | Seed database with initial data |
| deploy.sh | Deploy to production |

## Usage

### Setup Development Environment

```bash
chmod +x scripts/setup.sh
./scripts/setup.sh
```

This script:
1. Checks prerequisites (Docker, Java, Node.js)
2. Starts infrastructure services
3. Installs backend dependencies
4. Installs frontend dependencies

### Seed Database

```bash
chmod +x scripts/seed.sh
./scripts/seed.sh
```

This script:
1. Checks if PostgreSQL is running
2. Runs Flyway migrations
3. Seeds initial data

### Deploy to Production

```bash
chmod +x scripts/deploy.sh
./scripts/deploy.sh
```

This script:
1. Checks for .env file
2. Builds Docker images
3. Starts production services

## Prerequisites

- Docker and Docker Compose
- Java 25
- Node.js 20+
- npm
