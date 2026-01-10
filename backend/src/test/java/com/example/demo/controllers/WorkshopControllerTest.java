package com.example.demo.controllers;

import com.example.demo.BaseTest;
import com.example.demo.dto.request.WorkshopRequestDTO;
import com.example.demo.models.Role;
import com.example.demo.models.Workshop;
import com.example.demo.repositories.WorkshopRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.HashSet;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@DisplayName("Интеграционные тесты для WorkshopController")
public class WorkshopControllerTest extends BaseTest {

    @Autowired
    private WorkshopRepository workshopRepository;

    @Override
    @BeforeEach
    public void setupBaseTest() {
        super.setupBaseTest();
        workshopRepository.deleteAll();
    }

    @Test
    @DisplayName("Создание цеха ADMIN'ом")
    public void testCreateWorkshopAsAdmin() throws Exception {
        WorkshopRequestDTO dto = new WorkshopRequestDTO();
        dto.setName("Шоколадный цех");
        dto.setDescription("Производство шоколада");
        dto.setCapacity(50);
        dto.setVisitDurationMinutes(30);
        dto.setForemanIds(new HashSet<>());
        dto.setEquipmentIds(new HashSet<>());

        mockMvc.perform(post("/api/workshops")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Шоколадный цех")))
                .andExpect(jsonPath("$.description", is("Производство шоколада")))
                .andExpect(jsonPath("$.capacity", is(50)))
                .andExpect(jsonPath("$.visitDurationMinutes", is(30)));
    }

    @Test
    @DisplayName("Создание цеха FOREMAN'ом")
    public void testCreateWorkshopAsForeman() throws Exception {
        WorkshopRequestDTO dto = new WorkshopRequestDTO();
        dto.setName("Кондитерский цех");
        dto.setDescription("Производство конфет");
        dto.setCapacity(30);
        dto.setVisitDurationMinutes(25);
        dto.setForemanIds(new HashSet<>());
        dto.setEquipmentIds(new HashSet<>());

        mockMvc.perform(post("/api/workshops")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.FOREMAN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Кондитерский цех")));
    }

    @Test
    @DisplayName("Создание цеха WORKER'ом запрещено")
    public void testCreateWorkshopAsWorkerForbidden() throws Exception {
        WorkshopRequestDTO dto = new WorkshopRequestDTO();
        dto.setName("Недопустимый цех");
        dto.setDescription("Не должно создаться");
        dto.setForemanIds(new HashSet<>());
        dto.setEquipmentIds(new HashSet<>());

        mockMvc.perform(post("/api/workshops")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.WORKER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Получение всех цехов")
    public void testGetAllWorkshops() throws Exception {
        Workshop workshop = new Workshop();
        workshop.setName("Тестовый цех");
        workshop.setDescription("Описание");
        workshop.setCapacity(40);
        workshop.setVisitDurationMinutes(20);
        workshopRepository.save(workshop);

        mockMvc.perform(get("/api/workshops")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("Получение цеха по ID")
    public void testGetWorkshopById() throws Exception {
        Workshop workshop = new Workshop();
        workshop.setName("Конкретный цех");
        workshop.setDescription("Описание цеха");
        workshop.setCapacity(60);
        workshop.setVisitDurationMinutes(35);
        Workshop savedWorkshop = workshopRepository.save(workshop);

        mockMvc.perform(get("/api/workshops/" + savedWorkshop.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Конкретный цех")))
                .andExpect(jsonPath("$.capacity", is(60)));
    }

    @Test
    @DisplayName("Получение несуществующего цеха")
    public void testGetNonExistentWorkshop() throws Exception {
        mockMvc.perform(get("/api/workshops/99999")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Обновление цеха")
    public void testUpdateWorkshop() throws Exception {
        Workshop workshop = new Workshop();
        workshop.setName("Старое название");
        workshop.setDescription("Старое описание");
        workshop.setCapacity(20);
        workshop.setVisitDurationMinutes(15);
        Workshop savedWorkshop = workshopRepository.save(workshop);

        WorkshopRequestDTO dto = new WorkshopRequestDTO();
        dto.setName("Новое название");
        dto.setDescription("Новое описание");
        dto.setCapacity(80);
        dto.setVisitDurationMinutes(40);
        dto.setForemanIds(new HashSet<>());
        dto.setEquipmentIds(new HashSet<>());

        mockMvc.perform(put("/api/workshops/" + savedWorkshop.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Новое название")))
                .andExpect(jsonPath("$.description", is("Новое описание")))
                .andExpect(jsonPath("$.capacity", is(80)));
    }

    @Test
    @DisplayName("Обновление несуществующего цеха")
    public void testUpdateNonExistentWorkshop() throws Exception {
        WorkshopRequestDTO dto = new WorkshopRequestDTO();
        dto.setName("Обновленный цех");
        dto.setDescription("Описание");
        dto.setForemanIds(new HashSet<>());
        dto.setEquipmentIds(new HashSet<>());

        mockMvc.perform(put("/api/workshops/99999")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Удаление цеха ADMIN'ом")
    public void testDeleteWorkshopAsAdmin() throws Exception {
        Workshop workshop = new Workshop();
        workshop.setName("Цех для удаления");
        workshop.setDescription("Будет удален");
        Workshop savedWorkshop = workshopRepository.save(workshop);

        mockMvc.perform(delete("/api/workshops/" + savedWorkshop.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/workshops/" + savedWorkshop.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Удаление цеха WORKER'ом запрещено")
    public void testDeleteWorkshopAsWorkerForbidden() throws Exception {
        Workshop workshop = new Workshop();
        workshop.setName("Цех");
        workshopRepository.save(workshop);

        mockMvc.perform(delete("/api/workshops/" + workshop.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.WORKER)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Получение цехов с пагинацией")
    public void testGetWorkshopsPaged() throws Exception {
        for (int i = 0; i < 15; i++) {
            Workshop workshop = new Workshop();
            workshop.setName("Цех " + i);
            workshop.setDescription("Описание " + i);
            workshop.setCapacity(50);
            workshop.setVisitDurationMinutes(30);
            workshopRepository.save(workshop);
        }

        mockMvc.perform(get("/api/workshops/paged")
                .param("page", "0")
                .param("size", "10")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(10)));
    }

    @Test
    @DisplayName("Доступ к цехам для WORKER")
    public void testGetWorkshopsAsWorker() throws Exception {
        Workshop workshop = new Workshop();
        workshop.setName("Цех");
        workshopRepository.save(workshop);

        mockMvc.perform(get("/api/workshops")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.WORKER)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Доступ к цехам для GUIDE")
    public void testGetWorkshopsAsGuide() throws Exception {
        Workshop workshop = new Workshop();
        workshop.setName("Цех для экскурсии");
        workshopRepository.save(workshop);

        mockMvc.perform(get("/api/workshops")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.GUIDE)))
                .andExpect(status().isOk());
    }
}

