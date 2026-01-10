"""
Page Object для страницы задач
"""
from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC
from page_objects.base_page import BasePage
import config
import time

class TasksPage(BasePage):
    """
    Page Object для страницы задач
    """
    
    # Локаторы для Material-UI компонентов
    CREATE_BUTTON = (By.XPATH, "//button[contains(text(), 'Создать')]")
    DIALOG_TITLE = (By.XPATH, "//h2[contains(text(), 'задачу')]")
    # TextField с label="Название"
    TASK_NAME_INPUT = (By.XPATH, "//label[contains(text(), 'Название')]/following-sibling::div//input")
    # TextField с label="Описание" (multiline - это textarea)
    TASK_DESCRIPTION_INPUT = (By.XPATH, "//label[contains(text(), 'Описание')]/following-sibling::div//textarea")
    # Select для статуса
    STATUS_SELECT = (By.XPATH, "//label[contains(text(), 'Статус')]/..//div[@role='combobox' or contains(@class, 'MuiSelect')]")
    # Select для выбора рабочего
    USER_SELECT = (By.XPATH, "//label[contains(text(), 'Выбрать рабочего')]/..//div[@role='combobox' or contains(@class, 'MuiSelect')]")
    USER_OPTION = "//li[@role='option' and contains(text(), '{}')]"
    SAVE_BUTTON = (By.XPATH, "//button[contains(text(), 'Сохранить')]")
    NOTIFICATION = (By.XPATH, "//div[contains(@class, 'MuiAlert') or contains(@class, 'MuiSnackbar')]")
    
    # Кнопки переключения "Все" / "Мои"
    ALL_TASKS_BUTTON = (By.XPATH, "//button[@aria-label='все задачи' or contains(., 'Все')]")
    MY_TASKS_BUTTON = (By.XPATH, "//button[@aria-label='мои задачи' or contains(., 'Мои')]")
    
    # Строка таблицы с конкретной задачей
    TASK_ROW = "//div[@role='row' and contains(., '{}')]"
    
    # Кнопка редактирования в строке
    EDIT_BUTTON_IN_ROW = "//div[@role='row' and contains(., '{}')]//button[@title='Редактировать']"
    
    # Чипы статуса в таблице
    STATUS_CHIP = "//div[@role='row' and contains(., '{}')]//span[contains(@class, 'MuiChip')]"
    
    def __init__(self, driver):
        super().__init__(driver)
        self.url = f"{config.BASE_URL}/tasks"
    
    def open(self):
        """Открыть страницу задач"""
        super().open(self.url)
    
    def create_task(self, name, description, username, status="IN_PROGRESS"):
        """
        Создать новую задачу
        
        Args:
            name: Название задачи
            description: Описание
            username: Имя пользователя для назначения
            status: Статус задачи (по умолчанию IN_PROGRESS)
        """
        # Ждем загрузки страницы
        time.sleep(1)
        
        # Нажимаем кнопку создания
        self.click(self.CREATE_BUTTON)
        
        # Ждем появления диалога
        dialog_locator = (By.XPATH, "//div[contains(@class, 'MuiDialog-root')]")
        self.wait.until(EC.presence_of_element_located(dialog_locator))
        time.sleep(0.5)
        
        # Заполняем поля
        self.input_text(self.TASK_NAME_INPUT, name)
        self.input_text(self.TASK_DESCRIPTION_INPUT, description)
        
        # Выбираем пользователя
        self.click(self.USER_SELECT)
        time.sleep(0.3)
        
        user_locator = (By.XPATH, self.USER_OPTION.format(username))
        self.click(user_locator)
        time.sleep(0.3)
        
        # Статус уже может быть выбран по умолчанию, но можно изменить если нужно
        # Для простоты оставляем выбор статуса опциональным
        
        # Сохраняем
        self.click(self.SAVE_BUTTON)
        
        # Ждем уведомления
        self.wait_for_notification()
    
    def switch_to_my_tasks(self):
        """Переключиться на вкладку 'Мои задачи'"""
        time.sleep(0.5)
        self.click(self.MY_TASKS_BUTTON)
        time.sleep(1)
    
    def switch_to_all_tasks(self):
        """Переключиться на вкладку 'Все задачи'"""
        time.sleep(0.5)
        self.click(self.ALL_TASKS_BUTTON)
        time.sleep(1)
    
    def is_task_exists(self, task_name):
        """Проверить существование задачи в таблице"""
        task_locator = (By.XPATH, self.TASK_ROW.format(task_name))
        return self.is_element_visible(task_locator, timeout=5)
    
    def edit_task_status(self, task_name, new_status):
        """
        Изменить статус задачи
        
        Args:
            task_name: Название задачи
            new_status: Новый статус (NOT_ASSIGNED, IN_PROGRESS, COMPLETED)
        """
        # Нажимаем кнопку редактирования
        edit_button = (By.XPATH, self.EDIT_BUTTON_IN_ROW.format(task_name))
        self.click(edit_button)
        
        # Ждем появления диалога
        self.find_element(self.DIALOG_TITLE)
        time.sleep(0.5)
        
        # Выбираем статус
        self.click(self.STATUS_SELECT)
        time.sleep(0.3)
        
        status_option = (By.XPATH, f"//li[@role='option' and @data-value='{new_status}']")
        self.click(status_option)
        
        # Сохраняем
        self.click(self.SAVE_BUTTON)
        
        # Ждем уведомления
        self.wait_for_notification()
    
    def get_task_status(self, task_name):
        """Получить статус задачи из таблицы"""
        status_chip_locator = (By.XPATH, self.STATUS_CHIP.format(task_name))
        return self.get_text(status_chip_locator)
    
    def wait_for_notification(self):
        """Ждать появления уведомления"""
        self.find_element(self.NOTIFICATION)
        time.sleep(1)

