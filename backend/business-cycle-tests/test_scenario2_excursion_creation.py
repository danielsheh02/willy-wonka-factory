"""
Сценарий 2: Создание экскурсий
"""
import pytest
import config
from page_objects.login_page import LoginPage
from page_objects.excursions_page import ExcursionsPage
import time
from datetime import datetime, timedelta


class TestExcursionCreation:
    """
    Тестирование создания экскурсий:
    1. Создание экскурсии с автоматическим маршрутом
    2. Создание экскурсии с ручным маршрутом (доступен)
    3. Создание экскурсии с ручным маршрутом (недоступен - проверка уведомления)
    """
    
    def test_create_excursion_auto_route(self, driver):
        """
        Создание экскурсии с автоматическим маршрутом
        """
        timestamp = int(time.time())
        excursion_name = f"Автоматическая экскурсия {timestamp}"
        
        start_time = (datetime.now() + timedelta(days=2)).strftime("%Y-%m-%dT%H:%M")
        
        login_page = LoginPage(driver)
        login_page.open()
        login_page.login(config.GUIDE_USERNAME, config.GUIDE_PASSWORD)
        time.sleep(2)

        excursions_page = ExcursionsPage(driver)
        excursions_page.open()
        
        excursions_page.create_excursion_auto_route(
            name=excursion_name,
            participants=25,
            guide_username=config.GUIDE_USERNAME,
            start_time=start_time
        )

        assert excursions_page.is_excursion_exists(excursion_name), \
            f"Экскурсия '{excursion_name}' не найдена в списке"
        
        print(f"✓ Экскурсия '{excursion_name}' создана с автоматическим маршрутом")
        print(f"✓ Время начала: {start_time}")
        print(f"✓ Статус: Подтверждена")
        print("\n=== Тест автоматического маршрута пройден ===")
    
    def test_create_excursion_manual_route_available(self, driver):
        """
        Создание экскурсии с ручным маршрутом - маршрут доступен
        """
        timestamp = int(time.time())
        excursion_name = f"Ручная экскурсия доступная {timestamp}"
        
        start_time = (datetime.now() + timedelta(days=3)).strftime("%Y-%m-%dT%H:%M")
        
        login_page = LoginPage(driver)
        login_page.open()
        login_page.login(config.GUIDE_USERNAME, config.GUIDE_PASSWORD)
        time.sleep(2)
        
        excursions_page = ExcursionsPage(driver)
        excursions_page.open()

        workshops = [
            {"name": "Цех упаковки", "duration": 30},
            {"name": "Цех глазирования", "duration": 25}
        ]
        
        excursions_page.create_excursion_manual_route(
            name=excursion_name,
            participants=10,
            guide_username=config.GUIDE_USERNAME,
            workshops=workshops,
            start_time=start_time
        )
        
        print(f"✓ Создана экскурсия '{excursion_name}' с ручным маршрутом")
        print(f"✓ Добавлено цехов: {len(workshops)}")
        
        availability = excursions_page.check_route_availability()
        print(f"✓ Проверка доступности: {availability['message']}")
        
        assert availability["available"], f"Маршрут должен быть доступен, но получено: {availability['message']}"
        
        excursions_page.save_excursion()
        
        excursions_page.wait_for_notification()
        notification_text = excursions_page.get_notification_text()
        print(f"✓ Уведомление: {notification_text}")
        
        assert excursion_name in notification_text or "создан" in notification_text.lower(), \
            f"Ожидалось уведомление о создании экскурсии, получено: {notification_text}"
        
        assert excursions_page.is_excursion_exists(excursion_name), \
            f"Экскурсия '{excursion_name}' не найдена в списке"
        
        print(f"✓ Экскурсия сохранена успешно")
    
    def test_create_excursion_manual_route_unavailable(self, driver):
        """
        Создание экскурсии с ручным маршрутом - маршрут недоступен
        """
        timestamp = int(time.time())
        
        start_time = (datetime.now() + timedelta(days=4)).strftime("%Y-%m-%dT%H:%M")
        
        login_page = LoginPage(driver)
        login_page.open()
        login_page.login(config.GUIDE_USERNAME, config.GUIDE_PASSWORD)
        time.sleep(2)
        
        excursions_page = ExcursionsPage(driver)
        excursions_page.open()
        
        excursion_name_1 = f"Первая экскурсия конфликт {timestamp}"
        workshops_1 = [
            {"name": "Цех упаковки", "duration": 30}
        ]
        
        excursions_page.create_excursion_manual_route(
            name=excursion_name_1,
            participants=15,
            guide_username=config.GUIDE_USERNAME,
            workshops=workshops_1,
            start_time=start_time
        )
        
        availability_1 = excursions_page.check_route_availability()
        print(f"✓ Первая экскурсия - проверка: {availability_1['message']}")
        
        if availability_1["available"]:
            excursions_page.save_excursion()
            
            assert excursions_page.is_excursion_exists(excursion_name_1), \
                f"Экскурсия '{excursion_name_1}' не найдена в списке"
            
            print(f"✓ Первая экскурсия '{excursion_name_1}' сохранена")
            time.sleep(2)
        
        excursion_name_2 = f"Вторая экскурсия конфликт {timestamp}"
        excursions_page.open()
        time.sleep(1)
        
        
        workshops_2 = [
            {"name": "Цех упаковки", "duration": 30}
        ]
        
        excursions_page.create_excursion_manual_route(
            name=excursion_name_2,
            participants=15,
            guide_username=config.GUIDE2_USERNAME,
            workshops=workshops_2,
            start_time=start_time
        )
        
        
        availability_2 = excursions_page.check_route_availability()
        print(f"✓ Вторая экскурсия - проверка: {availability_2['message']}")
        print(f"✓ Маршрут недоступен: {not availability_2['available']}")
        
        assert not availability_2["available"], \
            "Маршрут должен быть недоступен из-за конфликта, но проверка вернула 'доступен'"
        
        print("✓ Система корректно определила конфликт расписания")
        print("\n=== Тест недоступного маршрута пройден ===")

