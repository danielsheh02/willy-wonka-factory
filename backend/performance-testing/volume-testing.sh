#!/bin/bash

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

API_URL="${API_URL:-http://localhost:7999}"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Volume Testing${NC}"
echo -e "${BLUE}Объемное тестирование${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "${YELLOW}Параметры теста:${NC}"
echo -e "  Задания: 1000 (POST + GET)"
echo -e "  Оборудование: 1000 (POST + GET)"
echo -e "  Пользователи: 1000 (POST + GET)"
echo -e "  Цель: проверка работы с большими объемами данных"
echo ""

echo -e "${BLUE}Очистка базы данных...${NC}"
docker-compose down
sudo rm -rf ../pg-data/
docker-compose up -d
sleep 10
echo -e "${GREEN}✓ База данных очищена и перезапущена${NC}"
echo ""

echo -e "${BLUE}Загрузка токенов...${NC}"
if [ ! -f "tokens/admin_token.txt" ]; then
    echo -e "${YELLOW}Токены не найдены. Запускаем get-token.sh...${NC}"
    ./get-token.sh
fi

ADMIN_TOKEN=$(cat tokens/admin_token.txt 2>/dev/null)
FOREMAN_TOKEN=$(cat tokens/foreman_token.txt 2>/dev/null)
WORKER_TOKEN=$(cat tokens/worker_token.txt 2>/dev/null)

if [ -z "$ADMIN_TOKEN" ]; then
    echo -e "${RED}Не удалось загрузить токены!${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Токены загружены${NC}"
echo ""

RESULTS_DIR="results/volume-testing-$(date +%Y%m%d-%H%M%S)"
mkdir -p $RESULTS_DIR

echo -e "${BLUE}Результаты будут сохранены в: ${RESULTS_DIR}${NC}"
echo ""


echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}ЭТАП 1: Создание больших объемов данных${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""


echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE} Тест 1: POST /api/tasks${NC}"
echo -e "${BLUE} 1000 POST запросов для заданий${NC}"
echo -e "${BLUE}========================================${NC}"

TASKS_COUNT=1000
TASKS_CONCURRENT=1

echo -e "${YELLOW}Начало создания заданий: $(date)${NC}"
START_TIME=$(date +%s)

hey -n $TASKS_COUNT -c $TASKS_CONCURRENT \
    -m POST \
    -H "Authorization: Bearer $FOREMAN_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"name":"Volume Test Task","description":"Large volume test task","status":"NOT_ASSIGNED"}' \
    "${API_URL}/api/tasks" \
    > "${RESULTS_DIR}/test1-post-tasks.txt"

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

echo -e "${GREEN}✓ Создание завершено за ${DURATION} секунд${NC}"
echo -e "${YELLOW}Статистика создания:${NC}"
grep -E "Total:|Requests/sec:|Average:|Slowest:|Fastest:|Status code" "${RESULTS_DIR}/test1-post-tasks.txt"
echo ""

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Тест 2: GET /api/tasks${NC}"
echo -e "${BLUE} 1000 GET запросов для заданий${NC}"
echo -e "${BLUE}========================================${NC}"

GET_REQUESTS=1000
GET_CONCURRENT=1

echo -e "${YELLOW}Начало чтения заданий: $(date)${NC}"
START_TIME=$(date +%s)

hey -n $GET_REQUESTS -c $GET_CONCURRENT \
    -H "Authorization: Bearer $WORKER_TOKEN" \
    "${API_URL}/api/tasks" \
    > "${RESULTS_DIR}/test2-get-tasks-with-records.txt"

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

echo -e "${GREEN}✓ Чтение завершено за ${DURATION} секунд${NC}"
echo -e "${YELLOW}Статистика чтения:${NC}"
grep -E "Total:|Requests/sec:|Average:|Slowest:|Fastest:|Status code" "${RESULTS_DIR}/test2-get-tasks-with-records.txt"
echo ""

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Тест 3: POST /api/equipments${NC}"
echo -e "${BLUE} 1000 POST запросов для оборудования${NC}"
echo -e "${BLUE}========================================${NC}"


WORKSHOP_ID=$(curl -s -H "Authorization: Bearer $FOREMAN_TOKEN" "${API_URL}/api/workshops" | grep -o '"id":[0-9]*' | grep -o '[0-9]*' | head -1)

if [ -z "$WORKSHOP_ID" ]; then
    echo -e "${RED}Не удалось получить ID цеха!${NC}"
    WORKSHOP_ID=1
fi

echo -e "${YELLOW}Используем Workshop ID: ${WORKSHOP_ID}${NC}"

EQUIPMENT_COUNT=1000
EQUIPMENT_CONCURRENT=1

echo -e "${YELLOW}Начало создания оборудования: $(date)${NC}"
START_TIME=$(date +%s)

hey -n $EQUIPMENT_COUNT -c $EQUIPMENT_CONCURRENT \
    -m POST \
    -H "Authorization: Bearer $FOREMAN_TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"name\":\"Volume Test Equipment\",\"description\":\"Large volume test equipment\",\"model\":\"Model-VT-2026\",\"status\":\"WORKING\",\"health\":100,\"temperature\":25,\"workshopId\":${WORKSHOP_ID}}" \
    "${API_URL}/api/equipments" \
    > "${RESULTS_DIR}/test3-post-equipments.txt"

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

echo -e "${GREEN}✓ Создание завершено за ${DURATION} секунд${NC}"
echo -e "${YELLOW}Статистика создания:${NC}"
grep -E "Total:|Requests/sec:|Average:|Slowest:|Fastest:|Status code" "${RESULTS_DIR}/test3-post-equipments.txt"
echo ""

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Тест 4: GET /api/equipments${NC}"
echo -e "${BLUE} 1000 GET запросов для оборудования${NC}"
echo -e "${BLUE}========================================${NC}"

GET_EQUIPMENT_REQUESTS=1000
GET_EQUIPMENT_CONCURRENT=1

echo -e "${YELLOW}Начало чтения оборудования: $(date)${NC}"
START_TIME=$(date +%s)

hey -n $GET_EQUIPMENT_REQUESTS -c $GET_EQUIPMENT_CONCURRENT \
    -H "Authorization: Bearer $FOREMAN_TOKEN" \
    "${API_URL}/api/equipments" \
    > "${RESULTS_DIR}/test4-get-equipments-with-records.txt"

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

echo -e "${GREEN}✓ Чтение завершено за ${DURATION} секунд${NC}"
echo -e "${YELLOW}Статистика чтения:${NC}"
grep -E "Total:|Requests/sec:|Average:|Slowest:|Fastest:|Status code" "${RESULTS_DIR}/test4-get-equipments-with-records.txt"
echo ""

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Тест 5: POST /api/users${NC}"
echo -e "${BLUE} 1000 POST запросов для пользователей${NC}"
echo -e "${BLUE}========================================${NC}"

USERS_COUNT=1000
USERS_CONCURRENT=1

echo -e "${YELLOW}Начало создания пользователей: $(date)${NC}"
START_TIME=$(date +%s)

for i in $(seq 1 $USERS_COUNT); do
    curl -s -X POST "${API_URL}/api/users" \
        -H "Authorization: Bearer $ADMIN_TOKEN" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"perftest_user_$i\",\"password\":\"password123\",\"role\":\"WORKER\"}" \
        > "${RESULTS_DIR}/test5-post-users.txt"
done

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

echo -e "${GREEN}✓ Создание завершено за ${DURATION} секунд${NC}"
echo -e "${YELLOW}Статистика создания:${NC}"
cat "${RESULTS_DIR}/test5-post-users.txt" | head -20
echo ""

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Тест 6: GET /api/users${NC}"
echo -e "${BLUE} 1000 GET запросов для пользователей${NC}"
echo -e "${BLUE}========================================${NC}"

GET_USERS_REQUESTS=1000
GET_USERS_CONCURRENT=1

echo -e "${YELLOW}Начало чтения пользователей: $(date)${NC}"
START_TIME=$(date +%s)

hey -n $GET_USERS_REQUESTS -c $GET_USERS_CONCURRENT \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    "${API_URL}/api/users" \
    > "${RESULTS_DIR}/test6-get-users-with-records.txt"

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

echo -e "${GREEN}✓ Чтение завершено за ${DURATION} секунд${NC}"
echo -e "${YELLOW}Статистика чтения:${NC}"
grep -E "Total:|Requests/sec:|Average:|Slowest:|Fastest:|Status code" "${RESULTS_DIR}/test6-get-users-with-records.txt"
echo ""

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Итоговый анализ объемного тестирования${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

analyze_volume_results() {
    local file=$1
    local test_name=$2
    local records_count=$3
    
    if [ ! -f "$file" ]; then
        echo -e "${RED}${test_name}: Файл не найден${NC}"
        return
    fi
    
    echo -e "${YELLOW}${test_name} (${records_count} записей):${NC}"
    
    avg_time=$(grep "Average:" $file | awk '{print $2}' | sed 's/secs//')
    slowest_time=$(grep "Slowest:" $file | awk '{print $2}' | sed 's/secs//')
    fastest_time=$(grep "Fastest:" $file | awk '{print $2}' | sed 's/secs//')
    rps=$(grep "Requests/sec:" $file | awk '{print $2}')
    total_time=$(grep "Total:" $file | awk '{print $2}' | sed 's/secs//')
    
    if [ -z "$avg_time" ]; then
        grep "Создано" $file
        echo ""
        return
    fi
    
    avg_time_ms=$(echo "$avg_time * 1000" | bc)
    slowest_time_ms=$(echo "$slowest_time * 1000" | bc)
    fastest_time_ms=$(echo "$fastest_time * 1000" | bc)
    
    echo -e "  Общее время теста: ${total_time} сек"
    echo -e "  Среднее время отклика: ${avg_time_ms} мс"
    echo -e "  Минимальное время отклика: ${fastest_time_ms} мс"
    echo -e "  Максимальное время отклика: ${slowest_time_ms} мс"
    echo -e "  Запросов в секунду: ${rps}"
    
    if (( $(echo "$avg_time_ms < 1000" | bc -l) )); then
        echo -e "  ${GREEN}✓ Среднее время отклика приемлемо для больших объемов (<1000мс)${NC}"
    elif (( $(echo "$avg_time_ms < 2000" | bc -l) )); then
        echo -e "  ${YELLOW}⚠ Среднее время отклика высокое (1000-2000мс)${NC}"
    else
        echo -e "  ${RED}✗ Среднее время отклика критично высокое (>2000мс)${NC}"
    fi
    
    if (( $(echo "$slowest_time_ms < 5000" | bc -l) )); then
        echo -e "  ${GREEN}✓ Максимальное время отклика приемлемо (<5000мс)${NC}"
    else
        echo -e "  ${YELLOW}⚠ Максимальное время отклика очень высокое (>5000мс)${NC}"
    fi
    echo ""
}

echo -e "${BLUE}=== Создание данных (POST) ===${NC}"
analyze_volume_results "${RESULTS_DIR}/test1-post-tasks.txt" "POST /api/tasks"
analyze_volume_results "${RESULTS_DIR}/test3-post-equipments.txt" "POST /api/equipments"
analyze_volume_results "${RESULTS_DIR}/test5-post-users.txt" "POST /api/users"

echo -e "${BLUE}=== Чтение данных (GET) ===${NC}"
analyze_volume_results "${RESULTS_DIR}/test2-get-tasks-with-records.txt" "GET /api/tasks"
analyze_volume_results "${RESULTS_DIR}/test4-get-equipments-with-records.txt" "GET /api/equipments"
analyze_volume_results "${RESULTS_DIR}/test6-get-users-with-records.txt" "GET /api/users"

echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}Volume Testing завершен!${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

echo -e "${GREEN}✓ Объемное тестирование успешно завершено${NC}"

