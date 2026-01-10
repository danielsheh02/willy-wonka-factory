package com.example.demo.controllers;

import com.example.demo.BaseTest;
import com.example.demo.dto.request.AuthUserRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@DisplayName("Функциональные тесты для AuthController")
public class AuthControllerTest extends BaseTest {

    @Test
    @DisplayName("Успешная регистрация нового пользователя")
    public void testSignupSuccess() throws Exception {
        AuthUserRequestDTO requestDTO = new AuthUserRequestDTO();
        requestDTO.setUsername("newuser");
        requestDTO.setPassword("password123");

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("newuser")))
                .andExpect(jsonPath("$.role", notNullValue()));
    }

    @Test
    @DisplayName("Регистрация с существующим username")
    public void testSignupWithExistingUsername() throws Exception {
        AuthUserRequestDTO requestDTO = new AuthUserRequestDTO();
        requestDTO.setUsername("admin_user");
        requestDTO.setPassword("password123");

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(requestDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Успешная аутентификация пользователя")
    public void testSigninSuccess() throws Exception {
        AuthUserRequestDTO requestDTO = new AuthUserRequestDTO();
        requestDTO.setUsername("admin_user");
        requestDTO.setPassword("password123");

        mockMvc.perform(post("/api/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.username", is("admin_user")));
    }

    @Test
    @DisplayName("Аутентификация с неверным паролем")
    public void testSigninWithWrongPassword() throws Exception {
        AuthUserRequestDTO requestDTO = new AuthUserRequestDTO();
        requestDTO.setUsername("admin_user");
        requestDTO.setPassword("wrongpassword");

        mockMvc.perform(post("/api/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(requestDTO)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("Invalid")));
    }

    @Test
    @DisplayName("Аутентификация с несуществующим пользователем")
    public void testSigninWithNonExistentUser() throws Exception {
        AuthUserRequestDTO requestDTO = new AuthUserRequestDTO();
        requestDTO.setUsername("nonexistent");
        requestDTO.setPassword("password123");

        mockMvc.perform(post("/api/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(requestDTO)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Регистрация с пустым username")
    public void testSignupWithEmptyUsername() throws Exception {
        AuthUserRequestDTO requestDTO = new AuthUserRequestDTO();
        requestDTO.setUsername("");
        requestDTO.setPassword("password123");

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(requestDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Получение JWT токена после успешной аутентификации")
    public void testGetJwtTokenAfterSignin() throws Exception {
        AuthUserRequestDTO requestDTO = new AuthUserRequestDTO();
        requestDTO.setUsername("worker_user");
        requestDTO.setPassword("password123");

        MvcResult result = mockMvc.perform(post("/api/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        assert response.contains("token");
        assert response.contains("worker_user");
    }
}

