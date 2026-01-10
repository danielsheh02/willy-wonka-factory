"""
Page Object для страницы оборудования
"""
from selenium.webdriver.common.by import By
from page_objects.base_page import BasePage
import config
import time

class EquipmentPage(BasePage):
    """
    Page Object для страницы оборудования
    """
    
    # Локаторы для Material-UI компонентов
    CREATE_BUTTON = (By.XPATH, "//button[contains(text(), 'Добавить')]")
    DIALOG_TITLE = (By.XPATH, "//h2[contains(text(), 'оборудование')]")
    # TextField компоненты
    NAME_INPUT = (By.XPATH, "//label[contains(text(), 'Название')]/following-sibling::div//input")
    MODEL_INPUT = (By.XPATH, "//label[contains(text(), 'Модель')]/following-sibling::div//input")
    DESCRIPTION_INPUT = (By.XPATH, "//label[contains(text(), 'Описание')]/following-sibling::div//input")
    HEALTH_INPUT = (By.XPATH, "//label[contains(text(), 'Состояние')]/following-sibling::div//input[@type='number']")
    TEMPERATURE_INPUT = (By.XPATH, "//label[contains(text(), 'Температура')]/following-sibling::div//input[@type='number']")
    # Select компоненты
    STATUS_SELECT = (By.XPATH, "//label[contains(text(), 'Статус')]/..//div[@role='combobox' or contains(@class, 'MuiSelect')]")
    WORKSHOP_SELECT = (By.XPATH, "//label[contains(text(), 'Цех')]/..//div[@role='combobox' or contains(@class, 'MuiSelect')]")
    OPTION = "//li[@role='option' and contains(text(), '{}')]"
    SAVE_BUTTON = (By.XPATH, "//button[contains(text(), 'Сохранить')]")
    NOTIFICATION = (By.XPATH, "//div[contains(@class, 'MuiAlert') or contains(@class, 'MuiSnackbar')]")
    
    # Строка таблицы с конкретным оборудованием
    EQUIPMENT_ROW = "//div[@role='row' and contains(., '{}')]"
    EDIT_BUTTON_IN_ROW = "//div[@role='row' and contains(., '{}')]//button[@title='Редактировать']"
    DELETE_BUTTON_IN_ROW = "//div[@role='row' and contains(., '{}')]//button[@title='Удалить']"
    CONFIRM_DELETE_BUTTON = (By.XPATH, "//button[contains(text(), 'Удалить') and not(contains(@title, 'Удалить'))]")
    
    def __init__(self, driver):
        super().__init__(driver)
        self.url = f"{config.BASE_URL}/equipment"
    
    def open(self):
        """Открыть страницу оборудования"""
        super().open(self.url)
    
    def create_equipment(self, name, model, health, workshop_name):
        """
        Создать новое оборудование
        """
        time.sleep(1)
        self.click(self.CREATE_BUTTON)
        # Ждем появления Dialog
        from selenium.webdriver.support import expected_conditions as EC
        dialog_locator = (By.XPATH, "//div[contains(@class, 'MuiDialog-root')]")
        self.wait.until(EC.presence_of_element_located(dialog_locator))
        time.sleep(0.5)
        
        # Заполняем поля
        self.input_text(self.NAME_INPUT, name)
        self.input_text(self.MODEL_INPUT, model)
        self.input_text_clear(self.HEALTH_INPUT, str(health))
        
        # Выбираем цех
        self.click(self.WORKSHOP_SELECT)
        time.sleep(0.3)
        workshop_locator = (By.XPATH, self.OPTION.format(workshop_name))
        self.click(workshop_locator)
        
        # Сохраняем
        self.click(self.SAVE_BUTTON)
        self.wait_for_notification()
        # Ждем обновления таблицы после закрытия Dialog
        time.sleep(2)
    
    def edit_equipment(self, equipment_name, new_health=None, new_temperature=None):
        """
        Редактировать оборудование
        """
        # Нажимаем кнопку редактирования
        edit_button = (By.XPATH, self.EDIT_BUTTON_IN_ROW.format(equipment_name))
        self.click(edit_button)
        
        # Ждем появления диалога
        self.find_element(self.DIALOG_TITLE)
        time.sleep(0.5)
        
        # Изменяем поля если указаны
        if new_health is not None:
            self.input_text_clear(self.HEALTH_INPUT, "11")
        
        if new_temperature is not None:
            self.input_text_clear(self.TEMPERATURE_INPUT, str(new_temperature))
        
        # Сохраняем
        self.click(self.SAVE_BUTTON)
        self.wait_for_notification()
    
    def delete_equipment(self, equipment_name):
        """
        Удалить оборудование
        """
        # Нажимаем кнопку удаления
        delete_button = (By.XPATH, self.DELETE_BUTTON_IN_ROW.format(equipment_name))
        self.click(delete_button)
        
        # Подтверждаем удаление
        time.sleep(0.5)
        self.click(self.CONFIRM_DELETE_BUTTON)
        self.wait_for_notification()
    
    def is_equipment_exists(self, equipment_name):
        """Проверить существование оборудования в таблице"""
        equipment_locator = (By.XPATH, self.EQUIPMENT_ROW.format(equipment_name))
        return self.is_element_visible(equipment_locator, timeout=10)
    
    def wait_for_notification(self):
        """Ждать появления уведомления"""
        self.find_element(self.NOTIFICATION)
        time.sleep(1)

