#!/bin/bash

#############################################################
# Проверка готовности системы к тестированию
# Willy Wonka Factory - Performance Testing
#############################################################

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Проверка готовности системы${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

ALL_OK=true

# 1. Проверка hey
echo -n "1. Утилита hey... "
if command -v hey &> /dev/null; then
    echo -e "${GREEN}✓ Установлена${NC}"
else
    echo -e "${RED}✗ Не установлена${NC}"
    echo -e "${YELLOW}   Установка:${NC}"
    echo -e "${YELLOW}   wget https://github.com/rakyll/hey/releases/download/v0.1.4/hey_linux_amd64${NC}"
    echo -e "${YELLOW}   chmod +x hey_linux_amd64${NC}"
    echo -e "${YELLOW}   sudo mv hey_linux_amd64 /usr/local/bin/hey${NC}"
    ALL_OK=false
fi

# 2. Проверка Docker
echo -n "2. Docker... "
if command -v docker &> /dev/null; then
    echo -e "${GREEN}✓ Установлен${NC}"
else
    echo -e "${RED}✗ Не установлен${NC}"
    ALL_OK=false
fi

# 3. Проверка Docker Compose
echo -n "3. Docker Compose... "
if command -v docker-compose &> /dev/null; then
    echo -e "${GREEN}✓ Установлен${NC}"
else
    echo -e "${RED}✗ Не установлен${NC}"
    ALL_OK=false
fi

# 4. Проверка приложения
echo -n "4. Spring Boot приложение (порт 7999)... "
if curl -s http://localhost:7999/actuator/health > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Запущено${NC}"
else
    echo -e "${RED}✗ Не запущено${NC}"
    echo -e "${YELLOW}   Запуск: cd backend && ./gradlew bootRun${NC}"
    ALL_OK=false
fi

# 5. Проверка Actuator endpoints
echo -n "5. Actuator endpoints... "
if curl -s http://localhost:7999/actuator/prometheus > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Доступны${NC}"
else
    echo -e "${YELLOW}⚠ Не доступны (проверьте конфигурацию)${NC}"
fi

# 6. Проверка Prometheus
echo -n "6. Prometheus (порт 9090)... "
if curl -s http://localhost:9090/-/healthy > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Запущен${NC}"
else
    echo -e "${YELLOW}⚠ Не запущен (опционально)${NC}"
    echo -e "${YELLOW}   Запуск: cd monitoring && ./start-monitoring.sh${NC}"
fi

# 7. Проверка Grafana
echo -n "7. Grafana (порт 3000)... "
if curl -s http://localhost:3000/api/health > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Запущен${NC}"
else
    echo -e "${YELLOW}⚠ Не запущен (опционально)${NC}"
    echo -e "${YELLOW}   Запуск: cd monitoring && ./start-monitoring.sh${NC}"
fi

# 8. Проверка токенов
echo -n "8. JWT токены... "
if [ -f "tokens/admin_token.txt" ] && [ -s "tokens/admin_token.txt" ]; then
    echo -e "${GREEN}✓ Готовы${NC}"
else
    echo -e "${YELLOW}⚠ Не найдены${NC}"
    echo -e "${YELLOW}   Получение: ./get-token.sh${NC}"
fi

# 9. Проверка директории результатов
echo -n "9. Директория для результатов... "
if [ -d "results" ]; then
    echo -e "${GREEN}✓ Существует${NC}"
else
    mkdir -p results
    echo -e "${GREEN}✓ Создана${NC}"
fi

# 10. Проверка свободного места
echo -n "10. Свободное место на диске... "
FREE_SPACE=$(df -h . | awk 'NR==2 {print $4}' | sed 's/G//')
if (( $(echo "$FREE_SPACE > 1" | bc -l) )); then
    echo -e "${GREEN}✓ ${FREE_SPACE}G доступно${NC}"
else
    echo -e "${YELLOW}⚠ Мало места (${FREE_SPACE}G)${NC}"
fi

echo ""
echo -e "${BLUE}========================================${NC}"
if [ "$ALL_OK" = true ]; then
    echo -e "${GREEN}Система готова к тестированию!${NC}"
    echo ""
    echo -e "${YELLOW}Следующие шаги:${NC}"
    echo -e "  1. Запустите мониторинг (если еще не запущен):"
    echo -e "     ${BLUE}cd ../monitoring && ./start-monitoring.sh${NC}"
    echo ""
    echo -e "  2. Получите токены (если еще не получены):"
    echo -e "     ${BLUE}./get-token.sh${NC}"
    echo ""
    echo -e "  3. Запустите тесты:"
    echo -e "     ${BLUE}./1-performance-profiling.sh${NC}  # Быстрый тест (~10 мин)"
    echo -e "     ${BLUE}./2-load-testing.sh${NC}           # Нагрузочный тест (~20 мин)"
    echo -e "     ${BLUE}./3-stress-testing.sh${NC}         # Стресс-тест (~30 мин)"
    echo ""
    echo -e "  или все вместе:"
    echo -e "     ${BLUE}./run-all-tests.sh${NC}            # Полный цикл (~60 мин)"
else
    echo -e "${RED}Система не готова!${NC}"
    echo -e "${YELLOW}Исправьте ошибки выше и повторите проверку.${NC}"
fi
echo -e "${BLUE}========================================${NC}"
echo ""

