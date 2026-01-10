#!/bin/bash

#############################################################
# Скрипт для запуска бизнес-тестов (Selenium)
# Проверяет end-to-end сценарии пользователей
#############################################################

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}🎭 Тестирование бизнес-циклов${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Переход в директорию с тестами
cd "$(dirname "$0")"

# Создаем директории если не существуют
mkdir -p screenshots reports

# Активация виртуального окружения
echo -e "${YELLOW}[1/4] Активация виртуального окружения...${NC}"
if [ ! -d "venv" ]; then
    echo -e "${RED}✗ Виртуальное окружение не найдено!${NC}"
    echo "Запустите ./setup.sh для настройки окружения"
    exit 1
fi
source venv/bin/activate
echo -e "${GREEN}✓ Виртуальное окружение активировано${NC}"
echo ""

# Проверка доступности приложения
echo -e "${YELLOW}[2/4] Проверка доступности приложения...${NC}"

BASE_URL="${BASE_URL:-http://localhost:3000}"
API_URL="${API_URL:-http://localhost:7999}"

if curl -s "${API_URL}/actuator/health" > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Backend доступен: ${API_URL}${NC}"
else
    echo -e "${RED}✗ Backend недоступен на ${API_URL}!${NC}"
    echo -e "${YELLOW}Запустите backend: cd .. && ./gradlew bootRun${NC}"
    exit 1
fi

if curl -s "${BASE_URL}" > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Frontend доступен: ${BASE_URL}${NC}"
else
    echo -e "${RED}✗ Frontend недоступен на ${BASE_URL}!${NC}"
    echo -e "${YELLOW}Запустите frontend: cd ../../willy-wonka-admin-frontend && npm start${NC}"
    exit 1
fi
echo ""

# Информация о режиме запуска
echo -e "${YELLOW}[3/4] Настройки тестирования...${NC}"
BROWSER="${BROWSER:-chrome}"
HEADLESS="${HEADLESS:-false}"
echo -e "Браузер: ${BROWSER}"
echo -e "Headless: ${HEADLESS}"
echo -e "${YELLOW}ℹ Для переключения на Firefox: export BROWSER=firefox${NC}"
echo ""

# Запуск тестов
echo -e "${YELLOW}[4/4] Запуск тестов...${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

pytest --html=reports/business-tests-report.html --self-contained-html --capture=tee-sys -v

TEST_EXIT_CODE=$?

echo ""
echo -e "${BLUE}========================================${NC}"

if [ $TEST_EXIT_CODE -eq 0 ]; then
    echo -e "${GREEN}✓ Все тесты прошли успешно!${NC}"
else
    echo -e "${YELLOW}✗ Некоторые тесты провалились!${NC}"
    echo ""
    echo "Скриншоты ошибок: screenshots/"
    echo "Детальный отчет: reports/business-tests-report.html"
fi

echo -e "${BLUE}========================================${NC}"
echo ""

# Деактивация виртуального окружения
deactivate

exit $TEST_EXIT_CODE
