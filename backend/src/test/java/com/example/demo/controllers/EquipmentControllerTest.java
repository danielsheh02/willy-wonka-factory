package com.example.demo.controllers;

import com.example.demo.BaseTest;
import com.example.demo.dto.request.EquipmentRequestDTO;
import com.example.demo.models.Equipment;
import com.example.demo.models.EquipmentStatus;
import com.example.demo.models.Role;
import com.example.demo.models.Workshop;
import com.example.demo.repositories.EquipmentRepository;
import com.example.demo.repositories.WorkshopRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@DisplayName("Функциональные тесты для EquipmentController")
public class EquipmentControllerTest extends BaseTest {

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private WorkshopRepository workshopRepository;

    private Workshop testWorkshop;

    @Override
    @BeforeEach
    public void setupBaseTest() {
        super.setupBaseTest();
        equipmentRepository.deleteAll();
        workshopRepository.deleteAll();
        
        testWorkshop = new Workshop();
        testWorkshop.setName("Тестовый цех");
        testWorkshop.setDescription("Цех для тестирования");
        testWorkshop.setCapacity(50);
        testWorkshop.setVisitDurationMinutes(30);
        testWorkshop = workshopRepository.save(testWorkshop);
    }

    @Test
    @DisplayName("Создание оборудования ADMIN'ом")
    public void testCreateEquipmentAsAdmin() throws Exception {
        EquipmentRequestDTO dto = new EquipmentRequestDTO();
        dto.setName("Конвейер");
        dto.setDescription("Шоколадный конвейер");
        dto.setModel("CV-2000");
        dto.setStatus(EquipmentStatus.WORKING);
        dto.setHealth(100);
        dto.setTemperature(25.5);
        dto.setLastServicedAt(LocalDate.now());
        dto.setWorkshopId(testWorkshop.getId());

        mockMvc.perform(post("/api/equipments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Конвейер")))
                .andExpect(jsonPath("$.model", is("CV-2000")))
                .andExpect(jsonPath("$.status", is("WORKING")))
                .andExpect(jsonPath("$.health", is(100)));
    }

    @Test
    @DisplayName("Создание оборудования FOREMAN'ом")
    public void testCreateEquipmentAsForeman() throws Exception {
        EquipmentRequestDTO dto = new EquipmentRequestDTO();
        dto.setName("Миксер");
        dto.setDescription("Промышленный миксер");
        dto.setModel("MX-500");
        dto.setStatus(EquipmentStatus.WORKING);
        dto.setHealth(95);
        dto.setWorkshopId(testWorkshop.getId());

        mockMvc.perform(post("/api/equipments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.FOREMAN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Миксер")));
    }

    @Test
    @DisplayName("Создание оборудования MASTER'ом")
    public void testCreateEquipmentAsMaster() throws Exception {
        EquipmentRequestDTO dto = new EquipmentRequestDTO();
        dto.setName("Печь");
        dto.setDescription("Промышленная печь");
        dto.setModel("OV-1000");
        dto.setStatus(EquipmentStatus.WORKING);
        dto.setHealth(90);
        dto.setWorkshopId(testWorkshop.getId());

        mockMvc.perform(post("/api/equipments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.MASTER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Печь")));
    }

    @Test
    @DisplayName("Создание оборудования WORKER'ом запрещено")
    public void testCreateEquipmentAsWorkerForbidden() throws Exception {
        EquipmentRequestDTO dto = new EquipmentRequestDTO();
        dto.setName("Недопустимое оборудование");
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
    @DisplayName("Получение всего оборудования")
    public void testGetAllEquipment() throws Exception {
        Equipment equipment = new Equipment();
        equipment.setName("Тестовое оборудование");
        equipment.setDescription("Описание");
        equipment.setModel("TEST-100");
        equipment.setStatus(EquipmentStatus.WORKING);
        equipment.setHealth(85);
        equipment.setWorkshop(testWorkshop);
        equipmentRepository.save(equipment);

        mockMvc.perform(get("/api/equipments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("Получение оборудования по ID")
    public void testGetEquipmentById() throws Exception {
        Equipment equipment = new Equipment();
        equipment.setName("Конкретное оборудование");
        equipment.setDescription("Описание оборудования");
        equipment.setModel("EQ-200");
        equipment.setStatus(EquipmentStatus.WORKING);
        equipment.setHealth(75);
        equipment.setWorkshop(testWorkshop);
        Equipment savedEquipment = equipmentRepository.save(equipment);

        mockMvc.perform(get("/api/equipments/" + savedEquipment.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Конкретное оборудование")))
                .andExpect(jsonPath("$.model", is("EQ-200")))
                .andExpect(jsonPath("$.health", is(75)));
    }

    @Test
    @DisplayName("Получение несуществующего оборудования")
    public void testGetNonExistentEquipment() throws Exception {
        mockMvc.perform(get("/api/equipments/99999")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Обновление оборудования")
    public void testUpdateEquipment() throws Exception {
        Equipment equipment = new Equipment();
        equipment.setName("Старое название");
        equipment.setDescription("Старое описание");
        equipment.setModel("OLD-100");
        equipment.setStatus(EquipmentStatus.WORKING);
        equipment.setHealth(50);
        equipment.setWorkshop(testWorkshop);
        Equipment savedEquipment = equipmentRepository.save(equipment);

        EquipmentRequestDTO dto = new EquipmentRequestDTO();
        dto.setName("Новое название");
        dto.setDescription("Новое описание");
        dto.setModel("NEW-200");
        dto.setStatus(EquipmentStatus.UNDER_REPAIR);
        dto.setHealth(30);
        dto.setWorkshopId(testWorkshop.getId());

        mockMvc.perform(put("/api/equipments/" + savedEquipment.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Новое название")))
                .andExpect(jsonPath("$.status", is("UNDER_REPAIR")))
                .andExpect(jsonPath("$.health", is(30)));
    }

    @Test
    @DisplayName("Обновление несуществующего оборудования")
    public void testUpdateNonExistentEquipment() throws Exception {
        EquipmentRequestDTO dto = new EquipmentRequestDTO();
        dto.setName("Обновленное");
        dto.setStatus(EquipmentStatus.WORKING);
        dto.setHealth(100);
        dto.setWorkshopId(testWorkshop.getId());

        mockMvc.perform(put("/api/equipments/99999")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Удаление оборудования ADMIN'ом")
    public void testDeleteEquipmentAsAdmin() throws Exception {
        Equipment equipment = new Equipment();
        equipment.setName("Оборудование для удаления");
        equipment.setStatus(EquipmentStatus.BROKEN);
        equipment.setHealth(0);
        equipment.setWorkshop(testWorkshop);
        Equipment savedEquipment = equipmentRepository.save(equipment);

        mockMvc.perform(delete("/api/equipments/" + savedEquipment.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/equipments/" + savedEquipment.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Удаление оборудования WORKER'ом запрещено")
    public void testDeleteEquipmentAsWorkerForbidden() throws Exception {
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
    @DisplayName("Получение оборудования с пагинацией")
    public void testGetEquipmentPaged() throws Exception {
        for (int i = 0; i < 15; i++) {
            Equipment equipment = new Equipment();
            equipment.setName("Оборудование " + i);
            equipment.setModel("MODEL-" + i);
            equipment.setStatus(EquipmentStatus.WORKING);
            equipment.setHealth(80 + i % 20);
            equipment.setWorkshop(testWorkshop);
            equipmentRepository.save(equipment);
        }

        mockMvc.perform(get("/api/equipments/paged")
                .param("page", "0")
                .param("size", "10")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(10)));
    }

    @Test
    @DisplayName("Получение статусов оборудования")
    public void testGetEquipmentStatuses() throws Exception {
        mockMvc.perform(get("/api/equipments/statuses")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0]", isOneOf("WORKING", "UNDER_REPAIR", "BROKEN")));
    }

    @Test
    @DisplayName("Доступ к оборудованию для WORKER")
    public void testGetEquipmentAsWorker() throws Exception {
        Equipment equipment = new Equipment();
        equipment.setName("Оборудование");
        equipment.setStatus(EquipmentStatus.WORKING);
        equipment.setHealth(100);
        equipment.setWorkshop(testWorkshop);
        equipmentRepository.save(equipment);

        mockMvc.perform(get("/api/equipments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.WORKER)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Доступ к оборудованию для GUIDE")
    public void testGetEquipmentAsGuide() throws Exception {
        Equipment equipment = new Equipment();
        equipment.setName("Оборудование для экскурсии");
        equipment.setStatus(EquipmentStatus.WORKING);
        equipment.setHealth(100);
        equipment.setWorkshop(testWorkshop);
        equipmentRepository.save(equipment);

        mockMvc.perform(get("/api/equipments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.GUIDE)))
                .andExpect(status().isOk());
    }
}

