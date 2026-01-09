#!/bin/bash

set -e

echo "=================================================="
echo "Willy Wonka Factory - Интеграционные Тесты"
echo "=================================================="
echo ""

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${YELLOW}[1/5] Очистка старых тестовых контейнеров...${NC}"
docker-compose -f docker-compose.test.yml down -v 2>/dev/null || true

echo -e "${YELLOW}[2/5] Запуск тестовой базы данных PostgreSQL...${NC}"
docker-compose -f docker-compose.test.yml up -d

echo -e "${YELLOW}[3/5] Ожидание готовности базы данных...${NC}"
echo "Ожидание 5 секунд для инициализации PostgreSQL..."
sleep 5

echo "Проверка соединения с базой данных..."
MAX_RETRIES=10
RETRY_COUNT=0

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if docker exec willy-wonka-test-db pg_isready -U test_user -d willy_wonka_test > /dev/null 2>&1; then
        echo -e "${GREEN}База данных готова к работе!${NC}"
        break
    fi
    RETRY_COUNT=$((RETRY_COUNT + 1))
    echo "Попытка $RETRY_COUNT из $MAX_RETRIES..."
    sleep 2
done

if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
    echo -e "${RED}Ошибка: Не удалось подключиться к базе данных${NC}"
    docker-compose -f docker-compose.test.yml down -v
    exit 1
fi

echo ""
echo -e "${YELLOW}[4/5] Запуск интеграционных тестов...${NC}"
echo "=================================================="

if ./gradlew test --tests "com.example.demo.controllers.*" --tests "BaseIntegrationTest" --info; then
    echo ""
    echo -e "${GREEN}=================================================="
    echo "✓ Все тесты выполнены успешно!"
    echo "==================================================${NC}"
    
    echo ""
    echo -e "${YELLOW}[5/5] Генерация отчета о покрытии кода...${NC}"
    ./gradlew jacocoTestReport
    
    echo ""
    echo -e "${GREEN}✓ Отчет о покрытии кода сгенерирован${NC}"
    echo "Отчет доступен по пути: build/reports/jacoco/test/html/index.html"
    echo ""
    echo "Отчеты тестов доступны по пути: build/reports/tests/test/index.html"
else
    echo ""
    echo -e "${RED}=================================================="
    echo "✗ Тесты завершились с ошибками!"
    echo "==================================================${NC}"
    echo ""
    echo "Для просмотра детальной информации откройте:"
    echo "build/reports/tests/test/index.html"
    
    echo ""
    echo -e "${YELLOW}Остановка тестовой базы данных...${NC}"
    docker-compose -f docker-compose.test.yml down -v
    
    exit 1
fi

echo ""
echo -e "${YELLOW}Остановка тестовой базы данных...${NC}"
docker-compose -f docker-compose.test.yml down -v

echo ""
echo -e "${GREEN}=================================================="
echo "Тестирование завершено успешно!"
echo "==================================================${NC}"

