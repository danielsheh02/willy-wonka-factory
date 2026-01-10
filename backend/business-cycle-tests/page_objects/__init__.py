"""
Page Object Model для бизнес-тестов
"""
from .base_page import BasePage
from .login_page import LoginPage
from .tasks_page import TasksPage
from .users_page import UsersPage
from .excursions_page import ExcursionsPage
from .equipment_page import EquipmentPage
from .golden_tickets_page import GoldenTicketsPage
from .public_booking_page import PublicBookingPage
from .notification_bell import NotificationBell

__all__ = [
    'BasePage',
    'LoginPage',
    'TasksPage',
    'UsersPage',
    'ExcursionsPage',
    'EquipmentPage',
    'GoldenTicketsPage',
    'PublicBookingPage',
    'NotificationBell',
]

