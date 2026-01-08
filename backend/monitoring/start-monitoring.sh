#!/bin/bash

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Запуск системы мониторинга${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

if ! command -v docker &> /dev/null; then
    echo -e "${RED}Docker не установлен!${NC}"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}Docker Compose не установлен!${NC}"
    exit 1
fi

echo -e "${YELLOW}1. Запуск Prometheus и Grafana...${NC}"
cd ..
docker-compose -f docker-compose.monitoring.yml up -d

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Контейнеры запущены${NC}"
else
    echo -e "${RED}✗ Ошибка запуска контейнеров${NC}"
    exit 1
fi

echo ""
echo -e "${YELLOW}2. Ожидание готовности сервисов...${NC}"
sleep 10

echo -n "Проверка Prometheus... "
if curl -s http://localhost:9090/-/healthy > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Работает${NC}"
else
    echo -e "${RED}✗ Не отвечает${NC}"
fi

echo -n "Проверка Grafana... "
if curl -s http://localhost:3000/api/health > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Работает${NC}"
else
    echo -e "${RED}✗ Не отвечает${NC}"
fi

echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}Система мониторинга готова!${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "${YELLOW}Доступ к сервисам:${NC}"
echo -e "  Prometheus: ${BLUE}http://localhost:9090${NC}"
echo -e "  Grafana:    ${BLUE}http://localhost:3000${NC}"
echo ""
echo -e "${YELLOW}Учетные данные Grafana:${NC}"
echo -e "  Username: admin"
echo -e "  Password: admin"
echo ""
echo -e "${YELLOW}Остановка:${NC}"
echo -e "  docker-compose -f docker-compose.monitoring.yml down"
echo ""

