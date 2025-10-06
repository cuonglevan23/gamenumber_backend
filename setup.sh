#!/bin/bash

echo "🚀 Starting Number Guessing Game Setup..."
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker first."
    exit 1
fi

echo "✅ Docker is running"
echo ""

# Stop and remove existing containers
echo "🧹 Cleaning up old containers..."
docker-compose down -v

echo ""
echo "🏗️  Building and starting services..."
docker-compose up --build -d

echo ""
echo "⏳ Waiting for services to be ready..."
sleep 10

# Check MySQL
echo -n "Checking MySQL... "
until docker-compose exec -T mysql mysqladmin ping -h localhost --silent 2>/dev/null; do
    echo -n "."
    sleep 2
done
echo "✅"

# Check Redis
echo -n "Checking Redis... "
until docker-compose exec -T redis redis-cli ping 2>/dev/null | grep -q PONG; do
    echo -n "."
    sleep 2
done
echo "✅"

# Check Spring Boot App
echo -n "Checking Spring Boot App... "
sleep 15
until curl -s http://localhost:8080/api/leaderboard > /dev/null 2>&1; do
    echo -n "."
    sleep 3
done
echo "✅"

echo ""
echo "════════════════════════════════════════════════════"
echo "🎉 Setup Complete! All services are running!"
echo "════════════════════════════════════════════════════"
echo ""
echo "📍 Application URL: http://localhost:8080"
echo "📊 MySQL: localhost:3306"
echo "🔴 Redis: localhost:6379"
echo ""
echo "🔧 Useful commands:"
echo "  - View logs:        docker-compose logs -f"
echo "  - Stop services:    docker-compose down"
echo "  - Restart:          docker-compose restart"
echo ""
echo "📚 Check README.md for API documentation"
echo ""
echo "🧪 Quick test:"
echo "curl http://localhost:8080/api/leaderboard"
echo ""

