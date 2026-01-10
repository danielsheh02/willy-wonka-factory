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
        
        # ===== Этап 1: ADMIN входит в систему =====
        login_page = LoginPage(driver)
        login_page.open()
        login_page.login(config.ADMIN_USERNAME, config.ADMIN_PASSWORD)
        time.sleep(2)
        
        # ===== Этап 2: Открываем страницу золотых билетов =====
        tickets_page = GoldenTicketsPage(driver)
        tickets_page.open()
        
        # Получаем текущее количество билетов
        initial_count = tickets_page.get_tickets_count_in_table()
        print(f"✓ Начальное количество билетов: {initial_count}")
        
        # ===== Этап 3: Генерируем новые билеты =====
        tickets_page.generate_tickets(count=tickets_to_generate, expires_days=30)
        
        print(f"✓ Запущена генерация {tickets_to_generate} билетов")
        
        # ===== Этап 4: Проверяем, что билеты появились в таблице =====
        time.sleep(2)
        
        # Обновляем страницу чтобы увидеть новые билеты
        driver.refresh()
        time.sleep(2)
        
        final_count = tickets_page.get_tickets_count_in_table()
        print(f"✓ Конечное количество билетов: {final_count}")
        
        # Проверяем, что количество увеличилось
        assert final_count >= initial_count + tickets_to_generate, \
            f"Билеты не появились в таблице. Ожидалось минимум {initial_count + tickets_to_generate}, получено {final_count}"
        
        print(f"✓ Билеты успешно сгенерированы и отображаются на фронтенде")
        print(f"✓ Добавлено билетов: {final_count - initial_count}")
        print("\n=== Сценарий 4 пройден успешно ===")

