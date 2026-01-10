# Устранение неполадок

## Проблема: `Exec format error` с ChromeDriver

### Симптомы:
```
OSError: [Errno 8] Exec format error: '...chromedriver...'
```

### Причина:
Webdriver-manager неправильно определил путь к исполняемому файлу ChromeDriver в новых версиях Chrome (141+).

### Решения:

#### ✅ Решение 1: Очистка кеша ChromeDriver (рекомендуется)
Полная переустановка ChromeDriver:

```bash
rm -rf ~/.wdm
./setup.sh
./run-business-tests.sh
```

Код в `conftest.py` автоматически найдет правильный исполняемый файл chromedriver и сделает его исполняемым.

#### ✅ Решение 2: Использовать Firefox (запасной вариант)
Если Chrome продолжает давать проблемы:

```bash
export BROWSER=firefox
./run-business-tests.sh
```

#### ✅ Решение 3: Ручная проверка ChromeDriver
```bash
# Найти установленный ChromeDriver
find ~/.wdm -name "chromedriver" -type f

# Сделать его исполняемым
chmod +x $(find ~/.wdm -name "chromedriver" -type f)
```

---

## Проблема: `externally-managed-environment`

### Симптомы:
```
error: externally-managed-environment
```

### Причина:
Python 3.12+ требует использование виртуального окружения для установки пакетов.

### Решение:
Запустите `setup.sh` - он автоматически создаст виртуальное окружение:

```bash
./setup.sh
```

---

## Проблема: Backend/Frontend недоступны

### Симптомы:
```
✗ Backend недоступен на http://localhost:7999!
✗ Frontend недоступен на http://localhost:3000!
```

### Решение:

**Запустите Backend (в отдельном терминале):**
```bash
cd backend
./gradlew bootRun
```

**Запустите Frontend (в отдельном терминале):**
```bash
cd willy-wonka-admin-frontend
npm start
```

**Проверьте доступность:**
```bash
curl http://localhost:7999/actuator/health
curl http://localhost:3000
```

---

## Проблема: Тесты медленно выполняются

### Решение 1: Headless режим
Запуск браузера без GUI ускоряет тесты:

```bash
export HEADLESS=true
./run-business-tests.sh
```

### Решение 2: Запуск конкретного сценария
Вместо всех тестов запустите один сценарий:

```bash
./run-specific-scenario.sh 1
```

---

## Проблема: Элементы не найдены на странице

### Возможные причины:
1. Frontend изменился (обновите локаторы в `page_objects/`)
2. Недостаточные таймауты (увеличьте в `config.py`)
3. Страница не загрузилась полностью

### Решение:
Увеличьте таймауты в `config.py`:

```python
IMPLICIT_WAIT = 15  # вместо 10
EXPLICIT_WAIT = 20  # вместо 15
```

Или запустите в видимом режиме для отладки:

```bash
export HEADLESS=false
./run-specific-scenario.sh 1
```

---

## Полезные команды

### Очистка кеша браузеров
```bash
rm -rf ~/.wdm
rm -rf screenshots/* reports/*
```

### Переустановка зависимостей
```bash
rm -rf venv
./setup.sh
```

### Проверка версий
```bash
source venv/bin/activate
pip list | grep selenium
firefox --version
google-chrome --version
```

### Просмотр логов
Скриншоты ошибок сохраняются в `screenshots/`
HTML отчеты сохраняются в `reports/`

---

## Рекомендуемая конфигурация

**Для разработки (с визуализацией, по умолчанию):**
```bash
# Chrome используется автоматически
./run-specific-scenario.sh 1
```

**Для CI/CD (быстро, без GUI):**
```bash
export HEADLESS=true
./run-business-tests.sh
```

**Для отладки проблем с Chrome:**
```bash
export BROWSER=firefox
export HEADLESS=false
./run-specific-scenario.sh 1
```

---

## Поддержка браузеров

| Браузер | Статус | Рекомендация |
|---------|--------|--------------|
| Chrome  | ✅ Поддерживается | **По умолчанию** (с автоисправлением путей) |
| Chromium| ✅ Поддерживается | Работает аналогично Chrome |
| Firefox | ✅ Стабильно | Запасной вариант |

---

## Проблема: Уведомление Chrome "Change Your Password"

### Симптомы:
- Chrome показывает всплывающее уведомление "The password you just used was found in a data breach. Google Password Manager recommends changing your password now"
- Уведомление блокирует взаимодействие с элементами страницы
- Тесты падают с `TimeoutException` или не могут найти элементы после логина

### Причина:
Google Chrome обнаруживает, что используемый пароль (`password123`) найден в базе данных утечек паролей, и показывает предупреждение безопасности.

### Решение:
✅ **Проблема уже исправлена в `conftest.py`**

Уведомление автоматически отключается через Chrome preferences:

```python
prefs = {
    "credentials_enable_service": False,
    "profile.password_manager_enabled": False,
    "profile.password_manager_leak_detection": False  # Ключевой параметр!
}
options.add_experimental_option("prefs", prefs)
```

**Дополнительных действий не требуется.** Тесты должны работать без уведомлений.

### Альтернативные решения (если проблема сохраняется):
1. **Переключиться на Firefox:**
   ```bash
   export BROWSER=firefox
   ./run-business-tests.sh
   ```

2. **Использовать более безопасные тестовые пароли** (требует изменения в `DataInitializer.java`)

---

Если проблема не решена, проверьте:
- Версию Python: `python3 --version` (должен быть 3.8+)
- Установлен ли браузер: `firefox --version` или `google-chrome --version`
- Доступность приложения: `curl http://localhost:3000` и `curl http://localhost:7999/actuator/health`

