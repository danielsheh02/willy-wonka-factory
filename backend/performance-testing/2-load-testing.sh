#!/bin/bash


GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

API_URL="${API_URL:-http://localhost:7999}"

docker-compose down
sudo rm -rf ../pg-data/
docker-compose up -d
sleep 10

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Load Testing${NC}"
echo -e "${BLUE}Нагрузочное тестирование${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "${YELLOW}Параметры теста:${NC}"
echo -e "  Нагрузка: Постепенное увеличение от 100 до 200 пользователей"
echo -e "  Длительность: Несколько этапов по 10 секунд"
echo -e "  Цель: Проверка стабильности при пиковой нагрузке"
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

RESULTS_DIR="results/load-testing-$(date +%Y%m%d-%H%M%S)"
mkdir -p $RESULTS_DIR

echo -e "${BLUE}Результаты будут сохранены в: ${RESULTS_DIR}${NC}"
echo ""

#############################################################
# ЭТАП ПОДГОТОВКИ: Создание данных через POST
#############################################################
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}ЭТАП ПОДГОТОВКИ: Создание данных${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

#############################################################
# Создание задач
#############################################################
echo -e "${YELLOW}Создание задач...${NC}"

POST_TASKS=1000
POST_CONCURRENT=50

hey -n $POST_TASKS -c $POST_CONCURRENT \
    -m POST \
    -H "Authorization: Bearer $FOREMAN_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"name":"Load Test Task","description":"Task for load testing","status":"NOT_ASSIGNED"}' \
    "${API_URL}/api/tasks" \
    > "${RESULTS_DIR}/prep-post-tasks.txt"

echo -e "${GREEN}✓ Создано задач: ${POST_TASKS}${NC}"
echo ""

#############################################################
# Создание пользователей
#############################################################
echo -e "${YELLOW}Создание пользователей...${NC}"

POST_USERS=200

for i in $(seq 1 $POST_USERS); do
    curl -s -X POST "${API_URL}/api/users" \
        -H "Authorization: Bearer $ADMIN_TOKEN" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"loadtest_user_$i\",\"password\":\"password123\",\"role\":\"WORKER\"}" \
        > /dev/null 2>&1
done

echo -e "${GREEN}✓ Создано пользователей: ${POST_USERS}${NC}"
echo ""

#############################################################
# Создание оборудования
#############################################################
echo -e "${YELLOW}Создание оборудования...${NC}"

WORKSHOP_ID=$(curl -s -H "Authorization: Bearer $FOREMAN_TOKEN" "${API_URL}/api/workshops" | grep -o '"id":[0-9]*' | grep -o '[0-9]*' | head -1)

if [ -z "$WORKSHOP_ID" ]; then
    WORKSHOP_ID=1
fi

POST_EQUIPMENT=500

hey -n $POST_EQUIPMENT -c $POST_CONCURRENT \
    -m POST \
    -H "Authorization: Bearer $FOREMAN_TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"name\":\"Load Test Equipment\",\"description\":\"Equipment for load testing\",\"model\":\"Model-LT-2026\",\"status\":\"WORKING\",\"health\":100,\"temperature\":23,\"workshopId\":${WORKSHOP_ID}}" \
    "${API_URL}/api/equipments" \
    > "${RESULTS_DIR}/prep-post-equipments.txt"

echo -e "${GREEN}✓ Создано оборудования: ${POST_EQUIPMENT}${NC}"
echo ""

get_count() {
    local endpoint=$1
    local token=$2
    local count=$(curl -s -H "Authorization: Bearer $token" "${API_URL}${endpoint}/count" | grep -o '"count":[0-9]*' | grep -o '[0-9]*')
    echo $count
}

TASKS_COUNT=$(get_count "/api/tasks" "$WORKER_TOKEN")
USERS_COUNT=$(get_count "/api/users" "$ADMIN_TOKEN")
EQUIPMENT_COUNT=$(get_count "/api/equipments" "$FOREMAN_TOKEN")

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Подготовка завершена!${NC}"
echo -e "${GREEN}========================================${NC}"
echo -e "  Задачи: ${TASKS_COUNT}"
echo -e "  Пользователи: ${USERS_COUNT}"
echo -e "  Оборудование: ${EQUIPMENT_COUNT}"
echo ""
echo -e "${YELLOW}Начинаем нагрузочное тестирование...${NC}"
echo ""
sleep 5

run_load_test() {
    local users=$1
    local duration=$2
    local endpoint=$3
    local method=$4
    local token=$5
    local data=$6
    local test_name=$7
    
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}${test_name}${NC}"
    echo -e "${BLUE}Пользователей: ${users} | Длительность: ${duration}с${NC}"
    echo -e "${BLUE}========================================${NC}"
    
    if [ "$method" = "GET" ]; then
        hey -z ${duration}s -c $users \
            -H "Authorization: Bearer $token" \
            "${API_URL}${endpoint}" \
            > "${RESULTS_DIR}/${test_name// /_}.txt"
    else
        hey -z ${duration}s -c $users \
            -m $method \
            -H "Authorization: Bearer $token" \
            -H "Content-Type: application/json" \
            -d "$data" \
            "${API_URL}${endpoint}" \
            > "${RESULTS_DIR}/${test_name// /_}.txt"
    fi
    
    echo -e "${GREEN}✓ Тест завершен${NC}"
    echo -e "${YELLOW}Краткие результаты:${NC}"
    grep -E "Requests/sec:|Average:|Slowest:|Status code" "${RESULTS_DIR}/${test_name// /_}.txt" | head -5
    echo ""
    
    sleep 5
}

#############################################################
# Этап 1: 100 пользователей - Базовая нагрузка
#############################################################
echo -e "${YELLOW}=== ЭТАП 1: 100 пользователей ===${NC}"
echo ""

run_load_test 100 10 "/api/tasks" "GET" "$WORKER_TOKEN" "" "Этап 1.1 - GET tasks (100 users)"
run_load_test 100 10 "/api/users" "GET" "$ADMIN_TOKEN" "" "Этап 1.2 - GET users (100 users)"
run_load_test 100 10 "/api/equipments" "GET" "$FOREMAN_TOKEN" "" "Этап 1.3 - GET equipments (100 users)"

#############################################################
# Этап 2: 150 пользователей - Увеличенная нагрузка
#############################################################
echo -e "${YELLOW}=== ЭТАП 2: 150 пользователей ===${NC}"
echo ""

run_load_test 150 10 "/api/tasks" "GET" "$WORKER_TOKEN" "" "Этап 2.1 - GET tasks (150 users)"
run_load_test 150 10 "/api/users" "GET" "$ADMIN_TOKEN" "" "Этап 2.2 - GET users (150 users)"
run_load_test 150 10 "/api/equipments" "GET" "$FOREMAN_TOKEN" "" "Этап 2.3 - GET equipments (150 users)"

#############################################################
# Этап 3: 200 пользователей - Пиковая нагрузка
#############################################################
echo -e "${YELLOW}=== ЭТАП 3: 200 пользователей (ПИКОВАЯ НАГРУЗКА) ===${NC}"
echo ""

run_load_test 200 10 "/api/tasks" "GET" "$WORKER_TOKEN" "" "Этап 3.1 - GET tasks (200 users)"
run_load_test 200 10 "/api/users" "GET" "$ADMIN_TOKEN" "" "Этап 3.2 - GET users (200 users)"
run_load_test 200 10 "/api/equipments" "GET" "$FOREMAN_TOKEN" "" "Этап 3.3 - GET equipments (200 users)"

#############################################################
# Этап 4: Смешанная нагрузка с POST запросами
#############################################################
echo -e "${YELLOW}=== ЭТАП 4: Смешанная нагрузка (POST) ===${NC}"
echo ""

POST_DATA_TASK='{"name":"Load Test Task Concurrent","description":"Created during concurrent testing","status":"NOT_ASSIGNED"}'
POST_DATA_EQUIPMENT="{\"name\":\"Load Test Equip Concurrent\",\"description\":\"Created during concurrent testing\",\"model\":\"Model-C-2026\",\"status\":\"WORKING\",\"health\":95,\"temperature\":24,\"workshopId\":${WORKSHOP_ID}}"

run_load_test 100 10 "/api/tasks" "POST" "$FOREMAN_TOKEN" "$POST_DATA_TASK" "Этап 4.1 - POST tasks (100 users)"
run_load_test 50 10 "/api/equipments" "POST" "$FOREMAN_TOKEN" "$POST_DATA_EQUIPMENT" "Этап 4.2 - POST equipments (50 users)"

#############################################################
# Анализ и выводы
#############################################################
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Итоговый анализ Load Testing${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

analyze_load_results() {
    local file=$1
    local test_name=$2
    
    if [ ! -f "$file" ]; then
        echo -e "${RED}${test_name}: Файл не найден${NC}"
        return
    fi
    
    echo -e "${YELLOW}${test_name}:${NC}"
    
    total_requests=$(grep "Total:" $file | head -1 | awk '{print $2}')
    avg_time=$(grep "Average:" $file | awk '{print $2}' | sed 's/secs//')
    slowest_time=$(grep "Slowest:" $file | awk '{print $2}' | sed 's/secs//')
    fastest_time=$(grep "Fastest:" $file | awk '{print $2}' | sed 's/secs//')
    rps=$(grep "Requests/sec:" $file | awk '{print $2}')
    
    if [ -z "$avg_time" ]; then
        echo -e "  ${RED}✗ Не удалось извлечь данные${NC}"
        echo ""
        return
    fi
    
    avg_time_ms=$(echo "$avg_time * 1000" | bc 2>/dev/null || echo "N/A")
    slowest_time_ms=$(echo "$slowest_time * 1000" | bc 2>/dev/null || echo "N/A")
    fastest_time_ms=$(echo "$fastest_time * 1000" | bc 2>/dev/null || echo "N/A")
    
    echo -e "  Всего запросов: ${total_requests}"
    echo -e "  RPS: ${rps}"
    echo -e "  Среднее: ${avg_time_ms} мс"
    echo -e "  Fastest: ${fastest_time_ms} мс"
    echo -e "  Slowest: ${slowest_time_ms} мс"
    
    status_200=$(grep -A 10 "Status code distribution:" $file | grep "200" | awk '{print $3}' || echo "0")
    status_errors=$(grep -A 10 "Status code distribution:" $file | grep -E "\[(4|5)[0-9]{2}\]" | wc -l)
    
    if [ "$status_errors" -gt 0 ]; then
        echo -e "  ${RED}⚠ Обнаружены ошибки HTTP!${NC}"
        grep -A 10 "Status code distribution:" $file | grep -E "\[(4|5)[0-9]{2}\]"
    else
        echo -e "  ${GREEN}✓ Все запросы успешны${NC}"
    fi
    
    if [ "$avg_time_ms" != "N/A" ]; then
        if (( $(echo "$avg_time_ms < 1000" | bc -l) )); then
            echo -e "  ${GREEN}✓ Производительность хорошая${NC}"
        elif (( $(echo "$avg_time_ms < 2000" | bc -l) )); then
            echo -e "  ${YELLOW}⚠ Производительность приемлемая${NC}"
        else
            echo -e "  ${RED}✗ Производительность низкая${NC}"
        fi
    fi
    
    echo ""
}

echo -e "${BLUE}=== Анализ Этапа 1 (100 users) ===${NC}"
analyze_load_results "${RESULTS_DIR}/Этап_1.1_-_GET_tasks_(100_users).txt" "GET /api/tasks"
analyze_load_results "${RESULTS_DIR}/Этап_1.2_-_GET_users_(100_users).txt" "GET /api/users"
analyze_load_results "${RESULTS_DIR}/Этап_1.3_-_GET_equipments_(100_users).txt" "GET /api/equipments"

echo -e "${BLUE}=== Анализ Этапа 2 (150 users) ===${NC}"
analyze_load_results "${RESULTS_DIR}/Этап_2.1_-_GET_tasks_(150_users).txt" "GET /api/tasks"
analyze_load_results "${RESULTS_DIR}/Этап_2.2_-_GET_users_(150_users).txt" "GET /api/users"
analyze_load_results "${RESULTS_DIR}/Этап_2.3_-_GET_equipments_(150_users).txt" "GET /api/equipments"

echo -e "${BLUE}=== Анализ Этапа 3 (200 users - ПИКОВАЯ) ===${NC}"
analyze_load_results "${RESULTS_DIR}/Этап_3.1_-_GET_tasks_(200_users).txt" "GET /api/tasks"
analyze_load_results "${RESULTS_DIR}/Этап_3.2_-_GET_users_(200_users).txt" "GET /api/users"
analyze_load_results "${RESULTS_DIR}/Этап_3.3_-_GET_equipments_(200_users).txt" "GET /api/equipments"

echo -e "${BLUE}=== Анализ POST операций ===${NC}"
analyze_load_results "${RESULTS_DIR}/Этап_4.1_-_POST_tasks_(100_users).txt" "POST /api/tasks"
analyze_load_results "${RESULTS_DIR}/Этап_4.2_-_POST_equipments_(50_users).txt" "POST /api/equipments"


TASKS_COUNT=$(get_count "/api/tasks" "$WORKER_TOKEN")
USERS_COUNT=$(get_count "/api/users" "$ADMIN_TOKEN")
EQUIPMENT_COUNT=$(get_count "/api/equipments" "$FOREMAN_TOKEN")

echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}Load Testing завершен!${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "${YELLOW}Итого данных в системе:${NC}"
echo -e "  Задачи: ${TASKS_COUNT}"
echo -e "  Пользователи: ${USERS_COUNT}"
echo -e "  Оборудование: ${EQUIPMENT_COUNT}"
echo ""
