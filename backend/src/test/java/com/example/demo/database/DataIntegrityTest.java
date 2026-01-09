package com.example.demo.database;

import com.example.demo.models.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты целостности данных
 * Проверяет бизнес-правила и логическую целостность данных
 */
@DisplayName("Тесты целостности данных")
public class DataIntegrityTest extends BaseDatabaseTest {

    @Test
    @DisplayName("При создании User должно автоматически заполняться created_at")
    public void testUserCreatedAtAutoPopulated() {
        User user = new User();
        user.setUsername("user_timestamp_" + System.currentTimeMillis());
        user.setPassword("password");
        user.setRole(Role.WORKER);
        user = userRepository.save(user);

        assertNotNull(user.getCreatedAt(), "created_at должен быть заполнен автоматически");

        String createdAtFromDb = jdbcTemplate.queryForObject(
            "SELECT created_at::text FROM users WHERE id = ?",
            String.class, user.getId()
        );
        assertNotNull(createdAtFromDb, "created_at должен быть в БД");
    }

    @Test
    @DisplayName("При создании Task должно автоматически заполняться created_at")
    public void testTaskCreatedAtAutoPopulated() {
        Task task = new Task();
        task.setName("Test Task");
        task.setStatus(TaskStatus.NOT_ASSIGNED);
        task = taskRepository.save(task);

        assertNotNull(task.getCreatedAt(), "created_at должен быть заполнен автоматически");
    }

    @Test
    @DisplayName("GoldenTicket.excursion_id может быть NULL (до бронирования)")
    public void testTicketExcursionCanBeNull() {
        GoldenTicket ticket = new GoldenTicket();
        ticket.setTicketNumber(String.format("N%09d", System.currentTimeMillis() % 1000000000L));
        ticket.setStatus(TicketStatus.ACTIVE);
        ticket.setExcursion(null);
        
        assertDoesNotThrow(() -> {
            goldenTicketRepository.saveAndFlush(ticket);
        }, "excursion_id может быть NULL для активного билета");

        Integer excursionId = jdbcTemplate.queryForObject(
            "SELECT excursion_id FROM golden_tickets WHERE ticket_number = ?",
            Integer.class, ticket.getTicketNumber()
        );
        assertNull(excursionId, "excursion_id должен быть NULL");
    }

    @Test
    @DisplayName("Task.user_id может быть NULL (неназначенная задача)")
    public void testTaskUserCanBeNull() {
        Task task = new Task();
        task.setName("Unassigned Task");
        task.setStatus(TaskStatus.NOT_ASSIGNED);
        task.setUser(null);

        assertDoesNotThrow(() -> {
            taskRepository.saveAndFlush(task);
        }, "user_id может быть NULL для неназначенной задачи");

        Integer userId = jdbcTemplate.queryForObject(
            "SELECT user_id FROM tasks WHERE name = ?",
            Integer.class, "Unassigned Task"
        );
        assertNull(userId, "user_id должен быть NULL");
    }

    @Test
    @DisplayName("Workshop может существовать без оборудования")
    public void testWorkshopCanExistWithoutEquipment() {
        Workshop workshop = new Workshop();
        workshop.setName("Empty Workshop");
        workshop.setDescription("Workshop without equipment");
        workshop = workshopRepository.save(workshop);

        Long workshopId = workshop.getId();

        assertEquals(0, countRows("SELECT COUNT(*) FROM equipment WHERE workshop_id = ?", workshopId),
            "Workshop может существовать без оборудования");
    }

    @Test
    @DisplayName("Excursion.guide должен быть пользователем с ролью GUIDE")
    public void testExcursionGuideRole() {
        User guide = new User();
        guide.setUsername("valid_guide_" + System.currentTimeMillis());
        guide.setPassword("password");
        guide.setRole(Role.GUIDE);
        guide = userRepository.save(guide);

        Excursion excursion = new Excursion();
        excursion.setName("Excursion with GUIDE");
        excursion.setStartTime(LocalDateTime.now().plusDays(1));
        excursion.setParticipantsCount(10);
        excursion.setGuide(guide);
        excursion.setStatus(ExcursionStatus.CONFIRMED);

        assertDoesNotThrow(() -> {
            excursionRepository.saveAndFlush(excursion);
        }, "Экскурсия с GUIDE должна создаваться успешно");

        String role = jdbcTemplate.queryForObject(
            "SELECT u.role FROM excursions e JOIN users u ON e.guide_id = u.id WHERE e.id = ?",
            String.class, excursion.getId()
        );
        assertEquals("GUIDE", role, "Гид должен иметь роль GUIDE");
    }

    @Test
    @DisplayName("Количество ExcursionRoute для одной Excursion может быть любым (0+)")
    public void testExcursionRoutesCount() {
        User guide = new User();
        guide.setUsername("guide_routes_count_" + System.currentTimeMillis());
        guide.setPassword("password");
        guide.setRole(Role.GUIDE);
        guide = userRepository.save(guide);

        Excursion excursion = new Excursion();
        excursion.setName("Excursion for routes count test");
        excursion.setStartTime(LocalDateTime.now().plusDays(1));
        excursion.setParticipantsCount(10);
        excursion.setGuide(guide);
        excursion.setStatus(ExcursionStatus.DRAFT);
        excursion = excursionRepository.save(excursion);

        Long excursionId = excursion.getId();

        assertEquals(0, countRows("SELECT COUNT(*) FROM excursion_routes WHERE excursion_id = ?", excursionId),
            "Экскурсия может существовать без маршрутов");

        Workshop workshop1 = new Workshop();
        workshop1.setName("Workshop 1");
        workshop1 = workshopRepository.save(workshop1);

        Workshop workshop2 = new Workshop();
        workshop2.setName("Workshop 2");
        workshop2 = workshopRepository.save(workshop2);

        jdbcTemplate.update(
            "INSERT INTO excursion_routes (excursion_id, workshop_id, order_number, start_time, duration_minutes) VALUES (?, ?, ?, ?, ?)",
            excursionId, workshop1.getId(), 1, LocalDateTime.now(), 30
        );

        jdbcTemplate.update(
            "INSERT INTO excursion_routes (excursion_id, workshop_id, order_number, start_time, duration_minutes) VALUES (?, ?, ?, ?, ?)",
            excursionId, workshop2.getId(), 2, LocalDateTime.now(), 30
        );

        assertEquals(2, countRows("SELECT COUNT(*) FROM excursion_routes WHERE excursion_id = ?", excursionId),
            "Экскурсия может иметь несколько маршрутов");
    }

    @Test
    @DisplayName("Notification связан с User (NOT NULL)")
    public void testNotificationUserRelation() {
        User user = new User();
        user.setUsername("notif_user_" + System.currentTimeMillis());
        user.setPassword("password");
        user.setRole(Role.WORKER);
        user = userRepository.save(user);

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle("Test Notification");
        notification.setMessage("Test Message");
        notification.setType(NotificationType.INFO);
        notification = notificationRepository.save(notification);

        assertNotNull(notification.getUser(), "Notification должен быть связан с User");

        Integer userIdFromDb = jdbcTemplate.queryForObject(
            "SELECT user_id FROM notifications WHERE id = ?",
            Integer.class, notification.getId()
        );
        assertNotNull(userIdFromDb, "user_id в notifications должен быть NOT NULL");
    }

    @Test
    @DisplayName("Проверка дефолтных значений при создании сущностей")
    public void testDefaultValues() {
        GoldenTicket ticket = new GoldenTicket();
        ticket.setTicketNumber(String.format("D%09d", System.currentTimeMillis() % 1000000000L));
        ticket = goldenTicketRepository.save(ticket);

        assertEquals(TicketStatus.ACTIVE, ticket.getStatus(),
            "Status должен быть ACTIVE по умолчанию");

        User user = new User();
        user.setUsername("default_banned_" + System.currentTimeMillis());
        user.setPassword("password");
        user.setRole(Role.WORKER);
        user = userRepository.save(user);

        assertFalse(user.getIsBanned(), "is_banned должен быть false по умолчанию");
    }
}

