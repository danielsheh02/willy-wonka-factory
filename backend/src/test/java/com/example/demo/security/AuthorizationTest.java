package com.example.demo.security;

import com.example.demo.BaseTest;
import com.example.demo.dto.request.*;
import com.example.demo.models.*;
import com.example.demo.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Тесты прав доступа для всех ролей пользователей
 * Проверяет авторизацию и права доступа к эндпоинтам API
 */
@DisplayName("Тесты авторизации и прав доступа")
public class AuthorizationTest extends BaseTest {

    @Autowired
    private TaskRepository taskRepository;
    
    @Autowired
    private WorkshopRepository workshopRepository;
    
    @Autowired
    private EquipmentRepository equipmentRepository;
    
    @Autowired
    private ExcursionRepository excursionRepository;
    
    @Autowired
    private GoldenTicketRepository goldenTicketRepository;
    
    @Autowired
    private ExcursionRouteRepository excursionRouteRepository;

    private Workshop testWorkshop;

    @Override
    @BeforeEach
    public void setupBaseTest() {
        super.setupBaseTest();
        
        // Важно: удаляем в правильном порядке из-за foreign key constraints
        // 1. Золотые билеты (ссылаются на excursions)
        goldenTicketRepository.deleteAll();
        
        // 2. Маршруты экскурсий (ссылаются на workshops и excursions)
        excursionRouteRepository.deleteAll();
        
        // 3. Экскурсии (ссылаются на workshops)
        excursionRepository.deleteAll();
        
        // 4. Оборудование (ссылается на workshops)
        equipmentRepository.deleteAll();
        
        // 5. Задачи (не имеют внешних ключей на другие таблицы)
        taskRepository.deleteAll();
        
        // 6. Цеха (на них ссылаются equipment, excursions, excursion_routes)
        workshopRepository.deleteAll();
        
        testWorkshop = new Workshop();
        testWorkshop.setName("Тестовый цех");
        testWorkshop.setCapacity(50);
        testWorkshop.setVisitDurationMinutes(30);
        testWorkshop = workshopRepository.save(testWorkshop);
    }

    // ==================== Доступ без токена ====================

    @Test
    @DisplayName("Доступ без токена: GET /api/users - запрещен")
    public void testGetUsersWithoutToken() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Доступ без токена: GET /api/tasks - запрещен")
    public void testGetTasksWithoutToken() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Доступ без токена: GET /api/equipments - запрещен")
    public void testGetEquipmentsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/equipments"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Доступ без токена: GET /api/workshops - запрещен")
    public void testGetWorkshopsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/workshops"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Доступ без токена: POST /api/tickets/generate - запрещен")
    public void testGenerateTicketsWithoutToken() throws Exception {
        GenerateTicketsRequestDTO dto = new GenerateTicketsRequestDTO();
        dto.setCount(5);

        mockMvc.perform(post("/api/tickets/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Доступ без токена: GET /api/reports/statistics - запрещен")
    public void testGetStatisticsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/reports/statistics")
                .param("startDate", "2024-01-01T00:00:00")
                .param("endDate", "2024-12-31T23:59:59"))
                .andExpect(status().isForbidden());
    }

    // ==================== Публичные эндпоинты ====================

    @Test
    @DisplayName("Публичный доступ: POST /api/auth/signin - разрешен")
    public void testSigninPublicAccess() throws Exception {
        AuthUserRequestDTO dto = new AuthUserRequestDTO();
        dto.setUsername("admin_user");
        dto.setPassword("password123");

        mockMvc.perform(post("/api/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Публичный доступ: GET /api/excursions - разрешен")
    public void testGetExcursionsPublicAccess() throws Exception {
        mockMvc.perform(get("/api/excursions"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Публичный доступ: GET /api/tickets/validate/{ticketNumber} - разрешен")
    public void testValidateTicketPublicAccess() throws Exception {
        GoldenTicket ticket = new GoldenTicket("PUBLIC01");
        ticket.setStatus(TicketStatus.ACTIVE);
        goldenTicketRepository.save(ticket);

        mockMvc.perform(get("/api/tickets/validate/PUBLIC01"))
                .andExpect(status().isOk());
    }

    // ==================== ADMIN: полные права ====================

    @Test
    @DisplayName("ADMIN: POST /api/users - создание пользователя разрешено")
    public void testAdminCanCreateUser() throws Exception {
        UserRequestDTO dto = new UserRequestDTO();
        dto.setUsername("new_user_by_admin");
        dto.setPassword("password123");
        dto.setRole(Role.WORKER);

        mockMvc.perform(post("/api/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ADMIN: DELETE /api/users/{id} - удаление пользователя разрешено")
    public void testAdminCanDeleteUser() throws Exception {
        User worker = getUserForRole(Role.WORKER);

        mockMvc.perform(delete("/api/users/" + worker.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("ADMIN: POST /api/tasks - создание задачи разрешено")
    public void testAdminCanCreateTask() throws Exception {
        TaskRequestDTO dto = new TaskRequestDTO();
        dto.setName("Задача от админа");
        dto.setDescription("Описание");
        dto.setStatus(TaskStatus.NOT_ASSIGNED);

        mockMvc.perform(post("/api/tasks")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ADMIN: DELETE /api/tasks/{id} - удаление задачи разрешено")
    public void testAdminCanDeleteTask() throws Exception {
        Task task = new Task();
        task.setName("Задача");
        task.setStatus(TaskStatus.NOT_ASSIGNED);
        Task savedTask = taskRepository.save(task);

        mockMvc.perform(delete("/api/tasks/" + savedTask.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("ADMIN: POST /api/equipments - создание оборудования разрешено")
    public void testAdminCanCreateEquipment() throws Exception {
        EquipmentRequestDTO dto = new EquipmentRequestDTO();
        dto.setName("Оборудование");
        dto.setModel("TEST-100");
        dto.setStatus(EquipmentStatus.WORKING);
        dto.setHealth(100);
        dto.setWorkshopId(testWorkshop.getId());

        mockMvc.perform(post("/api/equipments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ADMIN: POST /api/workshops - создание цеха разрешено")
    public void testAdminCanCreateWorkshop() throws Exception {
        WorkshopRequestDTO dto = new WorkshopRequestDTO();
        dto.setName("Новый цех");
        dto.setCapacity(50);
        dto.setVisitDurationMinutes(30);

        mockMvc.perform(post("/api/workshops")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ADMIN: POST /api/tickets/generate - генерация билетов разрешена")
    public void testAdminCanGenerateTickets() throws Exception {
        GenerateTicketsRequestDTO dto = new GenerateTicketsRequestDTO();
        dto.setCount(5);

        mockMvc.perform(post("/api/tickets/generate")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ADMIN: GET /api/reports/statistics - просмотр статистики разрешен")
    public void testAdminCanViewStatistics() throws Exception {
        mockMvc.perform(get("/api/reports/statistics")
                .param("startDate", "2024-01-01T00:00:00")
                .param("endDate", "2024-12-31T23:59:59")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN)))
                .andExpect(status().isOk());
    }

    // ==================== FOREMAN: расширенные права ====================

    @Test
    @DisplayName("FOREMAN: POST /api/users - создание пользователя разрешено")
    public void testForemanCanCreateUser() throws Exception {
        UserRequestDTO dto = new UserRequestDTO();
        dto.setUsername("new_user_by_foreman");
        dto.setPassword("password123");
        dto.setRole(Role.WORKER);

        mockMvc.perform(post("/api/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.FOREMAN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("FOREMAN: POST /api/tasks - создание задачи разрешено")
    public void testForemanCanCreateTask() throws Exception {
        TaskRequestDTO dto = new TaskRequestDTO();
        dto.setName("Задача от мастера");
        dto.setStatus(TaskStatus.NOT_ASSIGNED);

        mockMvc.perform(post("/api/tasks")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.FOREMAN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("FOREMAN: POST /api/equipments - создание оборудования разрешено")
    public void testForemanCanCreateEquipment() throws Exception {
        EquipmentRequestDTO dto = new EquipmentRequestDTO();
        dto.setName("Оборудование");
        dto.setStatus(EquipmentStatus.WORKING);
        dto.setHealth(100);
        dto.setWorkshopId(testWorkshop.getId());

        mockMvc.perform(post("/api/equipments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.FOREMAN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("FOREMAN: POST /api/workshops - создание цеха разрешено")
    public void testForemanCanCreateWorkshop() throws Exception {
        WorkshopRequestDTO dto = new WorkshopRequestDTO();
        dto.setName("Цех от мастера");
        dto.setCapacity(40);

        mockMvc.perform(post("/api/workshops")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.FOREMAN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("FOREMAN: GET /api/reports/statistics - просмотр статистики разрешен")
    public void testForemanCanViewStatistics() throws Exception {
        mockMvc.perform(get("/api/reports/statistics")
                .param("startDate", "2024-01-01T00:00:00")
                .param("endDate", "2024-12-31T23:59:59")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.FOREMAN)))
                .andExpect(status().isOk());
    }

    // ==================== MASTER: специфичные права ====================

    @Test
    @DisplayName("MASTER: POST /api/equipments - создание оборудования разрешено")
    public void testMasterCanCreateEquipment() throws Exception {
        EquipmentRequestDTO dto = new EquipmentRequestDTO();
        dto.setName("Оборудование от мастера");
        dto.setStatus(EquipmentStatus.WORKING);
        dto.setHealth(100);
        dto.setWorkshopId(testWorkshop.getId());

        mockMvc.perform(post("/api/equipments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.MASTER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("MASTER: GET /api/equipments - просмотр оборудования разрешен")
    public void testMasterCanViewEquipment() throws Exception {
        mockMvc.perform(get("/api/equipments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.MASTER)))
                .andExpect(status().isOk());
    }

    // ==================== GUIDE: специфичные права для экскурсий ====================

    @Test
    @DisplayName("GUIDE: POST /api/excursions - создание экскурсии разрешено")
    public void testGuideCanCreateExcursion() throws Exception {
        User guide = getUserForRole(Role.GUIDE);
        
        ExcursionRequestDTO dto = new ExcursionRequestDTO();
        dto.setName("Экскурсия от гида");
        dto.setStartTime(java.time.LocalDateTime.now().plusDays(1));
        dto.setParticipantsCount(20);
        dto.setGuideId(guide.getId());
        dto.setStatus(ExcursionStatus.DRAFT);
        dto.setAutoGenerateRoute(true);

        mockMvc.perform(post("/api/excursions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.GUIDE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GUIDE: GET /api/tickets - просмотр билетов разрешен")
    public void testGuideCanViewTickets() throws Exception {
        mockMvc.perform(get("/api/tickets")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.GUIDE)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GUIDE: GET /api/reports/statistics - просмотр статистики запрещен")
    public void testGuideCannotViewStatistics() throws Exception {
        mockMvc.perform(get("/api/reports/statistics")
                .param("startDate", "2024-01-01T00:00:00")
                .param("endDate", "2024-12-31T23:59:59")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.GUIDE)))
                .andExpect(status().isForbidden());
    }

    // ==================== WORKER: ограниченные права ====================

    @Test
    @DisplayName("WORKER: GET /api/tasks - просмотр задач разрешен")
    public void testWorkerCanViewTasks() throws Exception {
        mockMvc.perform(get("/api/tasks")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.WORKER)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("WORKER: GET /api/equipments - просмотр оборудования разрешен")
    public void testWorkerCanViewEquipment() throws Exception {
        mockMvc.perform(get("/api/equipments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.WORKER)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("WORKER: GET /api/workshops - просмотр цехов разрешен")
    public void testWorkerCanViewWorkshops() throws Exception {
        mockMvc.perform(get("/api/workshops")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.WORKER)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("WORKER: POST /api/users - создание пользователя запрещено")
    public void testWorkerCannotCreateUser() throws Exception {
        UserRequestDTO dto = new UserRequestDTO();
        dto.setUsername("new_user");
        dto.setPassword("password123");
        dto.setRole(Role.WORKER);

        mockMvc.perform(post("/api/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.WORKER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("WORKER: DELETE /api/users/{id} - удаление пользователя запрещено")
    public void testWorkerCannotDeleteUser() throws Exception {
        User admin = getUserForRole(Role.ADMIN);

        mockMvc.perform(delete("/api/users/" + admin.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.WORKER)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("WORKER: POST /api/tasks - создание задачи запрещено")
    public void testWorkerCannotCreateTask() throws Exception {
        TaskRequestDTO dto = new TaskRequestDTO();
        dto.setName("Задача");
        dto.setStatus(TaskStatus.NOT_ASSIGNED);

        mockMvc.perform(post("/api/tasks")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.WORKER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("WORKER: DELETE /api/tasks/{id} - удаление задачи запрещено")
    public void testWorkerCannotDeleteTask() throws Exception {
        Task task = new Task();
        task.setName("Задача");
        task.setStatus(TaskStatus.NOT_ASSIGNED);
        Task savedTask = taskRepository.save(task);

        mockMvc.perform(delete("/api/tasks/" + savedTask.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.WORKER)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("WORKER: POST /api/equipments - создание оборудования запрещено")
    public void testWorkerCannotCreateEquipment() throws Exception {
        EquipmentRequestDTO dto = new EquipmentRequestDTO();
        dto.setName("Оборудование");
        dto.setStatus(EquipmentStatus.WORKING);
        dto.setHealth(100);
        dto.setWorkshopId(testWorkshop.getId());

        mockMvc.perform(post("/api/equipments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.WORKER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("WORKER: DELETE /api/equipments/{id} - удаление оборудования запрещено")
    public void testWorkerCannotDeleteEquipment() throws Exception {
        Equipment equipment = new Equipment();
        equipment.setName("Оборудование");
        equipment.setStatus(EquipmentStatus.WORKING);
        equipment.setHealth(100);
        equipment.setWorkshop(testWorkshop);
        Equipment savedEquipment = equipmentRepository.save(equipment);

        mockMvc.perform(delete("/api/equipments/" + savedEquipment.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.WORKER)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("WORKER: POST /api/workshops - создание цеха запрещено")
    public void testWorkerCannotCreateWorkshop() throws Exception {
        WorkshopRequestDTO dto = new WorkshopRequestDTO();
        dto.setName("Цех");
        dto.setCapacity(50);

        mockMvc.perform(post("/api/workshops")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.WORKER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("WORKER: DELETE /api/workshops/{id} - удаление цеха запрещено")
    public void testWorkerCannotDeleteWorkshop() throws Exception {
        mockMvc.perform(delete("/api/workshops/" + testWorkshop.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.WORKER)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("WORKER: POST /api/excursions - создание экскурсии запрещено")
    public void testWorkerCannotCreateExcursion() throws Exception {
        User guide = getUserForRole(Role.GUIDE);
        
        ExcursionRequestDTO dto = new ExcursionRequestDTO();
        dto.setName("Экскурсия");
        dto.setStartTime(java.time.LocalDateTime.now().plusDays(1));
        dto.setParticipantsCount(20);
        dto.setGuideId(guide.getId());
        dto.setAutoGenerateRoute(true);

        mockMvc.perform(post("/api/excursions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.WORKER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("WORKER: DELETE /api/excursions/{id} - удаление экскурсии запрещено")
    public void testWorkerCannotDeleteExcursion() throws Exception {
        User guide = getUserForRole(Role.GUIDE);
        
        Excursion excursion = new Excursion();
        excursion.setName("Экскурсия");
        excursion.setStartTime(java.time.LocalDateTime.now().plusDays(1));
        excursion.setParticipantsCount(20);
        excursion.setGuide(guide);
        excursion.setStatus(ExcursionStatus.DRAFT);
        Excursion savedExcursion = excursionRepository.save(excursion);

        mockMvc.perform(delete("/api/excursions/" + savedExcursion.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.WORKER)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("WORKER: POST /api/tickets/generate - генерация билетов запрещена")
    public void testWorkerCannotGenerateTickets() throws Exception {
        GenerateTicketsRequestDTO dto = new GenerateTicketsRequestDTO();
        dto.setCount(5);

        mockMvc.perform(post("/api/tickets/generate")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.WORKER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("WORKER: GET /api/reports/statistics - просмотр статистики запрещен")
    public void testWorkerCannotViewStatistics() throws Exception {
        mockMvc.perform(get("/api/reports/statistics")
                .param("startDate", "2024-01-01T00:00:00")
                .param("endDate", "2024-12-31T23:59:59")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.WORKER)))
                .andExpect(status().isForbidden());
    }
}

