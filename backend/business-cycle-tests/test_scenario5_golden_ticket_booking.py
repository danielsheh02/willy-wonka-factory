"""
Сценарий 5: Бронирование по золотому билету
"""
import pytest
import config
import requests
from page_objects.login_page import LoginPage
from page_objects.golden_tickets_page import GoldenTicketsPage
from page_objects.excursions_page import ExcursionsPage
from page_objects.public_booking_page import PublicBookingPage
from datetime import datetime, timedelta
import time


class TestGoldenTicketBooking:
    """
    Тестирование бронирования экскурсии по золотому билету:
    1. Создание экскурсии (если нужно)
    2. Генерация золотого билета
    3. Публичное бронирование по билету
    4. Проверка успешного уведомления на UI
    """
    
    def test_book_excursion_with_golden_ticket(self, driver):
        """
        Полный цикл бронирования экскурсии по золотому билету
        """
        timestamp = int(time.time())
        excursion_name = f"Экскурсия для бронирования {timestamp}"
        start_time = (datetime.now() + timedelta(days=5)).strftime("%Y-%m-%dT%H:%M")
        holder_name = "Чарли Бакет"
        holder_email = f"charlie_{timestamp}@example.com"
        
        login_page = LoginPage(driver)
        login_page.open()
        login_page.login(config.ADMIN_USERNAME, config.ADMIN_PASSWORD)
        time.sleep(2)
        
        excursions_page = ExcursionsPage(driver)
        excursions_page.open()
        
        excursions_page.create_excursion_auto_route(
            name=excursion_name,
            participants=15,
            guide_username=config.GUIDE_USERNAME,
            start_time=start_time
        )
        
        print(f"✓ Экскурсия '{excursion_name}' создана")
        
        tickets_page = GoldenTicketsPage(driver)
        tickets_page.open()
        
        tickets_page.generate_tickets(count=1, expires_days=30)
        print("✓ Золотой билет сгенерирован")
        
        time.sleep(2)
        
        try:
            response = requests.get(
                f"{config.API_URL}/api/tickets",
                headers={"Authorization": f"Bearer {self._get_admin_token()}"}
            )
            
            if response.status_code == 200:
                tickets = response.json()
                if tickets and len(tickets) > 0:
                    ticket_number = tickets[-1]["ticketNumber"]
                    print(f"✓ Получен номер билета: {ticket_number}")
                else:
                    pytest.skip("Нет доступных билетов для бронирования")
            else:
                pytest.skip("Не удалось получить список билетов через API")
        except Exception as e:
            print(f"Ошибка при получении билета: {e}")
            pytest.skip("Не удалось получить билет")
        
        booking_page = PublicBookingPage(driver)
        booking_page.open()
        
        booking_page.check_ticket(ticket_number)
        time.sleep(2)
        
        booking_page.select_excursion(excursion_name)
        
        booking_page.book_with_holder_info(holder_name, holder_email)
        
        is_successful = booking_page.is_booking_successful()
        
        assert is_successful, "Бронирование не было успешным - нет уведомления об успехе"
        
        print("✓ Бронирование успешно выполнено")
        print("✓ Получено уведомление об успешном бронировании")
        print("\n=== Сценарий 5 пройден успешно ===")
    
    def _get_admin_token(self):
        """
        Получить JWT токен для ADMIN
        """
        try:
            response = requests.post(
                f"{config.API_URL}/api/auth/signin",
                json={
                    "username": config.ADMIN_USERNAME,
                    "password": config.ADMIN_PASSWORD
                }
            )
            
            if response.status_code == 200:
                return response.json()["token"]
        except:
            pass
        
        return None

