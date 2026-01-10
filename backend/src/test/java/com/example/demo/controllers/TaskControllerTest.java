package com.example.demo.controllers;

import com.example.demo.BaseTest;
import com.example.demo.dto.request.TaskFilterRequestDTO;
import com.example.demo.dto.request.TaskRequestDTO;
import com.example.demo.models.Role;
import com.example.demo.models.Task;
import com.example.demo.models.TaskStatus;
import com.example.demo.models.User;
import com.example.demo.repositories.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@DisplayName("Функциональные тесты для TaskController")
public class TaskControllerTest extends BaseTest {

    @Autowired
    private TaskRepository taskRepository;

    @Override
    @BeforeEach
    public void setupBaseTest() {
        super.setupBaseTest();
        taskRepository.deleteAll();
    }

    @Test
    @DisplayName("Создание задачи ADMIN'ом")
    public void testCreateTaskAsAdmin() throws Exception {
        User worker = getUserForRole(Role.WORKER);
        
        TaskRequestDTO dto = new TaskRequestDTO();
        dto.setName("Ремонт оборудования");
        dto.setDescription("Починить конвейер №3");
        dto.setStatus(TaskStatus.IN_PROGRESS);
        dto.setUserId(worker.getId());

        mockMvc.perform(post("/api/tasks")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Ремонт оборудования")))
                .andExpect(jsonPath("$.description", is("Починить конвейер №3")))
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")));
    }

    @Test
    @DisplayName("Создание задачи FOREMAN'ом")
    public void testCreateTaskAsForeman() throws Exception {
        User worker = getUserForRole(Role.WORKER);
        
        TaskRequestDTO dto = new TaskRequestDTO();
        dto.setName("Инспекция цеха");
        dto.setDescription("Проверить состояние оборудования");
        dto.setStatus(TaskStatus.NOT_ASSIGNED);
        dto.setUserId(worker.getId());

        mockMvc.perform(post("/api/tasks")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.FOREMAN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Инспекция цеха")));
    }

    @Test
    @DisplayName("Создание задачи WORKER'ом запрещено")
    public void testCreateTaskAsWorkerForbidden() throws Exception {
        TaskRequestDTO dto = new TaskRequestDTO();
        dto.setName("Задача от рабочего");
        dto.setDescription("Описание");
        dto.setStatus(TaskStatus.NOT_ASSIGNED);

        mockMvc.perform(post("/api/tasks")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.WORKER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Получение всех задач")
    public void testGetAllTasks() throws Exception {
        Task task = new Task();
        task.setName("Тестовая задача");
        task.setDescription("Описание");
        task.setStatus(TaskStatus.NOT_ASSIGNED);
        taskRepository.save(task);

        mockMvc.perform(get("/api/tasks")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("Получение задачи по ID")
    public void testGetTaskById() throws Exception {
        Task task = new Task();
        task.setName("Конкретная задача");
        task.setDescription("Описание задачи");
        task.setStatus(TaskStatus.NOT_ASSIGNED);
        Task savedTask = taskRepository.save(task);

        mockMvc.perform(get("/api/tasks/" + savedTask.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Конкретная задача")))
                .andExpect(jsonPath("$.status", is("NOT_ASSIGNED")));
    }

    @Test
    @DisplayName("Получение несуществующей задачи")
    public void testGetNonExistentTask() throws Exception {
        mockMvc.perform(get("/api/tasks/99999")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Обновление задачи")
    public void testUpdateTask() throws Exception {
        Task task = new Task();
        task.setName("Старое название");
        task.setDescription("Старое описание");
        task.setStatus(TaskStatus.NOT_ASSIGNED);
        Task savedTask = taskRepository.save(task);

        TaskRequestDTO dto = new TaskRequestDTO();
        dto.setName("Новое название");
        dto.setDescription("Новое описание");
        dto.setStatus(TaskStatus.IN_PROGRESS);

        mockMvc.perform(put("/api/tasks/" + savedTask.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Новое название")))
                .andExpect(jsonPath("$.description", is("Новое описание")))
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")));
    }

    @Test
    @DisplayName("Удаление задачи ADMIN'ом")
    public void testDeleteTaskAsAdmin() throws Exception {
        Task task = new Task();
        task.setName("Задача для удаления");
        task.setDescription("Будет удалена");
        task.setStatus(TaskStatus.NOT_ASSIGNED);
        Task savedTask = taskRepository.save(task);

        mockMvc.perform(delete("/api/tasks/" + savedTask.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tasks/" + savedTask.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Удаление задачи WORKER'ом запрещено")
    public void testDeleteTaskAsWorkerForbidden() throws Exception {
        Task task = new Task();
        task.setName("Задача");
        task.setStatus(TaskStatus.NOT_ASSIGNED);
        Task savedTask = taskRepository.save(task);

        mockMvc.perform(delete("/api/tasks/" + savedTask.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.WORKER)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Получение задач с пагинацией")
    public void testGetTasksPaged() throws Exception {
        for (int i = 0; i < 15; i++) {
            Task task = new Task();
            task.setName("Задача " + i);
            task.setStatus(TaskStatus.NOT_ASSIGNED);
            taskRepository.save(task);
        }

        mockMvc.perform(get("/api/tasks/paged")
                .param("page", "0")
                .param("size", "10")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(10)));
    }

    @Test
    @DisplayName("Получение статусов задач")
    public void testGetTaskStatuses() throws Exception {
        mockMvc.perform(get("/api/tasks/statuses")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0]", isOneOf("NOT_ASSIGNED", "IN_PROGRESS", "COMPLETED")));
    }

    @Test
    @DisplayName("Взять задачу себе (assign to me)")
    public void testAssignTaskToMe() throws Exception {
        User worker = getUserForRole(Role.WORKER);
        
        Task task = new Task();
        task.setName("Задача для назначения");
        task.setStatus(TaskStatus.NOT_ASSIGNED);
        Task savedTask = taskRepository.save(task);

        mockMvc.perform(post("/api/tasks/" + savedTask.getId() + "/assign-to-me")
                .param("userId", worker.getId().toString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.WORKER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id", is(worker.getId().intValue())));
    }

    @Test
    @DisplayName("Отказаться от задачи (unassign)")
    public void testUnassignTask() throws Exception {
        User worker = getUserForRole(Role.WORKER);
        
        Task task = new Task();
        task.setName("Задача назначенная");
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setUser(worker);
        Task savedTask = taskRepository.save(task);

        mockMvc.perform(post("/api/tasks/" + savedTask.getId() + "/unassign")
                .param("userId", worker.getId().toString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.WORKER)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Получение нераспределенных задач")
    public void testGetUnassignedTasks() throws Exception {
        Task task1 = new Task();
        task1.setName("Нераспределенная 1");
        task1.setStatus(TaskStatus.NOT_ASSIGNED);
        taskRepository.save(task1);

        Task task2 = new Task();
        task2.setName("Нераспределенная 2");
        task2.setStatus(TaskStatus.NOT_ASSIGNED);
        taskRepository.save(task2);

        User worker = getUserForRole(Role.WORKER);
        Task task3 = new Task();
        task3.setName("Распределенная");
        task3.setStatus(TaskStatus.IN_PROGRESS);
        task3.setUser(worker);
        taskRepository.save(task3);

        mockMvc.perform(get("/api/tasks/unassigned")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("Автоматическое распределение задач")
    public void testDistributeTasks() throws Exception {
        Task task1 = new Task();
        task1.setName("Задача 1");
        task1.setStatus(TaskStatus.NOT_ASSIGNED);
        Task savedTask1 = taskRepository.save(task1);

        Task task2 = new Task();
        task2.setName("Задача 2");
        task2.setStatus(TaskStatus.NOT_ASSIGNED);
        Task savedTask2 = taskRepository.save(task2);

        Map<String, Object> request = new HashMap<>();
        request.put("taskIds", Arrays.asList(savedTask1.getId(), savedTask2.getId()));
        request.put("force", false);

        mockMvc.perform(post("/api/tasks/distribute")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Фильтрация задач с пагинацией")
    public void testFilterTasksPaged() throws Exception {
        User worker = getUserForRole(Role.WORKER);
        
        Task task = new Task();
        task.setName("Задача фильтр");
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setUser(worker);
        taskRepository.save(task);

        TaskFilterRequestDTO filterDTO = new TaskFilterRequestDTO(
            null, null, null, null, null, worker.getId(), TaskStatus.IN_PROGRESS
        );

        mockMvc.perform(post("/api/tasks/filter-paged")
                .param("page", "0")
                .param("size", "10")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(filterDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()));
    }
}

