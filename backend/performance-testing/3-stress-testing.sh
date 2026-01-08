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

echo ""
echo -e "${YELLOW}Параметры теста:${NC}"
echo -e "  Нагрузка: Постепенное увеличение до экстремальных значений"
echo -e "  Этапы: 300, 500, 1000, 2000 пользователей"
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

RESULTS_DIR="results/stress-testing-$(date +%Y%m%d-%H%M%S)"
mkdir -p $RESULTS_DIR

echo -e "${BLUE}Результаты будут сохранены в: ${RESULTS_DIR}${NC}"
echo ""

#############################################################
# ЭТАП ПОДГОТОВКИ: Создание большого объёма данных
#############################################################
echo -e "${RED}========================================${NC}"
echo -e "${RED}ЭТАП ПОДГОТОВКИ: Создание данных${NC}"
echo -e "${RED}========================================${NC}"
echo ""

#############################################################
# Создание задач
#############################################################
echo -e "${YELLOW}Создание задач...${NC}"

POST_TASKS=5000
POST_CONCURRENT=100

hey -n $POST_TASKS -c $POST_CONCURRENT \
    -m POST \
    -H "Authorization: Bearer $FOREMAN_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"name":"Stress Test Task","description":"Task for stress testing","status":"NOT_ASSIGNED"}' \
    "${API_URL}/api/tasks" \
    > "${RESULTS_DIR}/prep-post-tasks.txt"

echo -e "${GREEN}✓ Создано задач: ${POST_TASKS}${NC}"
echo ""

#############################################################
# Создание пользователей
#############################################################
echo -e "${YELLOW}Создание пользователей...${NC}"

POST_USERS=500

for i in $(seq 1 $POST_USERS); do
    curl -s -X POST "${API_URL}/api/users" \
        -H "Authorization: Bearer $ADMIN_TOKEN" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"stresstest_user_$i\",\"password\":\"password123\",\"role\":\"WORKER\"}" \
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

POST_EQUIPMENT=3000

hey -n $POST_EQUIPMENT -c $POST_CONCURRENT \
    -m POST \
    -H "Authorization: Bearer $FOREMAN_TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"name\":\"Stress Test Equipment\",\"description\":\"Equipment for stress testing\",\"model\":\"Model-ST-2026\",\"status\":\"WORKING\",\"health\":100,\"temperature\":22,\"workshopId\":${WORKSHOP_ID}}" \
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

sleep 10

run_stress_test() {
    local users=$1
    local duration=$2
    local endpoint=$3
    local method=$4
    local token=$5
    local data=$6
    local test_name=$7
    
    echo -e "${RED}========================================${NC}"
    echo -e "${RED}${test_name}${NC}"
    echo -e "${RED}Пользователей: ${users} | Длительность: ${duration}с${NC}"
    echo -e "${RED}========================================${NC}"
    
    if [ "$method" = "GET" ]; then
        hey -z ${duration}s -c $users \
            -H "Authorization: Bearer $token" \
            "${API_URL}${endpoint}" \
            > "${RESULTS_DIR}/${test_name// /_}.txt" 2>&1
    else
        hey -z ${duration}s -c $users \
            -m $method \
            -H "Authorization: Bearer $token" \
            -H "Content-Type: application/json" \
            -d "$data" \
            "${API_URL}${endpoint}" \
            > "${RESULTS_DIR}/${test_name// /_}.txt" 2>&1
    fi
    
    local exit_code=$?
    
    if [ $exit_code -eq 0 ]; then
        echo -e "${GREEN}✓ Тест завершен${NC}"
        echo -e "${YELLOW}Краткие результаты:${NC}"
        grep -E "Requests/sec:|Average:|Slowest:|Status code|Error" "${RESULTS_DIR}/${test_name// /_}.txt" | head -10
    else
        echo -e "${RED}✗ Тест завершился с ошибкой (код: ${exit_code})${NC}"
        echo -e "${YELLOW}Возможно, система достигла предела${NC}"
    fi
    
    echo ""
    
    echo -e "${YELLOW}Пауза 10 секунд для восстановления системы...${NC}"
    sleep 10
}

#############################################################
# Этап 1: 300 пользователей
#############################################################
echo -e "${YELLOW}=== ЭТАП 1: 300 пользователей ===${NC}"
echo ""

run_stress_test 300 10 "/api/tasks" "GET" "$WORKER_TOKEN" "" "Stress 1.1 - GET tasks (300 users)"
run_stress_test 300 10 "/api/users" "GET" "$ADMIN_TOKEN" "" "Stress 1.2 - GET users (300 users)"
run_stress_test 300 10 "/api/equipments" "GET" "$FOREMAN_TOKEN" "" "Stress 1.3 - GET equipments (300 users)"

#############################################################
# Этап 2: 500 пользователей
#############################################################
echo -e "${YELLOW}=== ЭТАП 2: 500 пользователей ===${NC}"
echo ""

run_stress_test 500 10 "/api/tasks" "GET" "$WORKER_TOKEN" "" "Stress 2.1 - GET tasks (500 users)"
run_stress_test 500 10 "/api/users" "GET" "$ADMIN_TOKEN" "" "Stress 2.2 - GET users (500 users)"
run_stress_test 500 10 "/api/equipments" "GET" "$FOREMAN_TOKEN" "" "Stress 2.3 - GET equipments (500 users)"

#############################################################
# Этап 3: 1000 пользователей
#############################################################
echo -e "${YELLOW}=== ЭТАП 3: 1000 пользователей ===${NC}"
echo ""

run_stress_test 1000 10 "/api/tasks" "GET" "$WORKER_TOKEN" "" "Stress 3.1 - GET tasks (1000 users, 10s)"
run_stress_test 1000 10 "/api/users" "GET" "$ADMIN_TOKEN" "" "Stress 3.2 - GET users (1000 users, 10s)"
run_stress_test 1000 10 "/api/equipments" "GET" "$FOREMAN_TOKEN" "" "Stress 3.3 - GET equipments (1000 users, 10s)"

#############################################################
# Этап 4: 2000 пользователей
#############################################################
echo -e "${YELLOW}=== ЭТАП 3: 2000 пользователей ===${NC}"
echo ""

run_stress_test 2000 10 "/api/tasks" "GET" "$WORKER_TOKEN" "" "Stress 4.1 - GET tasks (2000 users, 10s)"
run_stress_test 2000 10 "/api/users" "GET" "$ADMIN_TOKEN" "" "Stress 4.2 - GET users (2000 users, 10s)"
run_stress_test 2000 10 "/api/equipments" "GET" "$FOREMAN_TOKEN" "" "Stress 4.3 - GET equipments (2000 users, 10s)"

#############################################################
# Этап 5: Стресс-тест POST операций
#############################################################
echo -e "${YELLOW}=== ЭТАП 5: Стресс-тест POST операций ===${NC}"
echo ""

POST_DATA_TASK='{"name":"Stress Task Extreme","description":"Created during extreme stress","status":"NOT_ASSIGNED"}'
POST_DATA_EQUIPMENT="{\"name\":\"Stress Equip Extreme\",\"description\":\"Created during extreme stress\",\"model\":\"Model-EX-2026\",\"status\":\"WORKING\",\"health\":90,\"temperature\":26,\"workshopId\":${WORKSHOP_ID}}"

run_stress_test 300 10 "/api/tasks" "POST" "$FOREMAN_TOKEN" "$POST_DATA_TASK" "Stress 5.1 - POST tasks (300 users)"
run_stress_test 300 10 "/api/equipments" "POST" "$FOREMAN_TOKEN" "$POST_DATA_EQUIPMENT" "Stress 5.2 - POST equipments (300 users)"

#############################################################
# Анализ и выводы
#############################################################
echo -e "${RED}========================================${NC}"
echo -e "${RED}Итоговый анализ Stress Testing${NC}"
echo -e "${RED}========================================${NC}"
echo ""

analyze_stress_results() {
    local file=$1
    local test_name=$2
    
    if [ ! -f "$file" ]; then
        echo -e "${RED}${test_name}: Файл результатов не найден${NC}"
        echo ""
        return
    fi
    
    echo -e "${YELLOW}${test_name}:${NC}"
    
    if grep -q "Error" "$file"; then
        echo -e "  ${RED}✗ Обнаружены ошибки выполнения${NC}"
        grep "Error" "$file" | head -5
    fi
    
    total_requests=$(grep "Total:" $file | head -1 | awk '{print $2}' || echo "N/A")
    avg_time=$(grep "Average:" $file | awk '{print $2}' | sed 's/secs//' || echo "N/A")
    slowest_time=$(grep "Slowest:" $file | awk '{print $2}' | sed 's/secs//' || echo "N/A")
    rps=$(grep "Requests/sec:" $file | awk '{print $2}' || echo "N/A")
    
    echo -e "  Всего запросов: ${total_requests}"
    echo -e "  RPS: ${rps}"
    
    if [ "$avg_time" != "N/A" ] && [ -n "$avg_time" ]; then
        avg_time_ms=$(echo "$avg_time * 1000" | bc 2>/dev/null || echo "N/A")
        slowest_time_ms=$(echo "$slowest_time * 1000" | bc 2>/dev/null || echo "N/A")
        
        echo -e "  Среднее время: ${avg_time_ms} мс"
        echo -e "  Макс время: ${slowest_time_ms} мс"
        
        if [ "$avg_time_ms" != "N/A" ]; then
            if (( $(echo "$avg_time_ms > 5000" | bc -l) )); then
                echo -e "  ${RED}✗ КРИТИЧЕСКАЯ деградация производительности!${NC}"
            elif (( $(echo "$avg_time_ms > 2000" | bc -l) )); then
                echo -e "  ${RED}⚠ СИЛЬНАЯ деградация производительности${NC}"
            elif (( $(echo "$avg_time_ms > 1000" | bc -l) )); then
                echo -e "  ${YELLOW}⚠ Заметная деградация производительности${NC}"
            else
                echo -e "  ${GREEN}✓ Система справляется с нагрузкой${NC}"
            fi
        fi
    fi
    
    status_errors=$(grep -A 10 "Status code distribution:" $file | grep -E "\[(4|5)[0-9]{2}\]" | wc -l || echo "0")
    
    if [ "$status_errors" -gt 0 ]; then
        echo -e "  ${RED}⚠ Обнаружены HTTP ошибки:${NC}"
        grep -A 10 "Status code distribution:" $file | grep -E "\[(4|5)[0-9]{2}\]" | head -5
    fi
    
    echo ""
}

echo -e "${BLUE}=== Анализ результатов ===${NC}"
echo ""

for result_file in ${RESULTS_DIR}/*.txt; do
    if [ -f "$result_file" ]; then
        filename=$(basename "$result_file" .txt)
        if [[ "$filename" == prep-* ]]; then
            continue
        fi
        analyze_stress_results "$result_file" "$filename"
    fi
done

TASKS_COUNT=$(get_count "/api/tasks" "$WORKER_TOKEN")
USERS_COUNT=$(get_count "/api/users" "$ADMIN_TOKEN")
EQUIPMENT_COUNT=$(get_count "/api/equipments" "$FOREMAN_TOKEN")

echo -e "${RED}========================================${NC}"
echo -e "${RED}Stress Testing завершен!${NC}"
echo -e "${RED}========================================${NC}"
echo ""
echo -e "${YELLOW}Итого данных в системе:${NC}"
echo -e "  Задачи: ${TASKS_COUNT}"
echo -e "  Пользователи: ${USERS_COUNT}"
echo -e "  Оборудование: ${EQUIPMENT_COUNT}"
echo ""

