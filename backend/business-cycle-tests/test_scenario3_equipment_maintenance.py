"""
Сценарий 3: Обслуживание оборудования MASTER'ом
"""
import pytest
import config
from page_objects.login_page import LoginPage
from page_objects.equipment_page import EquipmentPage
import time


class TestEquipmentMaintenance:
    """
    Тестирование управления оборудованием:
    1. MASTER входит в систему
    2. MASTER создает оборудование
    3. MASTER редактирует оборудование
    4. MASTER удаляет оборудование
    """
    
    def test_equipment_crud_cycle(self, driver):
        """
        Полный цикл CRUD операций с оборудованием
        """
        timestamp = int(time.time())
        equipment_name = f"Тестовое оборудование {timestamp}"
        equipment_model = f"MODEL-{timestamp}"
        
        login_page = LoginPage(driver)
        login_page.open()
        login_page.login(config.MASTER_USERNAME, config.MASTER_PASSWORD)
        time.sleep(2)
        
        equipment_page = EquipmentPage(driver)
        equipment_page.open()
        
        workshop_name = "Цех упаковки"
        
        equipment_page.create_equipment(
            name=equipment_name,
            model=equipment_model,
            health=85,
            workshop_name=workshop_name
        )
        
        assert equipment_page.is_equipment_exists(equipment_name), \
            f"Оборудование '{equipment_name}' не найдено в таблице"
        
        print(f"✓ Оборудование '{equipment_name}' успешно создано")
        
        new_health = 70
        new_temperature = 45
        
        equipment_page.edit_equipment(
            equipment_name=equipment_name,
            new_health=new_health,
            new_temperature=new_temperature
        )
        
        print(f"✓ Оборудование '{equipment_name}' успешно отредактировано")
        
        equipment_page.delete_equipment(equipment_name)
        
        time.sleep(2)
        
        assert not equipment_page.is_equipment_exists(equipment_name), \
            f"Оборудование '{equipment_name}' все еще существует после удаления"
        
        print(f"✓ Оборудование '{equipment_name}' успешно удалено")
        print("\n=== Сценарий 3 пройден успешно ===")

