"""
Page Object для страницы управления пользователями
"""
from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC
from page_objects.base_page import BasePage
import config
import time

# EC уже импортирован выше

class UsersPage(BasePage):
    """
    Page Object для страницы пользователей
    """
    
    # Локаторы для Material-UI компонентов
    CREATE_BUTTON = (By.XPATH, "//button[contains(text(), 'Создать')]")
    DIALOG_TITLE = (By.XPATH, "//h2[contains(text(), 'Создать') or contains(text(), 'Редактировать')]")
    # TextField с label="Логин" (находим label, затем input внутри соседнего div)
    USERNAME_INPUT = (By.XPATH, "//label[contains(text(), 'Логин')]/following-sibling::div//input")
    # TextField с label="Пароль"
    PASSWORD_INPUT = (By.XPATH, "//label[contains(text(), 'Пароль')]/following-sibling::div//input")
    # Select (MUI) с label="Роль" - нужно кликнуть по самому Select
    ROLE_SELECT = (By.XPATH, "//div[@id='mui-component-select-role' or contains(@class, 'MuiSelect-select') and ancestor::div[preceding-sibling::label[contains(text(), 'Роль')]]]")
    # Альтернативный локатор для Role Select
    ROLE_SELECT_ALT = (By.XPATH, "//label[contains(text(), 'Роль')]/..//div[@role='combobox']")
    ROLE_OPTION = "//li[@role='option' and contains(text(), '{}')]"
    SAVE_BUTTON = (By.XPATH, "//button[contains(text(), 'Сохранить')]")
    NOTIFICATION = (By.XPATH, "//div[contains(@class, 'MuiAlert') or contains(@class, 'MuiSnackbar')]")
    
    # Строка таблицы с конкретным username
    USER_ROW = "//div[@role='row' and contains(., '{}')]"
    
    def __init__(self, driver):
        super().__init__(driver)
        self.url = f"{config.BASE_URL}/users"
    
    def open(self):
        """Открыть страницу пользователей"""
        super().open(self.url)
    
    def create_user(self, username, password, role):
        """
        Создать нового пользователя
        
        Args:
            username: Логин пользователя
            password: Пароль
            role: Роль (WORKER, FOREMAN, ADMIN, MASTER, GUIDE)
        """
        # Ждем загрузки страницы
        time.sleep(1)
        
        # Нажимаем кнопку создания
        self.click(self.CREATE_BUTTON)
        
        # Ждем появления и ВИДИМОСТИ диалога (visibility, а не просто presence!)
        dialog_locator = (By.XPATH, "//div[contains(@class, 'MuiDialog-root') and @role='presentation']")
        self.wait.until(EC.visibility_of_element_located(dialog_locator))
        # Дополнительно ждем появления заголовка Dialog
        title_locator = (By.XPATH, "//h2[contains(text(), 'Создать') or contains(text(), 'Редактировать')]")
        self.wait.until(EC.visibility_of_element_located(title_locator))
        time.sleep(0.5)
        
        # Заполняем поля
        self.input_text(self.USERNAME_INPUT, username)
        self.input_text(self.PASSWORD_INPUT, password)
        
        # Выбираем роль - пробуем оба локатора
        try:
            self.click(self.ROLE_SELECT_ALT)
        except:
            self.click(self.ROLE_SELECT)
        time.sleep(0.5)
        
        # Маппинг ролей на русские названия
        role_labels = {
            "WORKER": "Рабочий",
            "FOREMAN": "Начальник цеха",
            "ADMIN": "Администратор",
            "MASTER": "Мастер",
            "GUIDE": "Экскурсовод"
        }
        
        role_label = role_labels.get(role, role)
        role_locator = (By.XPATH, self.ROLE_OPTION.format(role_label))
        self.click(role_locator)
        
        # Сохраняем
        self.click(self.SAVE_BUTTON)
        
        # Ждем уведомления
        self.wait_for_notification()
    
    def wait_for_notification(self):
        """Ждать появления уведомления"""
        self.find_element(self.NOTIFICATION)
        time.sleep(1)
    
    def is_user_exists(self, username):
        """Проверить существование пользователя в таблице"""
        user_locator = (By.XPATH, self.USER_ROW.format(username))
        return self.is_element_visible(user_locator, timeout=5)

