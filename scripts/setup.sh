#!/bin/bash

# CRM SaaS Omnichannel - Setup Script
# This script sets up the development environment

set -e

echo "🚀 Setting up CRM SaaS Omnichannel development environment..."

# Check prerequisites
echo "📋 Checking prerequisites..."

command -v docker >/dev/null 2>&1 || { echo "❌ Docker is required but not installed. Aborting." >&2; exit 1; }
command -v docker-compose >/dev/null 2>&1 || { echo "❌ Docker Compose is required but not installed. Aborting." >&2; exit 1; }
command -v java >/dev/null 2>&1 || { echo "❌ Java 21 is required but not installed. Aborting." >&2; exit 1; }
command -v node >/dev/null 2>&1 || { echo "❌ Node.js 20+ is required but not installed. Aborting." >&2; exit 1; }
command -v npm >/dev/null 2>&1 || { echo "❌ npm is required but not installed. Aborting." >&2; exit 1; }

echo "✅ All prerequisites found!"

# Start infrastructure services
echo "🐳 Starting infrastructure services..."
cd docker
docker-compose -f docker-compose.dev.yml up -d
cd ..

echo "⏳ Waiting for services to be ready..."
sleep 10

# Setup backend
echo "☕ Setting up backend..."
cd backend
./mvnw clean install -DskipTests
cd ..

# Setup frontend
echo "📦 Setting up frontend..."
cd frontend
npm install
cd ..

echo ""
echo "✅ Setup complete!"
echo ""
echo "📊 Services running:"
echo "   - PostgreSQL: localhost:5432"
echo "   - Redis: localhost:6379"
echo "   - RabbitMQ: localhost:5672 (Management: http://localhost:15672)"
echo "   - MinIO: localhost:9000 (Console: http://localhost:9001)"
echo ""
echo "🚀 To start the application:"
echo "   Backend:  cd backend && ./mvnw spring-boot:run"
echo "   Frontend: cd frontend && npm run dev"
echo ""
echo "📖 API Documentation: http://localhost:8080/api/v1/docs/swagger"
echo "🌐 Frontend: http://localhost:3000"
