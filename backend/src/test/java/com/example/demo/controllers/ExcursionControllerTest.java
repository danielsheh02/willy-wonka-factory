package com.example.demo.controllers;

import com.example.demo.BaseTest;
import com.example.demo.dto.request.ExcursionRequestDTO;
import com.example.demo.models.*;
import com.example.demo.repositories.ExcursionRepository;
import com.example.demo.repositories.WorkshopRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@DisplayName("Функциональные тесты для ExcursionController")
public class ExcursionControllerTest extends BaseTest {

    @Autowired
    private ExcursionRepository excursionRepository;

    @Autowired
    private WorkshopRepository workshopRepository;

    private Workshop testWorkshop;

    @Override
    @BeforeEach
    public void setupBaseTest() {
        super.setupBaseTest();
        excursionRepository.deleteAll();
        workshopRepository.deleteAll();
        
        testWorkshop = new Workshop();
        testWorkshop.setName("Шоколадный цех");
        testWorkshop.setDescription("Производство шоколада");
        testWorkshop.setCapacity(50);
        testWorkshop.setVisitDurationMinutes(30);
        testWorkshop = workshopRepository.save(testWorkshop);
    }

    @Test
    @DisplayName("Создание экскурсии ADMIN'ом с автогенерацией маршрута")
    public void testCreateExcursionAsAdminWithAutoRoute() throws Exception {
        User guide = getUserForRole(Role.GUIDE);
        
        ExcursionRequestDTO dto = new ExcursionRequestDTO();
        dto.setName("Экскурсия по фабрике");
        dto.setStartTime(LocalDateTime.now().plusDays(1));
        dto.setParticipantsCount(20);
        dto.setGuideId(guide.getId());
        dto.setStatus(ExcursionStatus.DRAFT);
        dto.setAutoGenerateRoute(true);

        mockMvc.perform(post("/api/excursions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Экскурсия по фабрике")))
                .andExpect(jsonPath("$.participantsCount", is(20)))
                .andExpect(jsonPath("$.status", is("DRAFT")));
    }

    @Test
    @DisplayName("Создание экскурсии GUIDE'ом")
    public void testCreateExcursionAsGuide() throws Exception {
        User guide = getUserForRole(Role.GUIDE);
        
        ExcursionRequestDTO dto = new ExcursionRequestDTO();
        dto.setName("Утренняя экскурсия");
        dto.setStartTime(LocalDateTime.now().plusDays(2));
        dto.setParticipantsCount(15);
        dto.setGuideId(guide.getId());
        dto.setStatus(ExcursionStatus.DRAFT);
        dto.setAutoGenerateRoute(true);

        mockMvc.perform(post("/api/excursions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.GUIDE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Утренняя экскурсия")));
    }

    @Test
    @DisplayName("Создание экскурсии с ручным маршрутом")
    public void testCreateExcursionWithManualRoute() throws Exception {
        User guide = getUserForRole(Role.GUIDE);
        
        List<ExcursionRequestDTO.RoutePointDTO> routes = new ArrayList<>();
        ExcursionRequestDTO.RoutePointDTO point = new ExcursionRequestDTO.RoutePointDTO();
        point.setWorkshopId(testWorkshop.getId());
        point.setOrderNumber(1);
        point.setDurationMinutes(30);
        routes.add(point);

        ExcursionRequestDTO dto = new ExcursionRequestDTO();
        dto.setName("Кастомная экскурсия");
        dto.setStartTime(LocalDateTime.now().plusDays(3));
        dto.setParticipantsCount(25);
        dto.setGuideId(guide.getId());
        dto.setStatus(ExcursionStatus.DRAFT);
        dto.setAutoGenerateRoute(false);
        dto.setRoutes(routes);

        mockMvc.perform(post("/api/excursions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Кастомная экскурсия")));
    }

    @Test
    @DisplayName("Создание экскурсии WORKER'ом запрещено")
    public void testCreateExcursionAsWorkerForbidden() throws Exception {
        User guide = getUserForRole(Role.GUIDE);
        
        ExcursionRequestDTO dto = new ExcursionRequestDTO();
        dto.setName("Недопустимая экскурсия");
        dto.setStartTime(LocalDateTime.now().plusDays(1));
        dto.setParticipantsCount(10);
        dto.setGuideId(guide.getId());
        dto.setAutoGenerateRoute(true);

        mockMvc.perform(post("/api/excursions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.WORKER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Получение всех экскурсий (публичный доступ)")
    public void testGetAllExcursions() throws Exception {
        User guide = getUserForRole(Role.GUIDE);
        
        Excursion excursion = new Excursion();
        excursion.setName("Тестовая экскурсия");
        excursion.setStartTime(LocalDateTime.now().plusDays(1));
        excursion.setParticipantsCount(20);
        excursion.setGuide(guide);
        excursion.setStatus(ExcursionStatus.CONFIRMED);
        excursionRepository.save(excursion);
        
        mockMvc.perform(get("/api/excursions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("Получение экскурсии по ID")
    public void testGetExcursionById() throws Exception {
        User guide = getUserForRole(Role.GUIDE);
        
        Excursion excursion = new Excursion();
        excursion.setName("Конкретная экскурсия");
        excursion.setStartTime(LocalDateTime.now().plusDays(2));
        excursion.setParticipantsCount(30);
        excursion.setGuide(guide);
        excursion.setStatus(ExcursionStatus.CONFIRMED);
        Excursion savedExcursion = excursionRepository.save(excursion);

        mockMvc.perform(get("/api/excursions/" + savedExcursion.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Конкретная экскурсия")))
                .andExpect(jsonPath("$.participantsCount", is(30)))
                .andExpect(jsonPath("$.status", is("CONFIRMED")));
    }

    @Test
    @DisplayName("Получение несуществующей экскурсии")
    public void testGetNonExistentExcursion() throws Exception {
        mockMvc.perform(get("/api/excursions/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Получение экскурсий по гиду")
    public void testGetExcursionsByGuide() throws Exception {
        User guide = getUserForRole(Role.GUIDE);
        
        Excursion excursion1 = new Excursion();
        excursion1.setName("Экскурсия 1");
        excursion1.setStartTime(LocalDateTime.now().plusDays(1));
        excursion1.setParticipantsCount(15);
        excursion1.setGuide(guide);
        excursion1.setStatus(ExcursionStatus.CONFIRMED);
        excursionRepository.save(excursion1);

        Excursion excursion2 = new Excursion();
        excursion2.setName("Экскурсия 2");
        excursion2.setStartTime(LocalDateTime.now().plusDays(2));
        excursion2.setParticipantsCount(20);
        excursion2.setGuide(guide);
        excursion2.setStatus(ExcursionStatus.DRAFT);
        excursionRepository.save(excursion2);

        mockMvc.perform(get("/api/excursions/guide/" + guide.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("Обновление экскурсии")
    public void testUpdateExcursion() throws Exception {
        User guide = getUserForRole(Role.GUIDE);
        
        Excursion excursion = new Excursion();
        excursion.setName("Старое название");
        excursion.setStartTime(LocalDateTime.now().plusDays(1));
        excursion.setParticipantsCount(10);
        excursion.setGuide(guide);
        excursion.setStatus(ExcursionStatus.DRAFT);
        Excursion savedExcursion = excursionRepository.save(excursion);

        ExcursionRequestDTO dto = new ExcursionRequestDTO();
        dto.setName("Новое название");
        dto.setStartTime(LocalDateTime.now().plusDays(3));
        dto.setParticipantsCount(25);
        dto.setGuideId(guide.getId());
        dto.setStatus(ExcursionStatus.CONFIRMED);
        dto.setAutoGenerateRoute(true);

        mockMvc.perform(put("/api/excursions/" + savedExcursion.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Новое название")))
                .andExpect(jsonPath("$.participantsCount", is(25)))
                .andExpect(jsonPath("$.status", is("CONFIRMED")));
    }

    @Test
    @DisplayName("Удаление экскурсии")
    public void testDeleteExcursion() throws Exception {
        User guide = getUserForRole(Role.GUIDE);
        
        Excursion excursion = new Excursion();
        excursion.setName("Экскурсия для удаления");
        excursion.setStartTime(LocalDateTime.now().plusDays(1));
        excursion.setParticipantsCount(10);
        excursion.setGuide(guide);
        excursion.setStatus(ExcursionStatus.DRAFT);
        Excursion savedExcursion = excursionRepository.save(excursion);

        mockMvc.perform(delete("/api/excursions/" + savedExcursion.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("удалена")));

        mockMvc.perform(get("/api/excursions/" + savedExcursion.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Получение всех статусов экскурсий")
    public void testGetAllStatuses() throws Exception {
        mockMvc.perform(get("/api/excursions/statuses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0]", isOneOf("DRAFT", "CONFIRMED", "IN_PROGRESS", "COMPLETED", "CANCELLED")));
    }

    @Test
    @DisplayName("Проверка доступности маршрута")
    public void testCheckRouteAvailability() throws Exception {
        User guide = getUserForRole(Role.GUIDE);
        
        ExcursionRequestDTO dto = new ExcursionRequestDTO();
        dto.setStartTime(LocalDateTime.now().plusDays(1));
        dto.setParticipantsCount(20);
        dto.setGuideId(guide.getId());
        dto.setAutoGenerateRoute(true);

        mockMvc.perform(post("/api/excursions/check-availability")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()));
    }

    @Test
    @DisplayName("Удаление экскурсии WORKER'ом запрещено")
    public void testDeleteExcursionAsWorkerForbidden() throws Exception {
        User guide = getUserForRole(Role.GUIDE);
        
        Excursion excursion = new Excursion();
        excursion.setName("Экскурсия");
        excursion.setStartTime(LocalDateTime.now().plusDays(1));
        excursion.setParticipantsCount(10);
        excursion.setGuide(guide);
        excursion.setStatus(ExcursionStatus.DRAFT);
        Excursion savedExcursion = excursionRepository.save(excursion);

        mockMvc.perform(delete("/api/excursions/" + savedExcursion.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.WORKER)))
                .andExpect(status().isForbidden());
    }
}

