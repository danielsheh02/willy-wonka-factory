#!/bin/bash

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'


API_URL="${API_URL:-http://localhost:7999}"
CONCURRENT_USERS=50
REQUESTS_PER_USER=100
TOTAL_REQUESTS=$((CONCURRENT_USERS * REQUESTS_PER_USER))

docker-compose down
sudo rm -rf ../pg-data/
docker-compose up -d
sleep 10

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Performance Profiling${NC}"
echo -e "${BLUE}Тестирование производительности${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "${YELLOW}Параметры теста:${NC}"
echo -e "  Одновременных пользователей: ${CONCURRENT_USERS}"
echo -e "  Запросов на пользователя: ${REQUESTS_PER_USER}"
echo -e "  Общее количество запросов: ${TOTAL_REQUESTS}"
echo -e "  Порог времени отклика: 500мс"
echo ""

echo -e "${BLUE}Загрузка токенов...${NC}"
if [ ! -f "tokens/admin_token.txt" ]; then
    echo -e "${YELLOW}Токены не найдены. Запускаем get-token.sh...${NC}"
    ./get-token.sh
fi

ADMIN_TOKEN=$(cat tokens/admin_token.txt 2>/dev/null)
FOREMAN_TOKEN=$(cat tokens/foreman_token.txt 2>/dev/null)
WORKER_TOKEN=$(cat tokens/worker_token.txt 2>/dev/null)
MASTER_TOKEN=$(cat tokens/master_token.txt 2>/dev/null)

if [ -z "$ADMIN_TOKEN" ]; then
    echo -e "${RED}Не удалось загрузить токены!${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Токены загружены${NC}"
echo ""

RESULTS_DIR="results/performance-profiling-$(date +%Y%m%d-%H%M%S)"
mkdir -p $RESULTS_DIR

echo -e "${BLUE}Результаты будут сохранены в: ${RESULTS_DIR}${NC}"
echo ""

#############################################################
# ЭТАП 1: POST операции - Создание данных
#############################################################
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}ЭТАП 1: Создание данных (POST)${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

POST_REQUESTS=500
POST_CONCURRENT=25

#############################################################
# Тест 1: POST - Создание задач
#############################################################
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Тест 1: POST /api/tasks${NC}"
echo -e "${BLUE}Создание ${POST_REQUESTS} задач${NC}"
echo -e "${BLUE}========================================${NC}"

hey -n $POST_REQUESTS -c $POST_CONCURRENT \
    -m POST \
    -H "Authorization: Bearer $FOREMAN_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"name":"Performance Test Task","description":"Test task for performance profiling","status":"NOT_ASSIGNED"}' \
    "${API_URL}/api/tasks" \
    > "${RESULTS_DIR}/test1-post-tasks.txt"

echo -e "${GREEN}✓ Тест завершен${NC}"
echo -e "${YELLOW}Краткие результаты:${NC}"
grep -E "Total:|Requests/sec:|Average:|Slowest:|Status code" "${RESULTS_DIR}/test1-post-tasks.txt"
echo ""

#############################################################
# Тест 2: POST - Создание пользователей
#############################################################
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Тест 2: POST /api/users${NC}"
echo -e "${BLUE}Создание пользователей${NC}"
echo -e "${BLUE}========================================${NC}"

POST_USERS=100
POST_USERS_CONCURRENT=10

for i in $(seq 1 $POST_USERS); do
    curl -s -X POST "${API_URL}/api/users" \
        -H "Authorization: Bearer $ADMIN_TOKEN" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"perftest_user_$i\",\"password\":\"password123\",\"role\":\"WORKER\"}" \
        > /dev/null 2>&1
done

echo -e "${GREEN}✓ Создано ${POST_USERS} пользователей${NC}"
echo ""

#############################################################
# Тест 3: POST - Создание оборудования
#############################################################
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Тест 3: POST /api/equipments${NC}"
echo -e "${BLUE}Создание оборудования${NC}"
echo -e "${BLUE}========================================${NC}"

WORKSHOP_ID=$(curl -s -H "Authorization: Bearer $FOREMAN_TOKEN" "${API_URL}/api/workshops" | grep -o '"id":[0-9]*' | grep -o '[0-9]*' | head -1)

if [ -z "$WORKSHOP_ID" ]; then
    echo -e "${RED}Не удалось получить ID цеха!${NC}"
    WORKSHOP_ID=1
fi

echo -e "${YELLOW}Используем Workshop ID: ${WORKSHOP_ID}${NC}"

POST_EQUIPMENT=300
POST_EQUIPMENT_CONCURRENT=20

hey -n $POST_EQUIPMENT -c $POST_EQUIPMENT_CONCURRENT \
    -m POST \
    -H "Authorization: Bearer $FOREMAN_TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"name\":\"Performance Test Equipment\",\"description\":\"Test equipment for profiling\",\"model\":\"Model-PT-2026\",\"status\":\"WORKING\",\"health\":100,\"temperature\":25,\"workshopId\":${WORKSHOP_ID}}" \
    "${API_URL}/api/equipments" \
    > "${RESULTS_DIR}/test3-post-equipments.txt"

echo -e "${GREEN}✓ Тест завершен${NC}"
echo -e "${YELLOW}Краткие результаты:${NC}"
grep -E "Total:|Requests/sec:|Average:|Slowest:|Status code" "${RESULTS_DIR}/test3-post-equipments.txt"
echo ""

#############################################################
# ЭТАП 2: GET операции - Чтение созданных данных
#############################################################
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}ЭТАП 2: Чтение данных (GET)${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

get_count() {
    local endpoint=$1
    local token=$2
    local count=$(curl -s -H "Authorization: Bearer $token" "${API_URL}${endpoint}/count" | grep -o '"count":[0-9]*' | grep -o '[0-9]*')
    echo $count
}

#############################################################
# Тест 4: GET - Просмотр списка задач
#############################################################
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Тест 4: GET /api/tasks${NC}"
echo -e "${BLUE}Просмотр списка задач${NC}"
echo -e "${BLUE}========================================${NC}"

TASKS_COUNT=$(get_count "/api/tasks" "$WORKER_TOKEN")
echo -e "${YELLOW}Всего задач в системе: ${TASKS_COUNT}${NC}"

hey -n $TOTAL_REQUESTS -c $CONCURRENT_USERS \
    -H "Authorization: Bearer $WORKER_TOKEN" \
    "${API_URL}/api/tasks" \
    > "${RESULTS_DIR}/test4-get-tasks.txt"

echo -e "${GREEN}✓ Тест завершен${NC}"
echo -e "${YELLOW}Анализ результатов:${NC}"
grep -E "Total:|Requests/sec:|Average:|Slowest:|Fastest:|Status code" "${RESULTS_DIR}/test4-get-tasks.txt"
echo ""

#############################################################
# Тест 5: GET - Просмотр списка пользователей
#############################################################
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Тест 5: GET /api/users${NC}"
echo -e "${BLUE}Просмотр списка пользователей${NC}"
echo -e "${BLUE}========================================${NC}"

USERS_COUNT=$(get_count "/api/users" "$ADMIN_TOKEN")
echo -e "${YELLOW}Всего пользователей в системе: ${USERS_COUNT}${NC}"

hey -n $TOTAL_REQUESTS -c $CONCURRENT_USERS \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    "${API_URL}/api/users" \
    > "${RESULTS_DIR}/test5-get-users.txt"

echo -e "${GREEN}✓ Тест завершен${NC}"
echo -e "${YELLOW}Анализ результатов:${NC}"
grep -E "Total:|Requests/sec:|Average:|Slowest:|Fastest:|Status code" "${RESULTS_DIR}/test5-get-users.txt"
echo ""

#############################################################
# Тест 6: GET - Просмотр списка оборудования
#############################################################
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Тест 6: GET /api/equipments${NC}"
echo -e "${BLUE}Просмотр списка оборудования${NC}"
echo -e "${BLUE}========================================${NC}"

EQUIPMENT_COUNT=$(get_count "/api/equipments" "$FOREMAN_TOKEN")
echo -e "${YELLOW}Всего оборудования в системе: ${EQUIPMENT_COUNT}${NC}"

hey -n $TOTAL_REQUESTS -c $CONCURRENT_USERS \
    -H "Authorization: Bearer $FOREMAN_TOKEN" \
    "${API_URL}/api/equipments" \
    > "${RESULTS_DIR}/test6-get-equipments.txt"

echo -e "${GREEN}✓ Тест завершен${NC}"
echo -e "${YELLOW}Анализ результатов:${NC}"
grep -E "Total:|Requests/sec:|Average:|Slowest:|Fastest:|Status code" "${RESULTS_DIR}/test6-get-equipments.txt"
echo ""

#############################################################
# Анализ и выводы
#############################################################
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Итоговый анализ${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

analyze_results() {
    local file=$1
    local test_name=$2
    
    if [ ! -f "$file" ]; then
        echo -e "${RED}${test_name}: Файл не найден${NC}"
        return
    fi
    
    echo -e "${YELLOW}${test_name}:${NC}"
    
    avg_time=$(grep "Average:" $file | awk '{print $2}' | sed 's/secs//')
    slowest_time=$(grep "Slowest:" $file | awk '{print $2}' | sed 's/secs//')
    rps=$(grep "Requests/sec:" $file | awk '{print $2}')
    
    if [ -z "$avg_time" ]; then
        echo -e "  ${RED}✗ Не удалось извлечь данные${NC}"
        echo ""
        return
    fi
    
    avg_time_ms=$(echo "$avg_time * 1000" | bc)
    slowest_time_ms=$(echo "$slowest_time * 1000" | bc)
    
    echo -e "  Среднее время отклика: ${avg_time_ms} мс"
    echo -e "  Максимальное время отклика: ${slowest_time_ms} мс"
    echo -e "  Запросов в секунду: ${rps}"
    
    if (( $(echo "$avg_time_ms < 500" | bc -l) )); then
        echo -e "  ${GREEN}✓ Среднее время отклика в норме (<500мс)${NC}"
    else
        echo -e "  ${RED}✗ Среднее время отклика превышает норму (>500мс)${NC}"
    fi
    
    if (( $(echo "$slowest_time_ms < 1000" | bc -l) )); then
        echo -e "  ${GREEN}✓ Максимальное время отклика приемлемо (<1000мс)${NC}"
    else
        echo -e "  ${YELLOW}⚠ Максимальное время отклика высокое (>1000мс)${NC}"
    fi
    echo ""
}

echo -e "${BLUE}=== POST операции ===${NC}"
analyze_results "${RESULTS_DIR}/test1-post-tasks.txt" "POST /api/tasks"
analyze_results "${RESULTS_DIR}/test3-post-equipments.txt" "POST /api/equipments"

echo -e "${BLUE}=== GET операции ===${NC}"
analyze_results "${RESULTS_DIR}/test4-get-tasks.txt" "GET /api/tasks (${TASKS_COUNT} записей)"
analyze_results "${RESULTS_DIR}/test5-get-users.txt" "GET /api/users (${USERS_COUNT} записей)"
analyze_results "${RESULTS_DIR}/test6-get-equipments.txt" "GET /api/equipments (${EQUIPMENT_COUNT} записей)"

echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}Performance Profiling завершен!${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "${YELLOW}Данные созданы в тесте:${NC}"
echo -e "  - Задачи: ${POST_REQUESTS}"
echo -e "  - Пользователи: ${POST_USERS}"
echo -e "  - Оборудование: ${POST_EQUIPMENT}"
echo ""
