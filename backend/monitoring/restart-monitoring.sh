#!/bin/bash

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

cd /home/daniel/MAGA/мпи/willy-wonka-factory/backend

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Перезапуск мониторинга${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

echo -e "${YELLOW}1. Остановка контейнеров...${NC}"
docker-compose -f docker-compose.monitoring.yml down

echo ""
echo -e "${YELLOW}2. Запуск контейнеров...${NC}"
docker-compose -f docker-compose.monitoring.yml up -d

echo ""
echo -e "${YELLOW}3. Ожидание запуска (10 секунд)...${NC}"
sleep 10

echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Проверка состояния${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

echo -e "${YELLOW}Проверка Spring Boot Actuator:${NC}"
if curl -s http://localhost:7999/actuator/prometheus | head -5 > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Actuator доступен${NC}"
else
    echo -e "${RED}✗ Actuator недоступен! Запустите приложение.${NC}"
fi

echo ""
echo -e "${YELLOW}Проверка Prometheus:${NC}"
if curl -s http://localhost:9090/-/healthy | grep -q "Prometheus"; then
    echo -e "${GREEN}✓ Prometheus запущен${NC}"
else
    echo -e "${RED}✗ Prometheus недоступен${NC}"
fi

echo ""
echo -e "${YELLOW}Проверка Grafana:${NC}"
if curl -s http://localhost:3000/api/health | grep -q "ok"; then
    echo -e "${GREEN}✓ Grafana запущен${NC}"
else
    echo -e "${RED}✗ Grafana недоступен${NC}"
fi

echo ""
echo -e "${YELLOW}Проверка targets в Prometheus (через 5 секунд):${NC}"
sleep 5

TARGETS=$(curl -s http://localhost:9090/api/v1/targets | grep -o '"health":"[^"]*"' | head -1)
if echo "$TARGETS" | grep -q '"health":"up"'; then
    echo -e "${GREEN}✓ Spring Boot приложение подключено к Prometheus${NC}"
else
    echo -e "${RED}✗ Spring Boot приложение не подключено к Prometheus${NC}"
    echo -e "${YELLOW}Проверьте targets: http://localhost:9090/targets${NC}"
fi

echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}Готово!${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "${YELLOW}URLs:${NC}"
echo -e "  Prometheus: ${BLUE}http://localhost:9090${NC}"
echo -e "  Grafana:    ${BLUE}http://localhost:3000${NC} (admin/admin)"
echo -e "  Targets:    ${BLUE}http://localhost:9090/targets${NC}"
echo -e "  Metrics:    ${BLUE}http://localhost:7999/actuator/prometheus${NC}"
echo ""

