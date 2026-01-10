# Анализ локаторов фронтенда

## Общие проблемы

1. **Material-UI компоненты**: Фронтенд использует `@mui/material`, все input-поля - это `TextField`, кнопки - `Button`, выпадающие списки - `Select`.
2. **Отсутствие test-атрибутов**: В коде нет `data-testid` или `id` атрибутов для тестирования.
3. **Динамические классы**: Material-UI генерирует динамические CSS классы (`MuiTextField-root`, `MuiButton-root`, и т.д.).

## Правильные локаторы для каждой страницы

### LoginPage (`/login`)

**Поля:**
- **Логин**: `//input[@type='text' and contains(@name, 'Логин') or following-sibling::*/text()='Логин']` или проще: `//label[text()='Логин']/following-sibling::div//input`
- **Пароль**: `//label[text()='Пароль']/following-sibling::div//input`
- **Кнопка "Войти"**: `//button[@type='submit' and contains(text(), 'Войти')]`

**Правильный подход для Material-UI:**
```python
# TextField с label="Логин"
username_input = driver.find_element(By.XPATH, "//label[contains(text(), 'Логин')]/..//input")

# TextField с label="Пароль"
password_input = driver.find_element(By.XPATH, "//label[contains(text(), 'Пароль')]/..//input")

# Button с текстом "Войти"
login_button = driver.find_element(By.XPATH, "//button[contains(text(), 'Войти')]")
```

### UsersPage (`/users`)

**Кнопка создания**: `//button[contains(text(), 'Создать')]`

**Dialog поля:**
- **Логин**: `//label[contains(text(), 'Логин')]/..//input`
- **Роль** (Select): `//label[@id='mui-component-select-Роль']` или `//div[@role='button' and contains(., 'Роль')]`
- **Пароль**: `//label[contains(text(), 'Пароль')]/..//input`
- **Кнопка "Сохранить"**: `//button[contains(text(), 'Сохранить')]`

**Проверка существования пользователя в DataGrid:**
```python
# Ищем строку в таблице по тексту username
row = driver.find_element(By.XPATH, f"//div[@role='gridcell' and contains(text(), '{username}')]")
```

### TasksPage (`/tasks`)

**Переключатель "Мои задачи"**:
```python
# ToggleButton с aria-label="мои задачи"
my_tasks_button = driver.find_element(By.XPATH, "//button[@aria-label='мои задачи' or contains(text(), 'Мои')]")
```

**Dialog создания задачи:**
- **Название**: `//label[contains(text(), 'Название')]/..//input`
- **Описание**: `//label[contains(text(), 'Описание')]/..//textarea`
- **Статус** (Select): `//div[@id='mui-component-select-status' or contains(@aria-labelledby, 'Статус')]`
- **Кнопка "Сохранить"**: `//button[contains(text(), 'Сохранить')]`

**Статус задачи в DataGrid:**
```python
# Chip с текстом "Завершена" в конкретной строке
status = driver.find_element(By.XPATH, f"//div[@role='row' and contains(., '{task_name}')]//span[contains(@class, 'MuiChip') and contains(text(), 'Завершена')]")
```

### ExcursionsPage (`/excursions`)

**Кнопка "Создать экскурсию"**: `//button[contains(text(), 'Создать экскурсию')]`

**Stepper (2 шага):**
- Шаг 1: "Основная информация"
- Шаг 2: "Маршрут"

**Шаг 1 - поля:**
- **Название**: `//label[contains(text(), 'Название экскурсии')]/..//input`
- **Дата и время**: `//input[@type='datetime-local']`
- **Количество участников**: `//label[contains(text(), 'Количество участников')]/..//input`
- **Экскурсовод** (Select): Нужно кликнуть на Select, затем выбрать из выпадающего списка
- **Статус** (Select): Аналогично
- **Кнопка "Далее"**: `//button[contains(text(), 'Далее')]`

**Шаг 2 - маршрут:**
- **Switch "Автоматическое построение"**: `//input[@type='checkbox' and following-sibling::*//*[contains(text(), 'Автоматическое')]]`
- **Кнопка "Создать"**: `//button[contains(text(), 'Создать')]`

**Уведомление об ошибке (Alert):**
```python
# Alert с severity="error"
error_alert = driver.find_element(By.XPATH, "//div[contains(@class, 'MuiAlert-root') and contains(@class, 'MuiAlert-standardError')]")
```

### EquipmentPage (`/equipment`)

**Кнопка "+ Добавить"**: `//button[contains(text(), 'Добавить')]`

**Dialog:**
- **Название**: `//label[contains(text(), 'Название')]/..//input`
- **Модель**: `//label[contains(text(), 'Модель')]/..//input`
- **Состояние**: `//label[contains(text(), 'Состояние')]/..//input[@type='number']`
- **Температура**: `//label[contains(text(), 'Температура')]/..//input[@type='number']`
- **Цех** (Select): Click на Select, выбор из списка
- **Статус** (Select): Click на Select, выбор из списка
- **Кнопка "Сохранить"**: `//button[contains(text(), 'Сохранить')]`

### GoldenTicketsPage (`/tickets`)

**Кнопка "+ Сгенерировать билеты"**: `//button[contains(text(), 'Сгенерировать билеты')]`

**Dialog:**
- **Количество**: `//label[contains(text(), 'Количество билетов')]/..//input[@type='number']`
- **Срок действия**: `//label[contains(text(), 'Истекает через')]/..//input[@type='number']`
- **Кнопка "Сгенерировать"**: `//button[contains(text(), 'Сгенерировать') and @type='button']` (не submit!)

**Проверка билетов в DataGrid:**
```python
# DataGrid должен содержать строки с билетами
tickets = driver.find_elements(By.XPATH, "//div[@role='gridcell' and contains(@data-field, 'ticketNumber')]")
```

### PublicBookingPage (`/booking`)

**Шаг 1 - Ввод номера билета:**
- **Поле "Номер билета"**: `//label[contains(text(), 'Номер билета')]/..//input`
- **Кнопка "Проверить билет"**: `//button[contains(text(), 'Проверить билет')]`

**Шаг 2 - Выбор экскурсии:**
- **Список экскурсий**: `//div[@role='button' and contains(@class, 'MuiListItemButton')]`
- **Конкретная экскурсия**: `//div[@role='button']//span[contains(text(), '{excursion_name}')]`

**Шаг 3 - Ввод данных:**
- **Фамилия Имя**: `//label[contains(text(), 'Фамилия Имя')]/..//input`
- **Email**: `//label[contains(text(), 'Email')]/..//input[@type='email']`
- **Кнопка "Забронировать"**: `//button[contains(text(), 'Забронировать')]`

**Шаг 4 - Подтверждение:**
- **Сообщение об успехе**: `//h6[contains(text(), 'успешно')]` или `//div[contains(@class, 'MuiAlert-success')]`

### NotificationBell (правая часть AppBar)

**Иконка колокольчика:**
```python
# IconButton с NotificationsIcon
bell = driver.find_element(By.XPATH, "//button[@aria-label='notifications' or contains(@class, 'MuiIconButton') and .//*[name()='svg' and contains(@class, 'Notification')]]")
```

**Badge (счетчик уведомлений):**
```python
# MuiBadge-badge внутри кнопки колокольчика
badge = driver.find_element(By.XPATH, "//button[contains(@aria-label, 'notification')]//span[@class='MuiBadge-badge']")
```

**Список уведомлений (Menu после клика):**
```python
# Menu с role="menu"
menu = driver.find_element(By.XPATH, "//ul[@role='menu']")
notifications = menu.find_elements(By.XPATH, ".//li[@role='menuitem']")
```

## Рекомендации по исправлению Page Objects

1. **Использовать XPath с `contains(text(), ...)`** для поиска по тексту label или кнопок.
2. **Для TextField**: `//label[contains(text(), 'Label Text')]/..//input`
3. **Для Button**: `//button[contains(text(), 'Button Text')]`
4. **Для Select (MUI)**: Сначала клик на Select (`//div[contains(text(), 'Select Label')]`), затем выбор из `//li[@role='option' and contains(text(), 'Option Text')]`
5. **Для DataGrid**: `//div[@role='gridcell' and contains(text(), 'Cell Value')]`
6. **Для Alert**: `//div[contains(@class, 'MuiAlert') and contains(@class, 'severity')]`
7. **Для Snackbar**: `//div[contains(@class, 'MuiSnackbar')]//div[contains(@class, 'MuiAlert')]`

## Ключевые ожидания (Explicit Waits)

```python
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC

# Ожидание появления элемента
element = WebDriverWait(driver, 10).until(
    EC.presence_of_element_located((By.XPATH, "xpath"))
)

# Ожидание кликабельности
element = WebDriverWait(driver, 10).until(
    EC.element_to_be_clickable((By.XPATH, "xpath"))
)

# Ожидание видимости
element = WebDriverWait(driver, 10).until(
    EC.visibility_of_element_located((By.XPATH, "xpath"))
)
```

## Проблемы с текущими Page Objects

1. **`login_page.py`**: Использует `name="username"` и `name="password"`, но Material-UI TextField не имеет таких атрибутов.
2. **`users_page.py`**: Использует `id="username"`, `id="role"`, которых нет.
3. **`tasks_page.py`**: Использует `id="task-name"`, `id="task-description"`, которых нет.
4. **`excursions_page.py`**: Аналогично.
5. **`equipment_page.py`**: Аналогично.
6. **`golden_tickets_page.py`**: Использует `id="count"`, которого нет.
7. **`notification_bell.py`**: Ищет `id="notification-bell"`, которого нет.

**Все Page Objects нужно переписать с использованием XPath-локаторов на основе текстового содержимого label'ов и кнопок.**

