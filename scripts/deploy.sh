#!/bin/bash

# CRM SaaS Omnichannel - Deploy Script
# This script deploys the application to production

set -e

echo "🚀 Deploying CRM SaaS Omnichannel to production..."

# Check prerequisites
echo "📋 Checking prerequisites..."

command -v docker >/dev/null 2>&1 || { echo "❌ Docker is required but not installed. Aborting." >&2; exit 1; }
command -v docker-compose >/dev/null 2>&1 || { echo "❌ Docker Compose is required but not installed. Aborting." >&2; exit 1; }

# Check for .env file
if [ ! -f docker/.env ]; then
    echo "❌ docker/.env file not found. Please create it from .env.example"
    exit 1
fi

echo "✅ Prerequisites checked!"

# Build images
echo "🐳 Building Docker images..."
cd docker
docker-compose -f docker-compose.prod.yml build
cd ..

# Deploy
echo "🚀 Starting production services..."
cd docker
docker-compose -f docker-compose.prod.yml up -d
cd ..

echo ""
echo "✅ Deployment complete!"
echo ""
echo "📊 Services running:"
echo "   - Backend: http://localhost:8080"
echo "   - Frontend: http://localhost:3000"
echo "   - PostgreSQL: localhost:5432"
echo "   - Redis: localhost:6379"
echo "   - RabbitMQ: localhost:5672"
echo "   - MinIO: http://localhost:9001"
