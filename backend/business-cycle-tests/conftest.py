"""
Pytest fixtures для бизнес-тестов
"""
import pytest
import os
from selenium import webdriver
from selenium.webdriver.chrome.service import Service as ChromeService
from selenium.webdriver.firefox.service import Service as FirefoxService
from webdriver_manager.chrome import ChromeDriverManager
from webdriver_manager.firefox import GeckoDriverManager
from selenium.webdriver.chrome.options import Options as ChromeOptions
from selenium.webdriver.firefox.options import Options as FirefoxOptions
from datetime import datetime
import config

@pytest.fixture(scope="function")
def driver():
    """
    Создает экземпляр WebDriver для каждого теста
    """
    # Создаем директорию для скриншотов если не существует
    if not os.path.exists(config.SCREENSHOTS_DIR):
        os.makedirs(config.SCREENSHOTS_DIR)
    
    # Настройка опций браузера
    if config.BROWSER == "firefox":
        options = FirefoxOptions()
        if config.HEADLESS:
            options.add_argument("--headless")
        options.add_argument("--width=1920")
        options.add_argument("--height=1080")
        driver = webdriver.Firefox(
            service=FirefoxService(GeckoDriverManager().install()),
            options=options
        )
    else:  # chrome по умолчанию
        options = ChromeOptions()
        if config.HEADLESS:
            options.add_argument("--headless=new")
        options.add_argument("--no-sandbox")
        options.add_argument("--disable-dev-shm-usage")
        options.add_argument("--window-size=1920,1080")
        options.add_argument("--disable-gpu")
        options.add_argument("--disable-blink-features=AutomationControlled")
        
        # ВАЖНО: Отключаем уведомление Chrome "Change Your Password"
        prefs = {
            "credentials_enable_service": False,
            "profile.password_manager_enabled": False,
            "profile.password_manager_leak_detection": False  # Это ключевой параметр!
        }
        options.add_experimental_option("prefs", prefs)
        
        # Получаем путь к ChromeDriver через webdriver-manager
        initial_path = ChromeDriverManager().install()
        print(f"ChromeDriver path from manager: {initial_path}")
        
        # ВСЕГДА ищем правильный файл chromedriver, т.к. webdriver-manager может вернуть неправильный
        print("Searching for correct chromedriver executable...")
        base_dir = initial_path if os.path.isdir(initial_path) else os.path.dirname(initial_path)
        
        chrome_driver_path = None
        # Ищем исполняемый файл chromedriver рекурсивно
        for root, dirs, files in os.walk(base_dir):
            for file in files:
                # Ищем ТОЛЬКО файл с именем "chromedriver" или "chromedriver.exe" (без других расширений!)
                if file == "chromedriver" or file == "chromedriver.exe":
                    potential_path = os.path.join(root, file)
                    # Проверяем размер файла (должен быть > 1MB, т.к. настоящий драйвер большой)
                    try:
                        file_size = os.stat(potential_path).st_size
                        if file_size > 1_000_000:  # больше 1MB
                            chrome_driver_path = potential_path
                            print(f"Found chromedriver: {chrome_driver_path} ({file_size / 1024 / 1024:.1f} MB)")
                            break
                    except:
                        pass
            if chrome_driver_path:
                break
        
        if not chrome_driver_path:
            raise FileNotFoundError(
                f"ChromeDriver executable not found in {base_dir}. "
                "Try: rm -rf ~/.wdm && ./setup.sh"
            )
        
        # Делаем файл исполняемым
        if not os.access(chrome_driver_path, os.X_OK):
            print(f"Making chromedriver executable: {chrome_driver_path}")
            os.chmod(chrome_driver_path, 0o755)
        
        print(f"Using ChromeDriver: {chrome_driver_path}")
        
        driver = webdriver.Chrome(
            service=ChromeService(chrome_driver_path),
            options=options
        )
    
    # Настройка таймаутов
    driver.implicitly_wait(config.IMPLICIT_WAIT)
    driver.set_page_load_timeout(config.PAGE_LOAD_TIMEOUT)
    
    yield driver
    
    # Закрытие браузера после теста
    driver.quit()

@pytest.fixture(scope="function")
def take_screenshot(driver, request):
    """
    Делает скриншот при падении теста
    """
    yield
    
    if request.node.rep_call.failed:
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        test_name = request.node.name
        screenshot_path = os.path.join(
            config.SCREENSHOTS_DIR,
            f"{test_name}_{timestamp}.png"
        )
        driver.save_screenshot(screenshot_path)
        print(f"\nСкриншот сохранен: {screenshot_path}")

@pytest.hookimpl(tryfirst=True, hookwrapper=True)
def pytest_runtest_makereport(item, call):
    """
    Hook для получения информации о результате теста
    """
    outcome = yield
    rep = outcome.get_result()
    setattr(item, f"rep_{rep.when}", rep)

