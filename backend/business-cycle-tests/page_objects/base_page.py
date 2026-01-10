"""
Базовый класс для Page Object Model
"""
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.common.by import By
from selenium.common.exceptions import TimeoutException
from selenium.webdriver.common.keys import Keys
import config

class BasePage:
    """
    Базовый класс для всех Page Objects
    """
    
    def __init__(self, driver):
        self.driver = driver
        self.wait = WebDriverWait(driver, config.EXPLICIT_WAIT)
    
    def open(self, url):
        """Открыть URL"""
        self.driver.get(url)
    
    def find_element(self, locator):
        """Найти элемент с ожиданием"""
        return self.wait.until(EC.presence_of_element_located(locator))
    
    def find_elements(self, locator):
        """Найти несколько элементов"""
        return self.driver.find_elements(*locator)
    
    def click(self, locator):
        """Кликнуть по элементу с ожиданием кликабельности"""
        element = self.wait.until(EC.element_to_be_clickable(locator))
        element.click()

    def set_datetime_local(self, locator, value):
        element = self.find_element(locator)
        self.driver.execute_script(
            """
            const input = arguments[0];
            const value = arguments[1];

            // берём нативный setter
            const nativeInputValueSetter =
                Object.getOwnPropertyDescriptor(
                    window.HTMLInputElement.prototype,
                    'value'
                ).set;

            // устанавливаем значение "по-настоящему"
            nativeInputValueSetter.call(input, value);

            // сообщаем React, что было изменение
            input.dispatchEvent(new Event('input', { bubbles: true }));
            """,
            element,
            value
        )
    
    def input_text(self, locator, text):
        """Ввести текст в поле"""
        element = self.find_element(locator)
        element.clear()
        element.send_keys(text)

    def input_text_clear(self, locator, text):
        """
        Очистить Material-UI TextField и ввести текст
        """
        element = self.find_element(locator)
        element.click()
        element.send_keys(Keys.CONTROL, "a")
        element.send_keys(Keys.BACKSPACE)
        element.send_keys(text)
    
    def get_text(self, locator):
        """Получить текст элемента"""
        element = self.find_element(locator)
        return element.text
    
    def is_element_visible(self, locator, timeout=5):
        """Проверить видимость элемента"""
        try:
            wait = WebDriverWait(self.driver, timeout)
            wait.until(EC.visibility_of_element_located(locator))
            return True
        except TimeoutException:
            return False
    
    def wait_for_element_to_disappear(self, locator, timeout=10):
        """Ждать пока элемент исчезнет"""
        wait = WebDriverWait(self.driver, timeout)
        wait.until(EC.invisibility_of_element_located(locator))
    
    def scroll_to_element(self, locator):
        """Прокрутить до элемента"""
        element = self.find_element(locator)
        self.driver.execute_script("arguments[0].scrollIntoView(true);", element)
    
    def wait_for_url_contains(self, url_part, timeout=10):
        """Ждать пока URL содержит определенную строку"""
        wait = WebDriverWait(self.driver, timeout)
        wait.until(EC.url_contains(url_part))

