#!/bin/bash

#############################################################
# Скрипт последовательного запуска всех тестов
# Willy Wonka Factory - Complete Performance Testing Suite
#############################################################

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Полный цикл тестирования производительности${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Проверка готовности системы
echo -e "${YELLOW}Проверка готовности системы...${NC}"
echo ""

# Проверка приложения
echo -n "1. Проверка Spring Boot приложения... "
if curl -s http://localhost:7999/actuator/health > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC}"
else
    echo -e "${RED}✗${NC}"
    echo -e "${RED}Приложение не запущено на порту 7999!${NC}"
    exit 1
fi

# Проверка Prometheus
echo -n "2. Проверка Prometheus... "
if curl -s http://localhost:9090/-/healthy > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC}"
else
    echo -e "${YELLOW}⚠ (опционально)${NC}"
fi

# Проверка Grafana
echo -n "3. Проверка Grafana... "
if curl -s http://localhost:3000/api/health > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC}"
else
    echo -e "${YELLOW}⚠ (опционально)${NC}"
fi

# Проверка hey
echo -n "4. Проверка утилиты hey... "
if command -v hey &> /dev/null; then
    echo -e "${GREEN}✓${NC}"
else
    echo -e "${RED}✗${NC}"
    echo -e "${RED}Утилита 'hey' не установлена!${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}Система готова к тестированию!${NC}"
echo ""

# Получение токенов
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Этап 0: Получение токенов${NC}"
echo -e "${BLUE}========================================${NC}"
./get-token.sh

if [ ! -f "tokens/admin_token.txt" ]; then
    echo -e "${RED}Не удалось получить токены!${NC}"
    exit 1
fi

# Создаем общую директорию для результатов
MAIN_RESULTS_DIR="results/full-test-suite-$(date +%Y%m%d-%H%M%S)"
mkdir -p $MAIN_RESULTS_DIR

echo ""
read -p "Начать тестирование? Это займет ~30-60 минут. (yes/no): " -r
if [[ ! $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
    echo "Тестирование отменено"
    exit 0
fi

# Тест 1: Performance Profiling
echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Этап 1: Performance Profiling${NC}"
echo -e "${BLUE}Ожидаемая длительность: ~10 минут${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

./1-performance-profiling.sh

echo ""
echo -e "${GREEN}Performance Profiling завершен${NC}"
echo -e "${YELLOW}Пауза 60 секунд для восстановления системы...${NC}"
sleep 60

# Тест 2: Load Testing
echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Этап 2: Load Testing${NC}"
echo -e "${BLUE}Ожидаемая длительность: ~20 минут${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

./2-load-testing.sh

echo ""
echo -e "${GREEN}Load Testing завершен${NC}"
echo -e "${YELLOW}Пауза 120 секунд для восстановления системы...${NC}"
sleep 120

# Тест 3: Stress Testing (опционально)
echo ""
echo -e "${RED}========================================${NC}"
echo -e "${RED}Этап 3: Stress Testing${NC}"
echo -e "${RED}⚠ ВНИМАНИЕ: Экстремальная нагрузка!${NC}"
echo -e "${RED}========================================${NC}"
echo ""

read -p "Выполнить Stress Testing? (yes/no): " -r
if [[ $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
    echo "yes" | ./3-stress-testing.sh
    echo ""
    echo -e "${GREEN}Stress Testing завершен${NC}"
else
    echo -e "${YELLOW}Stress Testing пропущен${NC}"
fi

# Итоговый отчет
echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Тестирование завершено!${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

echo -e "${YELLOW}Результаты сохранены в:${NC}"
ls -lh results/ | grep "$(date +%Y%m%d)"

echo ""
echo -e "${YELLOW}Следующие шаги:${NC}"
echo -e "  1. Проверьте dashboard в Grafana (http://localhost:3000)"
echo -e "  2. Проанализируйте файлы результатов в директории results/"
echo -e "  3. Проверьте логи приложения на наличие ошибок"
echo -e "  4. Составьте отчет на основе TESTING.md"
echo ""

echo -e "${GREEN}Тестирование успешно завершено!${NC}"

