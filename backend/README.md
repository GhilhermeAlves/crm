# CRM SaaS Omnichannel — Backend

## Tech Stack

- Java 25 LTS
- Spring Boot 3.x
- Maven
- PostgreSQL 16
- Redis 7
- RabbitMQ 3
- Flyway
- Spring Security + JWT
- OpenAPI 3.1
- MapStruct
- Lombok
- JUnit 5 + Mockito

## Architecture

Clean Architecture + Hexagonal Architecture + DDD

```
domain/        → Entities, Value Objects, Repository Interfaces
application/   → Use Cases, Commands/Queries, Services
infrastructure/ → Implementations (DB, Cache, MQ, APIs)
presentation/   → REST Controllers, Request/Response DTOs
```

## Getting Started

### Prerequisites

- Java 25
- Maven 3.9+
- PostgreSQL 16
- Redis 7
- RabbitMQ 3

### Development

```bash
# Build
./mvnw clean install

# Run
./mvnw spring-boot:run

# Run with specific profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Test
./mvnw test

# Package
./mvnw clean package -DskipTests
```

### Docker

```bash
# Build image
docker build -t crm-backend:latest .

# Run container
docker run -p 8080:8080 crm-backend:latest
```

## API Documentation

Once the application is running, access:

- Swagger UI: http://localhost:8080/api/v1/docs/swagger
- OpenAPI Docs: http://localhost:8080/api/v1/docs

## Health Check

- Health: http://localhost:8080/api/v1/actuator/health
- Metrics: http://localhost:8080/api/v1/actuator/metrics
