#!/bin/bash

# CRM SaaS Omnichannel - Seed Script
# This script seeds the database with initial data

set -e

echo "🌱 Seeding database..."

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker first."
    exit 1
fi

# Check if PostgreSQL is running
if ! docker ps | grep -q crm-postgres; then
    echo "❌ PostgreSQL is not running. Starting services..."
    cd docker
    docker-compose -f docker-compose.dev.yml up -d postgres
    cd ..
    sleep 5
fi

echo "📦 Running Flyway migrations..."
cd backend
./mvnw flyway:migrate -Dspring.datasource.url=jdbc:postgresql://localhost:5432/crm_dev -Dspring.datasource.username=postgres -Dspring.datasource.password=postgres
cd ..

echo ""
echo "✅ Database seeded successfully!"
echo ""
echo "📊 Database: crm_dev"
echo "👤 Default user: admin@becommerce.com (password: Admin@123)"
