#!/bin/bash

#############################################################
# Скрипт для запуска тестов базы данных
# Проверяет структуру БД, каскадные связи, ограничения
#############################################################

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}🗄️  Тестирование базы данных${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Проверка наличия Docker
if ! command -v docker &> /dev/null; then
    echo -e "${RED}✗ Docker не установлен!${NC}"
    exit 1
fi

# Проверка наличия docker-compose
if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}✗ docker-compose не установлен!${NC}"
    exit 1
fi

echo -e "${YELLOW}[1/4] Запуск тестовой БД...${NC}"
docker-compose -f docker-compose.test.yml up -d

echo ""
echo -e "${YELLOW}[2/4] Ожидание готовности БД...${NC}"
sleep 10

# Проверка доступности БД
echo -e "${YELLOW}[3/4] Проверка подключения к БД...${NC}"
if docker exec willy-wonka-test-db pg_isready -U test_user -d willy_wonka_test > /dev/null 2>&1; then
    echo -e "${GREEN}✓ База данных готова к работе!${NC}"
else
    echo -e "${RED}✗ База данных недоступна!${NC}"
    echo -e "${YELLOW}Попытка перезапуска...${NC}"
    docker-compose -f docker-compose.test.yml restart
    sleep 10
fi

echo ""
echo -e "${YELLOW}[4/4] Запуск тестов...${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Запуск тестов БД
./gradlew test --tests "com.example.demo.database.*" --info

TEST_EXIT_CODE=$?

echo ""
echo -e "${BLUE}========================================${NC}"

if [ $TEST_EXIT_CODE -eq 0 ]; then
    echo -e "${GREEN}✓ Все тесты БД прошли успешно!${NC}"
else
    echo -e "${RED}✗ Тесты завершились с ошибками!${NC}"
    echo ""
    echo -e "${YELLOW}Для просмотра детальной информации откройте:${NC}"
    echo -e "  build/reports/tests/test/index.html"
fi

echo -e "${BLUE}========================================${NC}"
echo ""

# Предлагаем остановить БД
read -p "Остановить тестовую БД? (y/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}Остановка тестовой БД...${NC}"
    docker-compose -f docker-compose.test.yml down
    echo -e "${GREEN}✓ БД остановлена${NC}"
else
    echo -e "${YELLOW}БД продолжает работать на порту 55432${NC}"
fi

exit $TEST_EXIT_CODE

