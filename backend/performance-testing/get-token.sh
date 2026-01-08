#!/bin/bash

#############################################################
# Использует пользователей из DataInitializer:
# - admin1 (ADMIN)
# - foreman1 (FOREMAN)
# - worker1 (WORKER)
# - master1 (MASTER)
# - guide1 (GUIDE)
# Пароль для всех: password
#############################################################

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

API_URL="${API_URL:-http://localhost:7999}"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Получение JWT токенов${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

echo -n "Проверка доступности приложения... "
if curl -s "${API_URL}/actuator/health" > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC}"
else
    echo -e "${RED}✗${NC}"
    echo -e "${RED}Приложение не запущено на ${API_URL}!${NC}"
    echo -e "${YELLOW}Запустите приложение: cd backend && ./gradlew bootRun${NC}"
    exit 1
fi
echo ""

get_token() {
    local username=$1
    local password=$2
    local role=$3
    
    echo -e "${YELLOW}Получение токена для ${role} (пользователь: ${username})${NC}" >&2
    
    response=$(curl -s -X POST "${API_URL}/api/auth/signin" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"${username}\",\"password\":\"${password}\"}")
    
    token=$(echo $response | grep -o '"token":"[^"]*' | grep -o '[^"]*$')
    
    if [ -z "$token" ]; then
        echo -e "${RED}✗ Не удалось получить токен${NC}" >&2
        echo -e "${RED}Ответ сервера: ${response}${NC}" >&2
        echo "" >&2
        return 1
    fi
    
    echo -e "${GREEN}✓ Токен получен успешно${NC}" >&2
    echo "$token"
    return 0
}

mkdir -p tokens

echo -e "${BLUE}1. Получение токена ADMIN${NC}"
ADMIN_TOKEN=$(get_token "admin1" "password" "ADMIN")
if [ $? -eq 0 ]; then
    echo $ADMIN_TOKEN > tokens/admin_token.txt
    echo -e "${GREEN}Сохранен в: tokens/admin_token.txt${NC}"
fi
echo ""

echo -e "${BLUE}2. Получение токена FOREMAN${NC}"
FOREMAN_TOKEN=$(get_token "foreman1" "password" "FOREMAN")
if [ $? -eq 0 ]; then
    echo $FOREMAN_TOKEN > tokens/foreman_token.txt
    echo -e "${GREEN}Сохранен в: tokens/foreman_token.txt${NC}"
fi
echo ""

echo -e "${BLUE}3. Получение токена WORKER${NC}"
WORKER_TOKEN=$(get_token "worker1" "password" "WORKER")
if [ $? -eq 0 ]; then
    echo $WORKER_TOKEN > tokens/worker_token.txt
    echo -e "${GREEN}Сохранен в: tokens/worker_token.txt${NC}"
fi
echo ""

echo -e "${BLUE}4. Получение токена MASTER${NC}"
MASTER_TOKEN=$(get_token "master1" "password" "MASTER")
if [ $? -eq 0 ]; then
    echo $MASTER_TOKEN > tokens/master_token.txt
    echo -e "${GREEN}Сохранен в: tokens/master_token.txt${NC}"
fi
echo ""

echo -e "${BLUE}5. Получение токена GUIDE${NC}"
GUIDE_TOKEN=$(get_token "guide1" "password" "GUIDE")
if [ $? -eq 0 ]; then
    echo $GUIDE_TOKEN > tokens/guide_token.txt
    echo -e "${GREEN}Сохранен в: tokens/guide_token.txt${NC}"
fi
echo ""

echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}Все токены получены и сохранены!${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "${YELLOW}Использование:${NC}"
echo -e "  export ADMIN_TOKEN=\$(cat tokens/admin_token.txt)"
echo -e "  export FOREMAN_TOKEN=\$(cat tokens/foreman_token.txt)"
echo -e "  export WORKER_TOKEN=\$(cat tokens/worker_token.txt)"
echo -e "  export MASTER_TOKEN=\$(cat tokens/master_token.txt)"
echo -e "  export GUIDE_TOKEN=\$(cat tokens/guide_token.txt)"
echo ""

