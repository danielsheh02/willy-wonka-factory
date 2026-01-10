#!/bin/bash

#############################################################
# Скрипт для запуска конкретного бизнес-сценария
# Использование: ./run-specific-scenario.sh <номер>
#############################################################

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'


if [ -z "$1" ]; then
    echo -e "${RED}Укажите номер сценария (1-5)${NC}"
    echo "Использование: ./run-specific-scenario.sh <номер>"
    echo ""
    echo "Доступные сценарии:"
    echo "  1 - Управление задачами (Foreman & Worker)"
    echo "  2 - Создание экскурсий"
    echo "  3 - Обслуживание оборудования (Master)"
    echo "  4 - Генерация золотых билетов"
    echo "  5 - Бронирование по золотому билету"
    exit 1
fi

docker-compose down
sudo rm -rf ../pg-data/
docker-compose up -d
sleep 10

cd "$(dirname "$0")"


mkdir -p screenshots reports


if [ ! -d "venv" ]; then
    echo -e "${RED}✗ Виртуальное окружение не найдено!${NC}"
    echo "Запустите ./setup.sh для настройки окружения"
    exit 1
fi
source venv/bin/activate

case "$1" in
    1)
        TEST_FILE="test_scenario1_foreman_worker_tasks.py"
        SCENARIO_NAME="Управление задачами"
        ;;
    2)
        TEST_FILE="test_scenario2_excursion_creation.py"
        SCENARIO_NAME="Создание экскурсий"
        ;;
    3)
        TEST_FILE="test_scenario3_equipment_maintenance.py"
        SCENARIO_NAME="Обслуживание оборудования"
        ;;
    4)
        TEST_FILE="test_scenario4_golden_ticket_generation.py"
        SCENARIO_NAME="Генерация золотых билетов"
        ;;
    5)
        TEST_FILE="test_scenario5_golden_ticket_booking.py"
        SCENARIO_NAME="Бронирование по золотому билету"
        ;;
    *)
        echo -e "${RED}Неверный номер сценария. Используйте 1-5.${NC}"
        deactivate
        exit 1
        ;;
esac

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}🎭 Сценарий $1: ${SCENARIO_NAME}${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

BROWSER="${BROWSER:-chrome}"
HEADLESS="${HEADLESS:-false}"
echo -e "Браузер: ${BROWSER}"
echo -e "Headless: ${HEADLESS}"
echo -e "${YELLOW}ℹ Для переключения на Firefox: export BROWSER=firefox${NC}"
echo ""

pytest "$TEST_FILE" --html=reports/"$(basename "$TEST_FILE" .py)"-report.html --self-contained-html --capture=tee-sys -v

TEST_EXIT_CODE=$?

echo ""
echo -e "${BLUE}========================================${NC}"

if [ $TEST_EXIT_CODE -eq 0 ]; then
    echo -e "${GREEN}✓ Сценарий $1 прошел успешно!${NC}"
else
    echo -e "${YELLOW}✗ Сценарий $1 провалился!${NC}"
    echo ""
    echo "Скриншоты: screenshots/"
    echo "Отчет: reports/$(basename "$TEST_FILE" .py)-report.html"
fi

echo -e "${BLUE}========================================${NC}"
echo ""

deactivate

exit $TEST_EXIT_CODE
