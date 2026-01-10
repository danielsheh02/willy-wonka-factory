"""
Сценарий 1: Управление задачами FOREMAN и WORKER
"""
import pytest
import config
from page_objects.login_page import LoginPage
from page_objects.users_page import UsersPage
from page_objects.tasks_page import TasksPage
from page_objects.notification_bell import NotificationBell


class TestForemanWorkerTasks:
    """
    Тестирование полного цикла работы с задачами:
    1. FOREMAN создает пользователя WORKER
    2. FOREMAN создает задачу и назначает ее на WORKER
    3. WORKER входит в систему
    4. WORKER видит уведомление о назначенной задаче
    5. WORKER находит задачу в "МОИ ЗАДАЧИ"
    6. WORKER отмечает задачу как COMPLETED
    7. Проверяется обновление статуса на UI
    """
    
    def test_complete_task_lifecycle(self, driver):
        """
        Полный цикл жизни задачи от создания до выполнения
        """
        # Генерируем уникальные имена
        import time
        timestamp = int(time.time())
        worker_username = f"test_worker_{timestamp}"
        worker_password = "test_password123"
        task_name = f"Тестовая задача {timestamp}"
        task_description = "Проверка оборудования в цехе"
        
        # ===== Этап 1: FOREMAN входит в систему =====
        login_page = LoginPage(driver)
        login_page.open()
        login_page.login(config.FOREMAN_USERNAME, config.FOREMAN_PASSWORD)
        
        # Ожидаем перенаправления
        time.sleep(2)
        
        # ===== Этап 2: FOREMAN создает нового WORKER =====
        users_page = UsersPage(driver)
        users_page.open()
        users_page.create_user(worker_username, worker_password, "WORKER")
        
        # Проверяем, что пользователь создан
        assert users_page.is_user_exists(worker_username), \
            f"Пользователь {worker_username} не найден в таблице"
        
        # ===== Этап 3: FOREMAN создает задачу и назначает ее WORKER =====
        tasks_page = TasksPage(driver)
        tasks_page.open()
        tasks_page.create_task(
            name=task_name,
            description=task_description,
            username=worker_username,
            status="IN_PROGRESS"
        )
        
        # Переключаемся на "Все задачи" для проверки
        tasks_page.switch_to_all_tasks()
        
        # Проверяем, что задача создана
        assert tasks_page.is_task_exists(task_name), \
            f"Задача '{task_name}' не найдена в списке"
        
        # ===== Этап 4: Выход из системы и вход как WORKER =====
        login_page.open()  # Возврат на страницу логина (выход)
        time.sleep(1)
        login_page.login(worker_username, worker_password)
        time.sleep(2)
        
        # ===== Этап 5: WORKER проверяет наличие уведомлений =====
        notification_bell = NotificationBell(driver)
        
        # Проверяем, что есть непрочитанные уведомления
        has_notifications = notification_bell.has_notifications()
        assert has_notifications, "У WORKER нет уведомлений о назначенной задаче"
        
        unread_count = notification_bell.get_unread_count()
        print(f"\n✓ WORKER имеет {unread_count} непрочитанных уведомлений")
        
        # ===== Этап 6: WORKER открывает "МОИ ЗАДАЧИ" и находит задачу =====
        tasks_page.open()
        tasks_page.switch_to_my_tasks()
        
        # Проверяем наличие задачи в "Мои задачи"
        assert tasks_page.is_task_exists(task_name), \
            f"Задача '{task_name}' не найдена в разделе 'Мои задачи'"
        print(f"✓ Задача '{task_name}' найдена в разделе 'Мои задачи'")
        
        # ===== Этап 7: WORKER отмечает задачу как COMPLETED =====
        tasks_page.edit_task_status(task_name, "COMPLETED")
        
        # ===== Этап 8: Проверка обновления статуса =====
        time.sleep(1)
        status = tasks_page.get_task_status(task_name)
        assert "COMPLETED" in status or "Выполнено" in status or "Завершена" in status, \
            f"Статус задачи не обновился. Текущий статус: {status}"
        
        print(f"✓ Задача '{task_name}' успешно отмечена как COMPLETED")
        print("\n=== Сценарий 1 пройден успешно ===")

