"""
Сценарий 4: Генерация золотых билетов
"""
import pytest
import config
from page_objects.login_page import LoginPage
from page_objects.golden_tickets_page import GoldenTicketsPage
import time


class TestGoldenTicketGeneration:
    """
    Тестирование генерации золотых билетов:
    1. Вход как ADMIN
    2. Генерация указанного количества билетов
    3. Проверка отображения сгенерированных билетов на фронтенде
    """
    
    def test_generate_golden_tickets(self, driver):
        """
        Генерация золотых билетов и проверка их отображения
        """
        tickets_to_generate = 5
        
        login_page = LoginPage(driver)
        login_page.open()
        login_page.login(config.ADMIN_USERNAME, config.ADMIN_PASSWORD)
        time.sleep(2)
        
        tickets_page = GoldenTicketsPage(driver)
        tickets_page.open()
        
        initial_count = tickets_page.get_tickets_count_in_table()
        print(f"✓ Начальное количество билетов: {initial_count}")
        
        tickets_page.generate_tickets(count=tickets_to_generate, expires_days=30)
        
        print(f"✓ Запущена генерация {tickets_to_generate} билетов")
        
        time.sleep(2)
        
        driver.refresh()
        time.sleep(2)
        
        final_count = tickets_page.get_tickets_count_in_table()
        print(f"✓ Конечное количество билетов: {final_count}")
        
        assert final_count >= initial_count + tickets_to_generate, \
            f"Билеты не появились в таблице. Ожидалось минимум {initial_count + tickets_to_generate}, получено {final_count}"
        
        print(f"✓ Билеты успешно сгенерированы и отображаются на фронтенде")
        print(f"✓ Добавлено билетов: {final_count - initial_count}")
        print("\n=== Сценарий 4 пройден успешно ===")

