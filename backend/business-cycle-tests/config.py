"""
Конфигурация для бизнес-тестов
"""
import os

# URL приложения
BASE_URL = os.getenv("BASE_URL", "http://localhost:3000")
API_URL = os.getenv("API_URL", "http://localhost:7999")

# Учетные данные пользователей (из DataInitializer)
FOREMAN_USERNAME = "foreman1"
FOREMAN_PASSWORD = "password"

WORKER_USERNAME = "worker1"
WORKER_PASSWORD = "password"

ADMIN_USERNAME = "admin1"
ADMIN_PASSWORD = "password"

MASTER_USERNAME = "master1"
MASTER_PASSWORD = "password"

GUIDE_USERNAME = "guide1"
GUIDE_PASSWORD = "password"

GUIDE2_USERNAME = "guide2"
GUIDE2_PASSWORD = "password"

# Таймауты (секунды)
IMPLICIT_WAIT = 10
EXPLICIT_WAIT = 15
PAGE_LOAD_TIMEOUT = 30

# Настройки браузера
HEADLESS = os.getenv("HEADLESS", "false").lower() == "true"
BROWSER = os.getenv("BROWSER", "chrome")  # chrome или firefox

# Директории
SCREENSHOTS_DIR = "screenshots"
REPORTS_DIR = "reports"

