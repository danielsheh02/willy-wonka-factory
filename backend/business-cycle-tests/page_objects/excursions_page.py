"""
Page Object для страницы экскурсий
"""
from selenium.webdriver.common.by import By
from page_objects.base_page import BasePage
import config
import time

class ExcursionsPage(BasePage):
    """
    Page Object для страницы экскурсий
    """
    
    # Локаторы для Material-UI компонентов
    CREATE_BUTTON = (By.XPATH, "//button[contains(text(), 'Создать экскурсию')]")
    DIALOG_TITLE = (By.XPATH, "//h2[contains(text(), 'экскурсию')]")
    # Строка экскурсии в таблице
    EXCURSION_ROW = "//div[@role='row' and contains(., '{}')]"
    # Шаг 1: Основная информация
    NAME_INPUT = (By.XPATH, "//label[contains(text(), 'Название экскурсии')]/following-sibling::div//input")
    # START_TIME_INPUT = (By.XPATH, "//input[@type='datetime-local']")
    START_TIME_INPUT = (By.ID, "start-time-input")
    PARTICIPANTS_INPUT = (By.XPATH, "//label[contains(text(), 'Количество участников')]/following-sibling::div//input[@type='number']")
    GUIDE_SELECT = (By.XPATH, "//label[contains(text(), 'Экскурсовод')]/..//div[@role='combobox' or contains(@class, 'MuiSelect')]")
    STATUS_SELECT = (By.XPATH, "//label[contains(text(), 'Статус')]/..//div[@role='combobox' or contains(@class, 'MuiSelect')]")
    GUIDE_OPTION = "//li[@role='option' and contains(text(), '{}')]"
    # Навигация
    NEXT_BUTTON = (By.XPATH, "//button[contains(text(), 'Далее')]")
    CREATE_DIAG_BUTTON = (By.XPATH, "//button[contains(text(), 'CОЗДАТЬ')]")
    BACK_BUTTON = (By.XPATH, "//button[contains(text(), 'Назад')]")
    # Шаг 2: Маршрут
    AUTO_ROUTE_SWITCH = (By.XPATH, "//span[contains(text(), 'Автоматическое построение маршрута')]/..//input[@type='checkbox']")
    CHECK_AVAILABILITY_BUTTON = (By.XPATH, "//button[contains(text(), 'Проверить доступность')]")
    CREATE_SAVE_BUTTON = (By.ID, "create-excursion-save-button")
    # Уведомления и Alert'ы
    NOTIFICATION = (By.XPATH, "//div[contains(@class, 'MuiSnackbar')]//div[contains(@class, 'MuiAlert')]")
    ERROR_ALERT = (By.XPATH, "//div[contains(@class, 'MuiAlert-standardError')]")
    SUCCESS_ALERT = (By.XPATH, "//div[contains(@class, 'MuiAlert-standardSuccess')]")
    
    # Ручной маршрут
    ADD_WORKSHOP_BUTTON = (By.XPATH, "//button[contains(text(), 'Добавить цех')]")
    WORKSHOP_SELECT_TEMPLATE = "(//label[contains(text(), 'Цех')]/..//div[@role='combobox'])[{}]"
    WORKSHOP_OPTION_TEMPLATE = "//li[@role='option' and contains(text(), '{}')]"
    DURATION_INPUT_TEMPLATE = "(//label[contains(text(), 'Минут')]/following-sibling::div//input[@type='number'])[{}]"
    
    def __init__(self, driver):
        super().__init__(driver)
        self.url = f"{config.BASE_URL}/excursions"
    
    def open(self):
        """Открыть страницу экскурсий"""
        super().open(self.url)
    
    def create_excursion_auto_route(self, name, participants, guide_username, start_time=None):
        """
        Создать экскурсию с автоматическим маршрутом
        """
        time.sleep(1)
        self.click(self.CREATE_BUTTON)
        # Ждем появления Dialog
        from selenium.webdriver.support import expected_conditions as EC
        dialog_locator = (By.XPATH, "//div[contains(@class, 'MuiDialog-root')]")
        self.wait.until(EC.presence_of_element_located(dialog_locator))
        time.sleep(0.5)
        
        # ШАГ 1: Заполняем основные поля
        self.input_text(self.NAME_INPUT, name)
        
        # Устанавливаем время начала, если передано
        if start_time:
            self.driver.execute_script("""
            const el = document.getElementById('start-time-input');
            const nativeInputValueSetter = Object.getOwnPropertyDescriptor(
                HTMLInputElement.prototype, 'value'
            ).set;

            nativeInputValueSetter.call(el, arguments[0]);

            el.dispatchEvent(new Event('input', { bubbles: true }));
            """, start_time)
        
        self.input_text_clear(self.PARTICIPANTS_INPUT, str(participants))
        
        # Выбираем гида
        self.click(self.GUIDE_SELECT)
        time.sleep(0.3)
        guide_locator = (By.XPATH, self.GUIDE_OPTION.format(guide_username))
        self.click(guide_locator)
        time.sleep(0.3)
        
        # Выбираем статус "Подтверждена"
        self.click(self.STATUS_SELECT)
        time.sleep(0.3)
        confirmed_locator = (By.XPATH, "//li[@role='option']//span[contains(text(), 'Подтверждена')]")
        self.click(confirmed_locator)
        time.sleep(0.5)
        # time.sleep(10)
        # Нажимаем Далее (переход к шагу 2 - Маршрут)
        self.click(self.NEXT_BUTTON)
        time.sleep(1)
        # self.click(self.BACK_BUTTON)
        # time.sleep(1)
        # self.click(self.NEXT_BUTTON)
        # time.sleep(1)
        # self.click(self.BACK_BUTTON)
        # time.sleep(1)
        # ШАГ 2: На втором шаге Switch для автоматического маршрута должен быть включен по умолчанию
        # Ждем, пока кнопка станет доступной
        
        self.click(self.CREATE_SAVE_BUTTON)
        # self.wait.until(EC.element_to_be_clickable(self.CREATE_SAVE_BUTTON))
        time.sleep(0.5)
        
        # # Прокручиваем к кнопке и кликаем через JavaScript (чтобы избежать перекрытия)
        # button = self.find_element(self.CREATE_SAVE_BUTTON)
        # self.driver.execute_script("arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", button)
        # time.sleep(0.3)
        # self.driver.execute_script("arguments[0].click();", button)
        # time.sleep(1.5)
    
    def create_excursion_manual_route(self, name, participants, guide_username, workshops, start_time=None):
        """
        Создать экскурсию с ручным маршрутом
        
        Args:
            name: Название экскурсии
            participants: Количество участников
            guide_username: Имя пользователя гида
            workshops: Список словарей с информацией о цехах, например:
                [{"name": "Цех упаковки", "duration": 30}, {"name": "Цех смешивания", "duration": 20}]
            start_time: Время начала в формате "YYYY-MM-DDTHH:MM"
        """
        time.sleep(1)
        self.click(self.CREATE_BUTTON)
        # Ждем появления Dialog
        from selenium.webdriver.support import expected_conditions as EC
        dialog_locator = (By.XPATH, "//div[contains(@class, 'MuiDialog-root')]")
        self.wait.until(EC.presence_of_element_located(dialog_locator))
        time.sleep(0.5)
        
        # ШАГ 1: Заполняем основные поля
        self.input_text(self.NAME_INPUT, name)
        
        # Устанавливаем время начала, если передано
        if start_time:
            self.driver.execute_script("""
            const el = document.getElementById('start-time-input');
            const nativeInputValueSetter = Object.getOwnPropertyDescriptor(
                HTMLInputElement.prototype, 'value'
            ).set;

            nativeInputValueSetter.call(el, arguments[0]);

            el.dispatchEvent(new Event('input', { bubbles: true }));
            """, start_time)
        
        self.input_text_clear(self.PARTICIPANTS_INPUT, str(participants))
        
        # Выбираем гида
        self.click(self.GUIDE_SELECT)
        time.sleep(0.3)
        guide_locator = (By.XPATH, self.GUIDE_OPTION.format(guide_username))
        self.click(guide_locator)
        time.sleep(0.3)
        
        # Выбираем статус "Подтверждена"
        self.click(self.STATUS_SELECT)
        time.sleep(0.3)
        confirmed_locator = (By.XPATH, "//li[@role='option']//span[contains(text(), 'Подтверждена')]")
        self.click(confirmed_locator)
        time.sleep(0.5)
        
        # Нажимаем Далее (переход к шагу 2 - Маршрут)
        self.click(self.NEXT_BUTTON)
        time.sleep(1)
        
        # ШАГ 2: Отключаем автоматическое построение маршрута
        # Находим Switch и проверяем, включен ли он
        switch_input = self.find_element(self.AUTO_ROUTE_SWITCH)
        is_checked = switch_input.get_attribute("checked") == "true"
        
        # Если включен, кликаем по родительскому label, чтобы выключить
        if is_checked:
            switch_label = self.driver.find_element(By.XPATH, 
                "//span[contains(text(), 'Автоматическое построение маршрута')]/..")
            switch_label.click()
            time.sleep(0.5)
        
        # Добавляем цеха в маршрут
        for idx, workshop in enumerate(workshops):
            # Нажимаем "Добавить цех"
            self.click(self.ADD_WORKSHOP_BUTTON)
            time.sleep(0.5)
            
            # Выбираем цех
            workshop_select_locator = (By.XPATH, self.WORKSHOP_SELECT_TEMPLATE.format(idx + 1))
            self.click(workshop_select_locator)
            time.sleep(0.3)
            
            workshop_option = (By.XPATH, self.WORKSHOP_OPTION_TEMPLATE.format(workshop["name"]))
            self.click(workshop_option)
            time.sleep(0.3)
            
            # Устанавливаем длительность
            if "duration" in workshop:
                duration_input = (By.XPATH, self.DURATION_INPUT_TEMPLATE.format(idx + 1))
                self.input_text_clear(duration_input, str(workshop["duration"]))
                time.sleep(0.3)
        
        time.sleep(0.5)
    
    def check_route_availability(self):
        """
        Проверить доступность маршрута для ручного режима
        Returns:
            dict: {"available": bool, "message": str}
        """
        self.click(self.CHECK_AVAILABILITY_BUTTON)
        time.sleep(2)
        
        # Проверяем наличие Alert'а с результатом
        if self.is_element_visible(self.SUCCESS_ALERT, timeout=3):
            return {"available": True, "message": "Маршрут доступен"}
        elif self.is_element_visible(self.ERROR_ALERT, timeout=3):
            error_text = self.get_text(self.ERROR_ALERT)
            return {"available": False, "message": error_text}
        
        return {"available": False, "message": "Не удалось проверить доступность"}
    
    def save_excursion(self):
        """
        Сохранить экскурсию (нажать кнопку Создать/Обновить)
        """
        from selenium.webdriver.support import expected_conditions as EC
        self.wait.until(EC.element_to_be_clickable(self.CREATE_SAVE_BUTTON))
        time.sleep(0.5)
        
        # Прокручиваем к кнопке и кликаем через JavaScript
        button = self.find_element(self.CREATE_SAVE_BUTTON)
        self.driver.execute_script("arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", button)
        time.sleep(0.3)
        self.driver.execute_script("arguments[0].click();", button)
        time.sleep(1.5)
    
    def wait_for_notification(self):
        """Ждать появления уведомления"""
        self.find_element(self.NOTIFICATION)
        time.sleep(1)
    
    def get_notification_text(self):
        """Получить текст уведомления"""
        return self.get_text(self.NOTIFICATION)
    
    def is_excursion_exists(self, excursion_name):
        """Проверить существование экскурсии в таблице"""
        excursion_locator = (By.XPATH, self.EXCURSION_ROW.format(excursion_name))
        return self.is_element_visible(excursion_locator, timeout=5)

