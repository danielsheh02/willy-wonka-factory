#!/bin/bash

# Скрипт для запуска конкретного тестового класса
# Использование: ./run-specific-test.sh <TestClassName>
# Пример: ./run-specific-test.sh AuthControllerIntegrationTest

set -e

# Проверка аргументов
if [ $# -eq 0 ]; then
    echo "Использование: $0 <TestClassName>"
    echo ""
    echo "Доступные тесты:"
    echo "  - AuthControllerIntegrationTest"
    echo "  - UserControllerIntegrationTest"
    echo "  - TaskControllerIntegrationTest"
    echo "  - WorkshopControllerIntegrationTest"
    echo "  - EquipmentControllerIntegrationTest"
    echo "  - ExcursionControllerIntegrationTest"
    echo "  - GoldenTicketControllerIntegrationTest"
    echo "  - NotificationControllerIntegrationTest"
    echo "  - ReportControllerIntegrationTest"
    echo ""
    echo "Пример: $0 AuthControllerIntegrationTest"
    exit 1
fi

TEST_CLASS=$1

# Цвета
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo "=================================================="
echo "Запуск теста: $TEST_CLASS"
echo "=================================================="
echo ""

# Запуск тестовой БД
echo -e "${YELLOW}Запуск тестовой базы данных...${NC}"
docker-compose -f docker-compose.test.yml up -d

echo "Ожидание готовности БД..."
sleep 15

# Запуск конкретного теста
echo ""
echo -e "${YELLOW}Запуск теста $TEST_CLASS...${NC}"
echo ""

if ./gradlew test --tests "*.$TEST_CLASS" --info; then
    echo ""
    echo -e "${GREEN}✓ Тест $TEST_CLASS выполнен успешно!${NC}"
else
    echo ""
    echo -e "${RED}✗ Тест $TEST_CLASS провален${NC}"
    docker-compose -f docker-compose.test.yml down -v
    exit 1
fi

# Очистка
echo ""
echo -e "${YELLOW}Остановка тестовой базы данных...${NC}"
docker-compose -f docker-compose.test.yml down -v

echo ""
echo -e "${GREEN}Готово!${NC}"

