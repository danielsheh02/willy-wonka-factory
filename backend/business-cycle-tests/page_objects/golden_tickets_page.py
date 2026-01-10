"""
Page Object для страницы золотых билетов
"""
from selenium.webdriver.common.by import By
from page_objects.base_page import BasePage
import config
import time

class GoldenTicketsPage(BasePage):
    """
    Page Object для страницы золотых билетов
    """
    
    # Локаторы для Material-UI компонентов
    GENERATE_BUTTON = (By.XPATH, "//button[contains(text(), '+ Сгенерировать билеты')]")
    DIALOG_TITLE = (By.XPATH, "//h2[contains(text(), 'Генерация')]")
    # TextField компоненты
    COUNT_INPUT = (By.XPATH, "//label[contains(text(), 'Количество билетов')]/following-sibling::div//input[@type='number']")
    EXPIRES_INPUT = (By.XPATH, "//label[contains(text(), 'Истекает через')]/following-sibling::div//input[@type='number']")
    # Кнопка внутри Dialog (не в заголовке)
    GENERATE_CONFIRM_BUTTON = (By.XPATH, "//div[@role='dialog']//button[contains(text(), 'Сгенерировать') and not(ancestor::h2)]")
    NOTIFICATION = (By.XPATH, "//div[contains(@class, 'MuiSnackbar')]//div[contains(@class, 'MuiAlert')]")
    
    # Таблица билетов
    TICKETS_TABLE = (By.XPATH, "//div[@role='grid']")
    TICKET_ROW = "//div[@role='row' and contains(., '{}')]"
    
    def __init__(self, driver):
        super().__init__(driver)
        self.url = f"{config.BASE_URL}/tickets"
    
    def open(self):
        """Открыть страницу золотых билетов"""
        super().open(self.url)
    
    def generate_tickets(self, count, expires_days=30):
        """
        Сгенерировать золотые билеты
        
        Args:
            count: Количество билетов
            expires_days: Срок действия в днях
        """
        # time.sleep(1)
        self.click(self.GENERATE_BUTTON)
        # Ждем появления Dialog
        from selenium.webdriver.support import expected_conditions as EC
        dialog_locator = (By.XPATH, "//div[contains(@class, 'MuiDialog-root')]")
        self.wait.until(EC.presence_of_element_located(dialog_locator))
        time.sleep(0.5)
        
        # Заполняем поля
        self.input_text_clear(self.COUNT_INPUT, str(count))
        self.input_text_clear(self.EXPIRES_INPUT, str(expires_days))
        
        # Генерируем
        self.click(self.GENERATE_CONFIRM_BUTTON)
        self.wait_for_notification()
    
    def get_tickets_count_in_table(self):
        """
        Подсчитать количество билетов в таблице
        """
        time.sleep(1)
        rows = self.find_elements((By.XPATH, "//div[@role='row' and @data-rowindex]"))
        return len(rows)
    
    def wait_for_notification(self):
        """Ждать появления уведомления"""
        self.find_element(self.NOTIFICATION)
        time.sleep(1)

