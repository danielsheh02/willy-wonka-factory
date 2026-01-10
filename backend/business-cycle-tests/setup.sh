#!/bin/bash

#############################################################
# Скрипт для первичной настройки окружения бизнес-тестов
#############################################################

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}🔧 Настройка окружения для бизнес-тестов${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

cd "$(dirname "$0")"

echo -e "${YELLOW}[1/4] Проверка Python...${NC}"
if ! command -v python3 &> /dev/null; then
    echo -e "${RED}✗ Python 3 не установлен!${NC}"
    echo "Установите Python 3.8 или выше"
    exit 1
fi

PYTHON_VERSION=$(python3 --version | cut -d' ' -f2)
echo -e "${GREEN}✓ Python ${PYTHON_VERSION} найден${NC}"
echo ""

echo -e "${YELLOW}[2/4] Проверка pip...${NC}"
if ! command -v pip3 &> /dev/null; then
    echo -e "${RED}✗ pip3 не установлен!${NC}"
    exit 1
fi
echo -e "${GREEN}✓ pip3 найден${NC}"
echo ""

echo -e "${YELLOW}[3/5] Создание виртуального окружения...${NC}"
if [ ! -d "venv" ]; then
    python3 -m venv venv
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ Виртуальное окружение создано${NC}"
    else
        echo -e "${RED}✗ Ошибка создания виртуального окружения${NC}"
        exit 1
    fi
else
    echo -e "${GREEN}✓ Виртуальное окружение уже существует${NC}"
fi
echo ""

echo -e "${YELLOW}[4/5] Установка зависимостей Python...${NC}"
source venv/bin/activate
pip install --upgrade pip
pip install -r requirements.txt

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Зависимости установлены${NC}"
else
    echo -e "${RED}✗ Ошибка установки зависимостей${NC}"
    exit 1
fi
echo ""

echo -e "${YELLOW}[5/5] Проверка браузера...${NC}"
CHROME_FOUND=false
FIREFOX_FOUND=false

if command -v google-chrome &> /dev/null; then
    CHROME_VERSION=$(google-chrome --version)
    echo -e "${GREEN}✓ ${CHROME_VERSION}${NC}"
    CHROME_FOUND=true
elif command -v chromium-browser &> /dev/null; then
    CHROMIUM_VERSION=$(chromium-browser --version)
    echo -e "${GREEN}✓ ${CHROMIUM_VERSION}${NC}"
    CHROME_FOUND=true
fi

if command -v firefox &> /dev/null; then
    FIREFOX_VERSION=$(firefox --version)
    echo -e "${GREEN}✓ ${FIREFOX_VERSION}${NC}"
    FIREFOX_FOUND=true
fi

if [ "$CHROME_FOUND" = false ] && [ "$FIREFOX_FOUND" = false ]; then
    echo -e "${RED}✗ Браузер не найден!${NC}"
    echo "Установите Chrome, Chromium или Firefox:"
    echo "  sudo apt install google-chrome-stable"
    echo "  sudo apt install chromium-browser"
    echo "  sudo apt install firefox"
    exit 1
fi

if [ "$CHROME_FOUND" = true ]; then
    echo -e "${YELLOW}ℹ Chrome будет использован по умолчанию${NC}"
elif [ "$FIREFOX_FOUND" = true ]; then
    echo -e "${YELLOW}ℹ Firefox будет использован как запасной вариант${NC}"
    echo -e "${YELLOW}  Для переключения на Firefox: export BROWSER=firefox${NC}"
fi
echo ""

mkdir -p screenshots reports

echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}✓ Настройка завершена!${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "${YELLOW}Следующие шаги:${NC}"
echo "1. Запустите backend: cd .. && ./gradlew bootRun"
echo "2. Запустите frontend: cd ../../willy-wonka-admin-frontend && npm start"
echo "3. Запустите тесты: ./run-business-tests.sh"
echo ""

