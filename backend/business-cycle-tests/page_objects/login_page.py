"""
Page Object для страницы входа
"""
from selenium.webdriver.common.by import By
from page_objects.base_page import BasePage
import config

class LoginPage(BasePage):
    """
    Page Object для страницы логина
    """
    
    # Локаторы для Material-UI компонентов
    # TextField MUI: находим label, затем соседний div с input
    USERNAME_INPUT = (By.XPATH, "//label[contains(text(), 'Логин')]/following-sibling::div//input")
    PASSWORD_INPUT = (By.XPATH, "//label[contains(text(), 'Пароль')]/following-sibling::div//input")
    SUBMIT_BUTTON = (By.XPATH, "//button[@type='submit' and contains(text(), 'Войти')]")
    ERROR_MESSAGE = (By.XPATH, "//div[contains(@class, 'MuiAlert-standardError')]")
    
    def __init__(self, driver):
        super().__init__(driver)
        self.url = f"{config.BASE_URL}/login"
    
    def open(self):
        """Открыть страницу логина"""
        super().open(self.url)
    
    def login(self, username, password):
        """Выполнить вход"""
        self.input_text(self.USERNAME_INPUT, username)
        self.input_text(self.PASSWORD_INPUT, password)
        self.click(self.SUBMIT_BUTTON)
    
    def is_error_displayed(self):
        """Проверить отображение ошибки"""
        return self.is_element_visible(self.ERROR_MESSAGE, timeout=3)

