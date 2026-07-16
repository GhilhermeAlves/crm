# Docker Configuration

## Files

| File | Description |
|---|---|
| docker-compose.yml | Main compose file (all services) |
| docker-compose.dev.yml | Development services (DB, Cache, MQ) |
| docker-compose.prod.yml | Production configuration |

## Services

| Service | Image | Port | Description |
|---|---|---|---|
| backend | crm-backend:latest | 8080 | API Backend |
| frontend | crm-frontend:latest | 3000 | Frontend |
| postgres | postgres:16 | 5432 | Database |
| redis | redis:7 | 6379 | Cache |
| rabbitmq | rabbitmq:3-management | 5672 | Message Broker |
| minio | minio/minio | 9000 | S3 Storage |

## Commands

```bash
# Start all services
docker-compose up -d

# Start only development services
docker-compose -f docker-compose.dev.yml up -d

# View logs
docker-compose logs -f backend

# Stop all services
docker-compose down

# Rebuild and start
docker-compose up -d --build
```

## Development

For development, use `docker-compose.dev.yml` which only starts infrastructure services:

```bash
docker-compose -f docker-compose.dev.yml up -d
```

Then run the backend and frontend locally with your IDE.

## Production

For production, use `docker-compose.prod.yml`:

```bash
# Create .env file with production values
cp .env.example .env

# Start all services
docker-compose -f docker-compose.prod.yml up -d
```

## Health Checks

All services have health checks configured. The backend depends on:
- PostgreSQL (ready)
- Redis (ready)
- RabbitMQ (ready)
