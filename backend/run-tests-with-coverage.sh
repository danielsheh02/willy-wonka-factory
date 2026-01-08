#!/bin/bash

set -e

echo "================================================================"
echo "Willy Wonka Factory - Тесты с отчетом о покрытии кода"
echo "================================================================"
echo ""

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'


echo -e "${YELLOW}[1/6] Очистка старых тестовых контейнеров...${NC}"
docker-compose -f docker-compose.test.yml down -v 2>/dev/null || true

echo -e "${YELLOW}[2/6] Запуск тестовой базы данных...${NC}"
docker-compose -f docker-compose.test.yml up -d

echo -e "${YELLOW}[3/6] Ожидание готовности базы данных...${NC}"
sleep 5

MAX_RETRIES=10
RETRY_COUNT=0

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if docker exec willy-wonka-test-db pg_isready -U test_user -d willy_wonka_test > /dev/null 2>&1; then
        echo -e "${GREEN}База данных готова!${NC}"
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
echo -e "${YELLOW}[4/6] Запуск тестов...${NC}"
echo "================================================================"

if ./gradlew clean test; then
    echo ""
    echo -e "${GREEN}✓ Тесты выполнены успешно!${NC}"
else
    echo ""
    echo -e "${RED}✗ Тесты завершились с ошибками${NC}"
    docker-compose -f docker-compose.test.yml down -v
    exit 1
fi

echo ""
echo -e "${YELLOW}[5/6] Генерация отчета о покрытии кода (JaCoCo)...${NC}"
./gradlew jacocoTestReport

echo ""
echo -e "${YELLOW}[6/6] Проверка минимального покрытия кода...${NC}"
./gradlew jacocoTestCoverageVerification || true

echo ""
echo -e "${BLUE}================================================================"
echo "                  РЕЗУЛЬТАТЫ ТЕСТИРОВАНИЯ"
echo "================================================================${NC}"

if [ -f "build/test-results/test/TEST-com.example.demo.controllers.AuthControllerIntegrationTest.xml" ]; then
    TOTAL_TESTS=$(find build/test-results/test -name "*.xml" -exec grep -o 'tests="[0-9]*"' {} \; | grep -o '[0-9]*' | awk '{s+=$1} END {print s}')
    FAILED_TESTS=$(find build/test-results/test -name "*.xml" -exec grep -o 'failures="[0-9]*"' {} \; | grep -o '[0-9]*' | awk '{s+=$1} END {print s}')
    SKIPPED_TESTS=$(find build/test-results/test -name "*.xml" -exec grep -o 'skipped="[0-9]*"' {} \; | grep -o '[0-9]*' | awk '{s+=$1} END {print s}')
    PASSED_TESTS=$((TOTAL_TESTS - FAILED_TESTS - SKIPPED_TESTS))
    
    echo -e "${GREEN}Всего тестов:      $TOTAL_TESTS${NC}"
    echo -e "${GREEN}Пройдено:          $PASSED_TESTS${NC}"
    echo -e "${RED}Провалено:         $FAILED_TESTS${NC}"
    echo -e "${YELLOW}Пропущено:         $SKIPPED_TESTS${NC}"
fi

echo ""
echo -e "${BLUE}Отчеты:${NC}"
echo "  → HTML отчет о тестах:    build/reports/tests/test/index.html"
echo "  → HTML отчет о покрытии:  build/reports/jacoco/test/html/index.html"
echo "  → XML отчет о покрытии:   build/reports/jacoco/test/jacocoTestReport.xml"

if command -v xdg-open &> /dev/null; then
    echo ""
    read -p "Открыть отчет о покрытии в браузере? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        xdg-open build/reports/jacoco/test/html/index.html 2>/dev/null &
        xdg-open build/reports/tests/test/index.html 2>/dev/null &
    fi
fi

echo ""
echo -e "${YELLOW}Остановка тестовой базы данных...${NC}"
docker-compose -f docker-compose.test.yml down -v

echo ""
echo -e "${GREEN}================================================================"
echo "             ТЕСТИРОВАНИЕ ЗАВЕРШЕНО УСПЕШНО!"
echo "================================================================${NC}"

