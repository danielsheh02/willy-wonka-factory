package com.example.demo.database;

import com.example.demo.models.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты каскадных операций
 * Проверяет корректность каскадного удаления и обновления связанных сущностей
 */
@DisplayName("Тесты каскадных операций")
public class CascadeOperationsTest extends BaseDatabaseTest {

    @Test
    @DisplayName("Удаление Workshop должно удалить связанное Equipment (CASCADE)")
    public void testDeleteWorkshopCascadesToEquipment() {

        Workshop workshop = new Workshop();
        workshop.setName("Test Workshop");
        workshop.setDescription("Test");
        workshop = workshopRepository.save(workshop);

        Equipment equipment = new Equipment();
        equipment.setName("Test Equipment");
        equipment.setStatus(EquipmentStatus.WORKING);
        equipment.setHealth(100);
        equipment.setWorkshop(workshop);
        equipment = equipmentRepository.save(equipment);

        Long workshopId = workshop.getId();
        Long equipmentId = equipment.getId();

        // Проверяем, что оборудование создано
        assertEquals(1, countRows("SELECT COUNT(*) FROM equipment WHERE id = ?", equipmentId));

        // Удаляем цех через нативный SQL (чтобы проверить каскад на уровне БД)
        int deleted = jdbcTemplate.update("DELETE FROM workshops WHERE id = ?", workshopId);
        assertEquals(1, deleted, "Workshop должен быть удален");

        // Проверяем через SQL, что оборудование тоже удалено (благодаря ON DELETE CASCADE)
        assertEquals(0, countRows("SELECT COUNT(*) FROM equipment WHERE id = ?", equipmentId),
                "Equipment должно быть удалено при удалении Workshop");
        assertEquals(0, countRows("SELECT COUNT(*) FROM workshops WHERE id = ?", workshopId),
                "Workshop должен быть удален");
    }

    @Test
    @DisplayName("Удаление Excursion должно отвязать GoldenTicket (SET NULL)")
    public void testDeleteExcursionSetsTicketExcursionToNull() {
        User guide = new User();
        guide.setUsername("guide_test_" + System.currentTimeMillis());
        guide.setPassword("password");
        guide.setRole(Role.GUIDE);
        guide = userRepository.save(guide);

        Excursion excursion = new Excursion();
        excursion.setName("Test Excursion");
        excursion.setStartTime(LocalDateTime.now().plusDays(1));
        excursion.setParticipantsCount(10);
        excursion.setGuide(guide);
        excursion.setStatus(ExcursionStatus.CONFIRMED);
        excursion = excursionRepository.save(excursion);

        GoldenTicket ticket = new GoldenTicket();
        String shortNumber = String.format("T%09d", System.currentTimeMillis() % 1000000000L);
        ticket.setTicketNumber(shortNumber);
        ticket.setStatus(TicketStatus.BOOKED);
        ticket.setExcursion(excursion);
        ticket = goldenTicketRepository.save(ticket);

        Long excursionId = excursion.getId();
        Long ticketId = ticket.getId();

        // Проверяем, что билет связан с экскурсией
        Integer excursionIdFromDb = jdbcTemplate.queryForObject(
                "SELECT excursion_id FROM golden_tickets WHERE id = ?",
                Integer.class, ticketId);
        assertNotNull(excursionIdFromDb, "Билет должен быть связан с экскурсией");

        // Удаляем экскурсию через нативный SQL (чтобы проверить SET NULL на уровне БД)
        int deleted = jdbcTemplate.update("DELETE FROM excursions WHERE id = ?", excursionId);
        assertEquals(1, deleted, "Excursion должна быть удалена");

        // Проверяем через SQL, что билет остался, но excursion_id = NULL (благодаря ON DELETE SET NULL)
        assertEquals(1, countRows("SELECT COUNT(*) FROM golden_tickets WHERE id = ?", ticketId),
                "Билет должен остаться в БД");
        
        Integer excursionIdAfterDelete = jdbcTemplate.queryForObject(
                "SELECT excursion_id FROM golden_tickets WHERE id = ?",
                Integer.class, ticketId);
        assertNull(excursionIdAfterDelete,
                "excursion_id должен быть NULL после удаления экскурсии");
    }

    @Test
    @DisplayName("Удаление Excursion должно удалить все ExcursionRoute (CASCADE)")
    public void testDeleteExcursionCascadesToRoutes() {

        User guide = new User();
        guide.setUsername("guide_routes_" + System.currentTimeMillis());
        guide.setPassword("password");
        guide.setRole(Role.GUIDE);
        guide = userRepository.save(guide);

        Workshop workshop = new Workshop();
        workshop.setName("Workshop for Route");
        workshop = workshopRepository.save(workshop);

        Excursion excursion = new Excursion();
        excursion.setName("Test Excursion with Routes");
        excursion.setStartTime(LocalDateTime.now().plusDays(1));
        excursion.setParticipantsCount(10);
        excursion.setGuide(guide);
        excursion.setStatus(ExcursionStatus.CONFIRMED);
        excursion = excursionRepository.save(excursion);

        Long excursionId = excursion.getId();

        // Создаем маршрут через SQL (для чистоты теста)
        jdbcTemplate.update(
                "INSERT INTO excursion_routes (excursion_id, workshop_id, order_number, start_time, duration_minutes) VALUES (?, ?, ?, ?, ?)",
                excursionId, workshop.getId(), 1, LocalDateTime.now(), 30);

        // Проверяем, что маршрут создан
        assertEquals(1, countRows("SELECT COUNT(*) FROM excursion_routes WHERE excursion_id = ?", excursionId));

        // Удаляем экскурсию через нативный SQL (чтобы проверить каскад на уровне БД)
        int deleted = jdbcTemplate.update("DELETE FROM excursions WHERE id = ?", excursionId);
        assertEquals(1, deleted, "Excursion должна быть удалена");

        // Проверяем, что маршруты удалены (благодаря ON DELETE CASCADE)
        assertEquals(0, countRows("SELECT COUNT(*) FROM excursion_routes WHERE excursion_id = ?", excursionId),
                "ExcursionRoutes должны быть удалены при удалении Excursion");
    }

    @Test
    @DisplayName("Удаление User должно обнулить task.user_id и изменить статус задачи")
    public void testDeleteUserSetsTaskUserToNull() {

        User user = new User();
        user.setUsername("user_task_" + System.currentTimeMillis());
        user.setPassword("password");
        user.setRole(Role.WORKER);
        user = userRepository.save(user);


        Task task = new Task();
        task.setName("Test Task");
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setUser(user);
        task = taskRepository.save(task);

        Long userId = user.getId();
        Long taskId = task.getId();

        // Проверяем, что задача связана с пользователем и в статусе IN_PROGRESS
        Integer userIdFromDb = jdbcTemplate.queryForObject(
                "SELECT user_id FROM tasks WHERE id = ?",
                Integer.class, taskId);
        assertNotNull(userIdFromDb, "Task должен быть связан с User");
        
        String statusFromDb = jdbcTemplate.queryForObject(
                "SELECT status FROM tasks WHERE id = ?",
                String.class, taskId);
        assertEquals("IN_PROGRESS", statusFromDb, "Task должен быть в статусе IN_PROGRESS");

        // Удаляем пользователя через UserService (чтобы сработала бизнес-логика)
        userService.deleteUser(userId);

        // Проверяем, что задача осталась, но user_id = NULL
        assertEquals(1, countRows("SELECT COUNT(*) FROM tasks WHERE id = ?", taskId),
                "Task должен остаться в БД");
        
        Integer userIdAfterDelete = jdbcTemplate.queryForObject(
                "SELECT user_id FROM tasks WHERE id = ?",
                Integer.class, taskId);
        assertNull(userIdAfterDelete, "user_id должен быть NULL после удаления User");
        
        // Проверяем, что статус изменился на NOT_ASSIGNED
        String statusAfterDelete = jdbcTemplate.queryForObject(
                "SELECT status FROM tasks WHERE id = ?",
                String.class, taskId);
        assertEquals("NOT_ASSIGNED", statusAfterDelete, 
                "Статус задачи должен измениться на NOT_ASSIGNED после удаления User");
    }
}

