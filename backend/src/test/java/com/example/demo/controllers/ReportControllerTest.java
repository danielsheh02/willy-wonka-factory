package com.example.demo.controllers;

import com.example.demo.BaseTest;
import com.example.demo.models.*;
import com.example.demo.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@DisplayName("Функциональные тесты для ReportController")
public class ReportControllerTest extends BaseTest {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ExcursionRepository excursionRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private WorkshopRepository workshopRepository;

    @Override
    @BeforeEach
    public void setupBaseTest() {
        super.setupBaseTest();
        taskRepository.deleteAll();
        excursionRepository.deleteAll();
        equipmentRepository.deleteAll();
        workshopRepository.deleteAll();
    }

    @Test
    @DisplayName("Получение статистики ADMIN'ом")
    public void testGetStatisticsAsAdmin() throws Exception {
        // Создаем тестовые данные
        createTestData();

        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);

        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

        mockMvc.perform(get("/api/reports/statistics")
                .param("startDate", startDate.format(formatter))
                .param("endDate", endDate.format(formatter))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()));
    }

    @Test
    @DisplayName("Получение статистики FOREMAN'ом")
    public void testGetStatisticsAsForeman() throws Exception {
        createTestData();

        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);

        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

        mockMvc.perform(get("/api/reports/statistics")
                .param("startDate", startDate.format(formatter))
                .param("endDate", endDate.format(formatter))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.FOREMAN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()));
    }

    @Test
    @DisplayName("Доступ к статистике WORKER'ом запрещен")
    public void testGetStatisticsAsWorkerForbidden() throws Exception {
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);

        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

        mockMvc.perform(get("/api/reports/statistics")
                .param("startDate", startDate.format(formatter))
                .param("endDate", endDate.format(formatter))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.WORKER)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Доступ к статистике GUIDE'ом запрещен")
    public void testGetStatisticsAsGuideForbidden() throws Exception {
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);

        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

        mockMvc.perform(get("/api/reports/statistics")
                .param("startDate", startDate.format(formatter))
                .param("endDate", endDate.format(formatter))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.GUIDE)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Получение статистики за разные периоды")
    public void testGetStatisticsForDifferentPeriods() throws Exception {
        createTestData();

        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

        // За последний день
        LocalDateTime startDate1 = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate1 = LocalDateTime.now();

        mockMvc.perform(get("/api/reports/statistics")
                .param("startDate", startDate1.format(formatter))
                .param("endDate", endDate1.format(formatter))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()));

        // За последнюю неделю
        LocalDateTime startDate2 = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate2 = LocalDateTime.now();

        mockMvc.perform(get("/api/reports/statistics")
                .param("startDate", startDate2.format(formatter))
                .param("endDate", endDate2.format(formatter))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()));

        // За последний месяц
        LocalDateTime startDate3 = LocalDateTime.now().minusDays(30);
        LocalDateTime endDate3 = LocalDateTime.now();

        mockMvc.perform(get("/api/reports/statistics")
                .param("startDate", startDate3.format(formatter))
                .param("endDate", endDate3.format(formatter))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()));
    }

    @Test
    @DisplayName("Получение статистики без авторизации запрещено")
    public void testGetStatisticsWithoutAuthForbidden() throws Exception {
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();

        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

        mockMvc.perform(get("/api/reports/statistics")
                .param("startDate", startDate.format(formatter))
                .param("endDate", endDate.format(formatter)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Получение статистики с некорректными датами")
    public void testGetStatisticsWithInvalidDates() throws Exception {

        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = LocalDateTime.now().minusDays(7);

        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

        mockMvc.perform(get("/api/reports/statistics")
                .param("startDate", startDate.format(formatter))
                .param("endDate", endDate.format(formatter))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Получение статистики с задачами разных статусов")
    public void testGetStatisticsWithDifferentTaskStatuses() throws Exception {
        User worker = getUserForRole(Role.WORKER);

        // Создаем задачи с разными статусами
        Task completedTask = new Task();
        completedTask.setName("Завершенная задача");
        completedTask.setStatus(TaskStatus.COMPLETED);
        completedTask.setUser(worker);
        completedTask.setCompletedAt(LocalDateTime.now());
        taskRepository.save(completedTask);

        Task inProgressTask = new Task();
        inProgressTask.setName("Задача в процессе");
        inProgressTask.setStatus(TaskStatus.IN_PROGRESS);
        inProgressTask.setUser(worker);
        taskRepository.save(inProgressTask);

        Task notAssignedTask = new Task();
        notAssignedTask.setName("Не назначенная задача");
        notAssignedTask.setStatus(TaskStatus.NOT_ASSIGNED);
        taskRepository.save(notAssignedTask);

        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);

        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

        mockMvc.perform(get("/api/reports/statistics")
                .param("startDate", startDate.format(formatter))
                .param("endDate", endDate.format(formatter))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()));
    }

    @Test
    @DisplayName("Получение статистики с экскурсиями")
    public void testGetStatisticsWithExcursions() throws Exception {
        User guide = getUserForRole(Role.GUIDE);

        // Создаем экскурсии
        Excursion excursion1 = new Excursion();
        excursion1.setName("Экскурсия 1");
        excursion1.setStartTime(LocalDateTime.now().minusDays(2));
        excursion1.setParticipantsCount(15);
        excursion1.setGuide(guide);
        excursion1.setStatus(ExcursionStatus.COMPLETED);
        excursionRepository.save(excursion1);

        Excursion excursion2 = new Excursion();
        excursion2.setName("Экскурсия 2");
        excursion2.setStartTime(LocalDateTime.now().plusDays(1));
        excursion2.setParticipantsCount(20);
        excursion2.setGuide(guide);
        excursion2.setStatus(ExcursionStatus.CONFIRMED);
        excursionRepository.save(excursion2);

        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now().plusDays(2);

        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

        mockMvc.perform(get("/api/reports/statistics")
                .param("startDate", startDate.format(formatter))
                .param("endDate", endDate.format(formatter))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()));
    }

    private void createTestData() {
        User worker = getUserForRole(Role.WORKER);
        User guide = getUserForRole(Role.GUIDE);

        // Создаем задачи
        for (int i = 0; i < 3; i++) {
            Task task = new Task();
            task.setName("Задача " + i);
            task.setDescription("Описание " + i);
            task.setStatus(TaskStatus.IN_PROGRESS);
            task.setUser(worker);
            taskRepository.save(task);
        }

        // Создаем экскурсию
        Excursion excursion = new Excursion();
        excursion.setName("Тестовая экскурсия");
        excursion.setStartTime(LocalDateTime.now().minusDays(1));
        excursion.setParticipantsCount(20);
        excursion.setGuide(guide);
        excursion.setStatus(ExcursionStatus.COMPLETED);
        excursionRepository.save(excursion);

        // Создаем цех и оборудование
        Workshop workshop = new Workshop();
        workshop.setName("Тестовый цех");
        workshop.setCapacity(50);
        workshop.setVisitDurationMinutes(30);
        Workshop savedWorkshop = workshopRepository.save(workshop);

        Equipment equipment = new Equipment();
        equipment.setName("Тестовое оборудование");
        equipment.setModel("TEST-100");
        equipment.setStatus(EquipmentStatus.WORKING);
        equipment.setHealth(85);
        equipment.setWorkshop(savedWorkshop);
        equipmentRepository.save(equipment);
    }
}

