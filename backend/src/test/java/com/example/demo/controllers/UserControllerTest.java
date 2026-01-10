package com.example.demo.controllers;

import com.example.demo.BaseTest;
import com.example.demo.dto.request.UserRequestDTO;
import com.example.demo.models.Role;
import com.example.demo.models.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@DisplayName("Интеграционные тесты для UserController")
public class UserControllerTest extends BaseTest {

    @Test
    @DisplayName("Получение всех пользователей с правами ADMIN")
    public void testGetAllUsersAsAdmin() throws Exception {
        mockMvc.perform(get("/api/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(5)))); // Минимум 5 пользователей из setup
    }

    @Test
    @DisplayName("Получение всех пользователей с правами FOREMAN")
    public void testGetAllUsersAsForeman() throws Exception {
        mockMvc.perform(get("/api/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.FOREMAN)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Доступ к пользователям без токена запрещен")
    public void testGetAllUsersWithoutToken() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Получение пользователя по ID")
    public void testGetUserById() throws Exception {
        User admin = getUserForRole(Role.ADMIN);
        
        mockMvc.perform(get("/api/users/" + admin.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("admin_user")))
                .andExpect(jsonPath("$.role", is("ADMIN")));
    }

    @Test
    @DisplayName("Получение несуществующего пользователя")
    public void testGetNonExistentUser() throws Exception {
        mockMvc.perform(get("/api/users/99999")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Получение пользователя по username")
    public void testGetUserByUsername() throws Exception {
        mockMvc.perform(get("/api/users/by-username/admin_user")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("admin_user")));
    }

    @Test
    @DisplayName("Создание нового пользователя ADMIN'ом")
    public void testCreateUserAsAdmin() throws Exception {
        UserRequestDTO dto = new UserRequestDTO();
        dto.setUsername("new_worker");
        dto.setPassword("password123");
        dto.setRole(Role.WORKER);

        mockMvc.perform(post("/api/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("new_worker")))
                .andExpect(jsonPath("$.role", is("WORKER")));
    }

    @Test
    @DisplayName("Создание пользователя WORKER'ом запрещено")
    public void testCreateUserAsWorker() throws Exception {
        UserRequestDTO dto = new UserRequestDTO();
        dto.setUsername("another_user");
        dto.setPassword("password123");
        dto.setRole(Role.WORKER);

        mockMvc.perform(post("/api/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.WORKER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Обновление пользователя")
    public void testUpdateUser() throws Exception {
        User worker = getUserForRole(Role.WORKER);
        
        UserRequestDTO dto = new UserRequestDTO();
        dto.setUsername("updated_worker");
        dto.setPassword("newpassword");
        dto.setRole(Role.WORKER);

        mockMvc.perform(put("/api/users/" + worker.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("updated_worker")));
    }

    @Test
    @DisplayName("Обновление несуществующего пользователя")
    public void testUpdateNonExistentUser() throws Exception {
        UserRequestDTO dto = new UserRequestDTO();
        dto.setUsername("updated_user");
        dto.setPassword("newpassword");
        dto.setRole(Role.WORKER);

        mockMvc.perform(put("/api/users/99999")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Удаление пользователя ADMIN'ом")
    public void testDeleteUserAsAdmin() throws Exception {
        User worker = getUserForRole(Role.WORKER);
        
        mockMvc.perform(delete("/api/users/" + worker.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN)))
                .andExpect(status().isNoContent());

        // Проверяем, что пользователь действительно удален
        mockMvc.perform(get("/api/users/" + worker.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Удаление пользователя WORKER'ом запрещено")
    public void testDeleteUserAsWorker() throws Exception {
        User master = getUserForRole(Role.MASTER);
        
        mockMvc.perform(delete("/api/users/" + master.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.WORKER)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Получение пользователей с пагинацией")
    public void testGetUsersPaged() throws Exception {
        mockMvc.perform(get("/api/users/paged")
                .param("page", "0")
                .param("size", "10")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()));
    }

    @Test
    @DisplayName("Создание пользователя FOREMAN'ом")
    public void testCreateUserAsForeman() throws Exception {
        UserRequestDTO dto = new UserRequestDTO();
        dto.setUsername("foreman_created_user");
        dto.setPassword("password123");
        dto.setRole(Role.WORKER);

        mockMvc.perform(post("/api/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.FOREMAN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("foreman_created_user")));
    }
}

