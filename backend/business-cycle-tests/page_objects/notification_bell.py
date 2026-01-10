"""
Page Object для компонента уведомлений (колокольчик)
"""
from selenium.webdriver.common.by import By
from page_objects.base_page import BasePage
import time

class NotificationBell(BasePage):
    """
    Page Object для компонента уведомлений
    """
    
    # Локаторы для компонента уведомлений в AppBar
    # Badge (счетчик) внутри кнопки с иконкой колокольчика
    BELL_BADGE = (By.XPATH, "//button[contains(@class, 'MuiIconButton')]//span[contains(@class, 'MuiBadge-badge')]")
    # Сама кнопка колокольчика (IconButton)
    BELL_ICON = (By.XPATH, "//button[contains(@class, 'MuiIconButton') and .//svg]")
    # Menu после клика на колокольчик
    NOTIFICATION_MENU = (By.XPATH, "//ul[@role='menu']")
    NOTIFICATION_ITEM = (By.XPATH, "//li[@role='menuitem']")
    
    def __init__(self, driver):
        super().__init__(driver)
    
    def get_unread_count(self):
        """
        Получить количество непрочитанных уведомлений
        Returns:
            int: Количество уведомлений или 0 если нет badge
        """
        try:
            badge_text = self.get_text(self.BELL_BADGE)
            return int(badge_text)
        except:
            return 0
    
    def has_notifications(self):
        """
        Проверить наличие уведомлений (по badge)
        """
        return self.is_element_visible(self.BELL_BADGE, timeout=3)
    
    def open_notifications(self):
        """
        Открыть меню уведомлений
        """
        self.click(self.BELL_ICON)
        time.sleep(0.5)
        self.find_element(self.NOTIFICATION_MENU)

