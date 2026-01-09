package com.example.demo.database;

import com.example.demo.models.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты ограничений БД
 * Проверяет UNIQUE, NOT NULL, CHECK constraints и Foreign Keys
 */
@DisplayName("Тесты ограничений БД")
public class ConstraintsTest extends BaseDatabaseTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    @DisplayName("username должен быть уникальным")
    public void testUsernameUnique() {
        String username = "unique_user_" + System.currentTimeMillis();

        // Создаем первого пользователя
        User user1 = new User();
        user1.setUsername(username);
        user1.setPassword("password");
        user1.setRole(Role.WORKER);
        userRepository.save(user1);

        // Пытаемся создать второго с таким же username
        User user2 = new User();
        user2.setUsername(username);
        user2.setPassword("password2");
        user2.setRole(Role.ADMIN);

        assertThrows(DataIntegrityViolationException.class, () -> {
            userRepository.saveAndFlush(user2);
        }, "Должна быть ошибка при попытке создать пользователя с существующим username");
    }

    @Test
    @DisplayName("ticket_number должен быть уникальным")
    public void testTicketNumberUnique() {
        // Генерируем номер не длиннее 10 символов
        String ticketNumber = String.format("T%09d", System.currentTimeMillis() % 1000000000L);

        GoldenTicket ticket1 = new GoldenTicket();
        ticket1.setTicketNumber(ticketNumber);
        ticket1.setStatus(TicketStatus.ACTIVE);
        goldenTicketRepository.save(ticket1);

        GoldenTicket ticket2 = new GoldenTicket();
        ticket2.setTicketNumber(ticketNumber);
        ticket2.setStatus(TicketStatus.ACTIVE);

        assertThrows(DataIntegrityViolationException.class, () -> {
            goldenTicketRepository.saveAndFlush(ticket2);
        }, "Должна быть ошибка при попытке создать билет с существующим ticket_number");
    }

    @Test
    @DisplayName("NOT NULL constraint: username не может быть NULL")
    public void testUserUsernameNotNull() {
        assertThrows(DataIntegrityViolationException.class, () -> {
            jdbcTemplate.update("INSERT INTO users (username, password, role, created_at) VALUES (NULL, 'pass', 'WORKER', NOW())");
        }, "username не может быть NULL");
    }

    @Test
    @DisplayName("NOT NULL constraint: role не может быть NULL")
    public void testUserRoleNotNull() {
        assertThrows(DataIntegrityViolationException.class, () -> {
            jdbcTemplate.update("INSERT INTO users (username, password, role, created_at) VALUES ('test_user', 'pass', NULL, NOW())");
        }, "role не может быть NULL");
    }

    @Test
    @DisplayName("NOT NULL constraint для equipment.workshop_id")
    public void testEquipmentWorkshopIdNotNull() {
        assertThrows(DataIntegrityViolationException.class, () -> {
            jdbcTemplate.update(
                "INSERT INTO equipment (name, status, health, workshop_id) VALUES ('Test', 'WORKING', 100, NULL)"
            );
        }, "workshop_id не может быть NULL для equipment");
    }

    @Test
    @DisplayName("Foreign Key constraint: equipment.workshop_id должен ссылаться на существующий workshop")
    public void testEquipmentWorkshopForeignKey() {
        Long nonExistentWorkshopId = 99999L;

        assertThrows(DataIntegrityViolationException.class, () -> {
            jdbcTemplate.update(
                "INSERT INTO equipment (name, status, health, workshop_id) VALUES ('Test', 'WORKING', 100, ?)",
                nonExistentWorkshopId
            );
        }, "Нельзя создать equipment с несуществующим workshop_id");
    }

    @Test
    @DisplayName("Foreign Key constraint: task.user_id должен ссылаться на существующего user")
    public void testTaskUserForeignKey() {
        Long nonExistentUserId = 99999L;

        assertThrows(DataIntegrityViolationException.class, () -> {
            jdbcTemplate.update(
                "INSERT INTO tasks (name, status, user_id, created_at) VALUES ('Test', 'IN_PROGRESS', ?, NOW())",
                nonExistentUserId
            );
        }, "Нельзя создать task с несуществующим user_id");
    }

    @Test
    @DisplayName("Проверка ENUM значений для Role")
    public void testRoleEnumValues() {
        User user = new User();
        user.setUsername("enum_test_" + System.currentTimeMillis());
        user.setPassword("password");
        user.setRole(Role.WORKER);
        userRepository.save(user);

        // Проверяем, что значение сохранилось как строка
        String roleFromDb = jdbcTemplate.queryForObject(
            "SELECT role FROM users WHERE username = ?",
            String.class, user.getUsername()
        );
        assertEquals("WORKER", roleFromDb, "Role должен быть сохранен как строка");

        // Попытка вставить невалидное значение через SQL должна упасть
        assertThrows(Exception.class, () -> {
            jdbcTemplate.update(
                "INSERT INTO users (username, password, role, created_at) VALUES (?, 'pass', 'INVALID_ROLE', NOW())",
                "invalid_role_user"
            );
        }, "Должна быть ошибка при попытке вставить невалидное значение ENUM");
    }

    @Test
    @DisplayName("Проверка ENUM значений для EquipmentStatus")
    public void testEquipmentStatusEnumValues() {
        Workshop workshop = new Workshop();
        workshop.setName("Test Workshop");
        workshop = workshopRepository.save(workshop);

        Equipment equipment = new Equipment();
        equipment.setName("Test Equipment");
        equipment.setStatus(EquipmentStatus.WORKING);
        equipment.setHealth(100);
        equipment.setWorkshop(workshop);
        equipmentRepository.save(equipment);

        String statusFromDb = jdbcTemplate.queryForObject(
            "SELECT status FROM equipment WHERE name = ?",
            String.class, "Test Equipment"
        );
        assertEquals("WORKING", statusFromDb);
    }

    @Test
    @DisplayName("Health должен быть в диапазоне 0-100")
    public void testEquipmentHealthConstraint() {
        Workshop workshop = new Workshop();
        workshop.setName("Workshop for health test");
        workshop = workshopRepository.save(workshop);
        Long workshopId = workshop.getId();

        // Health = -1 (невалидно)
        Equipment equipment1 = new Equipment();
        equipment1.setName("Equipment with invalid health");
        equipment1.setStatus(EquipmentStatus.WORKING);
        equipment1.setHealth(-1);
        equipment1.setWorkshop(workshop);

        try {
            equipmentRepository.saveAndFlush(equipment1);
            fail("Health = -1 должен вызвать ошибку");
        } catch (Exception e) {
            // Ожидаемое исключение
            entityManager.clear(); // Очищаем persistence context после ошибки
        }

        // Перезагружаем workshop для следующего теста
        workshop = workshopRepository.findById(workshopId).orElseThrow();

        // Health = 101 (невалидно)
        Equipment equipment2 = new Equipment();
        equipment2.setName("Equipment with invalid health 2");
        equipment2.setStatus(EquipmentStatus.WORKING);
        equipment2.setHealth(101);
        equipment2.setWorkshop(workshop);

        try {
            equipmentRepository.saveAndFlush(equipment2);
            fail("Health = 101 должен вызвать ошибку");
        } catch (Exception e) {
            // Ожидаемое исключение
            entityManager.clear(); // Очищаем persistence context после ошибки
        }

        // Перезагружаем workshop для финального теста
        workshop = workshopRepository.findById(workshopId).orElseThrow();

        // Health = 50 (валидно)
        Equipment equipment3 = new Equipment();
        equipment3.setName("Equipment with valid health");
        equipment3.setStatus(EquipmentStatus.WORKING);
        equipment3.setHealth(50);
        equipment3.setWorkshop(workshop);

        assertDoesNotThrow(() -> {
            equipmentRepository.saveAndFlush(equipment3);
        }, "Health = 50 должен быть валидным");
    }
}

