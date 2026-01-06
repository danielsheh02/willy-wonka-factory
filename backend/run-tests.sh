#!/bin/bash

# Скрипт для запуска интеграционных тестов Willy Wonka Factory
# Использование: ./run-tests.sh

set -e

echo "=================================================="
echo "Willy Wonka Factory - Интеграционные Тесты"
echo "=================================================="
echo ""

# Цвета для вывода
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Проверка наличия Docker
if ! command -v docker &> /dev/null; then
    echo -e "${RED}Ошибка: Docker не установлен${NC}"
    exit 1
fi

# Проверка наличия Docker Compose
if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}Ошибка: Docker Compose не установлен${NC}"
    exit 1
fi

# Остановка и удаление старых контейнеров тестовой БД
echo -e "${YELLOW}[1/5] Очистка старых тестовых контейнеров...${NC}"
docker-compose -f docker-compose.test.yml down -v 2>/dev/null || true

# Запуск тестовой базы данных
echo -e "${YELLOW}[2/5] Запуск тестовой базы данных PostgreSQL...${NC}"
docker-compose -f docker-compose.test.yml up -d

# Ожидание готовности БД
echo -e "${YELLOW}[3/5] Ожидание готовности базы данных...${NC}"
echo "Ожидание 5 секунд для инициализации PostgreSQL..."
sleep 5

# Проверка доступности БД
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

# Запуск тестов
echo ""
echo -e "${YELLOW}[4/5] Запуск интеграционных тестов...${NC}"
echo "=================================================="

if ./gradlew clean test --info; then
    echo ""
    echo -e "${GREEN}=================================================="
    echo "✓ Все тесты выполнены успешно!"
    echo "==================================================${NC}"
    
    # Генерация отчета о покрытии
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
    
    # Очистка
    echo ""
    echo -e "${YELLOW}Остановка тестовой базы данных...${NC}"
    docker-compose -f docker-compose.test.yml down -v
    
    exit 1
fi

# Очистка
echo ""
echo -e "${YELLOW}Остановка тестовой базы данных...${NC}"
docker-compose -f docker-compose.test.yml down -v

echo ""
echo -e "${GREEN}=================================================="
echo "Тестирование завершено успешно!"
echo "==================================================${NC}"

