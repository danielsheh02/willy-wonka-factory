"""
Page Object для публичной страницы бронирования
"""
from selenium.webdriver.common.by import By
from page_objects.base_page import BasePage
import config
import time

class PublicBookingPage(BasePage):
    """
    Page Object для публичной страницы бронирования билетов
    """
    
    # Локаторы для Material-UI компонентов
    # Шаг 1: Ввод номера билета
    TICKET_NUMBER_INPUT = (By.XPATH, "//label[contains(text(), 'Номер билета')]/following-sibling::div//input")
    CHECK_TICKET_BUTTON = (By.XPATH, "//button[contains(text(), 'Проверить билет')]")
    # Шаг 2: Выбор экскурсии (это List с ListItemButton, а не Card)
    EXCURSION_LIST_ITEM = "//div[@role='button' and contains(@class, 'MuiListItemButton') and contains(., '{}')]"
    # Шаг 3: Информация о держателе
    HOLDER_NAME_INPUT = (By.XPATH, "//label[contains(text(), 'Фамилия Имя')]/following-sibling::div//input")
    HOLDER_EMAIL_INPUT = (By.XPATH, "//label[contains(text(), 'Email')]/following-sibling::div//input[@type='email']")
    BOOK_BUTTON = (By.XPATH, "//button[contains(text(), 'Забронировать')]")
    # Шаг 4: Результат
    SUCCESS_MESSAGE = (By.XPATH, "//h6[contains(text(), 'успешно') or contains(text(), 'успех')]")
    SUCCESS_ALERT = (By.XPATH, "//div[contains(@class, 'MuiAlert-standardSuccess')]")
    NOTIFICATION = (By.XPATH, "//div[contains(@class, 'MuiAlert')]")
    
    def __init__(self, driver):
        super().__init__(driver)
        self.url = f"{config.BASE_URL}/booking"
    
    def open(self):
        """Открыть страницу публичного бронирования"""
        super().open(self.url)
    
    def check_ticket(self, ticket_number):
        """
        Проверить билет по номеру
        """
        time.sleep(1)
        self.input_text(self.TICKET_NUMBER_INPUT, ticket_number)
        self.click(self.CHECK_TICKET_BUTTON)
        time.sleep(2)
    
    def select_excursion(self, excursion_name):
        """
        Выбрать экскурсию из списка (клик по ListItemButton)
        """
        excursion_item = (By.XPATH, self.EXCURSION_LIST_ITEM.format(excursion_name))
        self.click(excursion_item)
        time.sleep(1)
    
    def book_with_holder_info(self, holder_name, holder_email):
        """
        Заполнить информацию о держателе и забронировать
        """
        self.input_text(self.HOLDER_NAME_INPUT, holder_name)
        self.input_text(self.HOLDER_EMAIL_INPUT, holder_email)
        self.click(self.BOOK_BUTTON)
        time.sleep(2)
    
    def is_booking_successful(self):
        """
        Проверить успешность бронирования (по h6 с текстом "успешно" или Alert)
        """
        return (self.is_element_visible(self.SUCCESS_MESSAGE, timeout=5) or 
                self.is_element_visible(self.SUCCESS_ALERT, timeout=5))
    
    def get_notification_text(self):
        """Получить текст уведомления"""
        return self.get_text(self.NOTIFICATION)

