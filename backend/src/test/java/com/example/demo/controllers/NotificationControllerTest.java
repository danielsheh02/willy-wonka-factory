package com.example.demo.controllers;

import com.example.demo.BaseTest;
import com.example.demo.dto.request.NotificationRequestDTO;
import com.example.demo.models.Notification;
import com.example.demo.models.NotificationType;
import com.example.demo.models.Role;
import com.example.demo.models.User;
import com.example.demo.repositories.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@DisplayName("Интеграционные тесты для NotificationController")
public class NotificationControllerTest extends BaseTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Override
    @BeforeEach
    public void setupBaseTest() {
        super.setupBaseTest();
        notificationRepository.deleteAll();
    }

    @Test
    @DisplayName("Получение уведомлений текущего пользователя")
    public void testGetMyNotifications() throws Exception {
        User worker = getUserForRole(Role.WORKER);
        
        Notification notification = new Notification();
        notification.setUser(worker);
        notification.setTitle("Тестовое уведомление");
        notification.setMessage("Сообщение");
        notification.setType(NotificationType.TASK_ASSIGNED);
        notification.setIsRead(false);
        notificationRepository.save(notification);

        mockMvc.perform(get("/api/notifications/my")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.WORKER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].title", is("Тестовое уведомление")));
    }

    @Test
    @DisplayName("Получение непрочитанных уведомлений текущего пользователя")
    public void testGetMyUnreadNotifications() throws Exception {
        User worker = getUserForRole(Role.WORKER);
        
        // Создаем прочитанное уведомление
        Notification read = new Notification();
        read.setUser(worker);
        read.setTitle("Прочитано");
        read.setMessage("Прочитанное сообщение");
        read.setType(NotificationType.TASK_ASSIGNED);
        read.setIsRead(true);
        notificationRepository.save(read);

        // Создаем непрочитанное уведомление
        Notification unread = new Notification();
        unread.setUser(worker);
        unread.setTitle("Непрочитано");
        unread.setMessage("Непрочитанное сообщение");
        unread.setType(NotificationType.TASK_ASSIGNED);
        unread.setIsRead(false);
        notificationRepository.save(unread);

        mockMvc.perform(get("/api/notifications/my/unread")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.WORKER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Непрочитано")));
    }

    @Test
    @DisplayName("Получение количества непрочитанных уведомлений")
    public void testGetUnreadCount() throws Exception {
        User worker = getUserForRole(Role.WORKER);
        
        // Создаем 3 непрочитанных уведомления
        for (int i = 0; i < 3; i++) {
            Notification notification = new Notification();
            notification.setUser(worker);
            notification.setTitle("Уведомление " + i);
            notification.setMessage("Сообщение " + i);
            notification.setType(NotificationType.INFO);
            notification.setIsRead(false);
            notificationRepository.save(notification);
        }

        mockMvc.perform(get("/api/notifications/my/unread/count")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.WORKER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count", is(3)));
    }

    @Test
    @DisplayName("Создание уведомления")
    public void testCreateNotification() throws Exception {
        User worker = getUserForRole(Role.WORKER);
        
        NotificationRequestDTO dto = new NotificationRequestDTO();
        dto.setUserId(worker.getId());
        dto.setTitle("Новое уведомление");
        dto.setMessage("Текст уведомления");
        dto.setType(NotificationType.TASK_ASSIGNED);
        dto.setRelatedEntityId(1L);
        dto.setRelatedEntityType("Task");

        mockMvc.perform(post("/api/notifications")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Новое уведомление")))
                .andExpect(jsonPath("$.message", is("Текст уведомления")))
                .andExpect(jsonPath("$.type", is("TASK_ASSIGNED")))
                .andExpect(jsonPath("$.isRead", is(false)));
    }

    @Test
    @DisplayName("Создание уведомления с несуществующим пользователем")
    public void testCreateNotificationWithInvalidUser() throws Exception {
        NotificationRequestDTO dto = new NotificationRequestDTO();
        dto.setUserId(99999L);
        dto.setTitle("Уведомление");
        dto.setMessage("Сообщение");
        dto.setType(NotificationType.INFO);

        mockMvc.perform(post("/api/notifications")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Invalid userId")));
    }

    @Test
    @DisplayName("Отметить уведомление как прочитанное")
    public void testMarkAsRead() throws Exception {
        User worker = getUserForRole(Role.WORKER);
        
        Notification notification = new Notification();
        notification.setUser(worker);
        notification.setTitle("Уведомление");
        notification.setMessage("Сообщение");
        notification.setType(NotificationType.INFO);
        notification.setIsRead(false);
        Notification saved = notificationRepository.save(notification);

        mockMvc.perform(put("/api/notifications/" + saved.getId() + "/read")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.WORKER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRead", is(true)));
    }

    @Test
    @DisplayName("Отметить все уведомления как прочитанные")
    public void testMarkAllAsRead() throws Exception {
        User worker = getUserForRole(Role.WORKER);
        
        // Создаем несколько непрочитанных уведомлений
        for (int i = 0; i < 5; i++) {
            Notification notification = new Notification();
            notification.setUser(worker);
            notification.setTitle("Уведомление " + i);
            notification.setMessage("Сообщение " + i);
            notification.setType(NotificationType.INFO);
            notification.setIsRead(false);
            notificationRepository.save(notification);
        }

        mockMvc.perform(put("/api/notifications/my/read-all")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.WORKER)))
                .andExpect(status().isOk());

        // Проверяем, что все уведомления отмечены как прочитанные
        mockMvc.perform(get("/api/notifications/my/unread/count")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.WORKER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count", is(0)));
    }

    @Test
    @DisplayName("Удаление уведомления")
    public void testDeleteNotification() throws Exception {
        User worker = getUserForRole(Role.WORKER);
        
        Notification notification = new Notification();
        notification.setUser(worker);
        notification.setTitle("Уведомление для удаления");
        notification.setMessage("Будет удалено");
        notification.setType(NotificationType.INFO);
        Notification saved = notificationRepository.save(notification);

        mockMvc.perform(delete("/api/notifications/" + saved.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.WORKER)))
                .andExpect(status().isNoContent());

        // Проверяем, что уведомление удалено
        mockMvc.perform(get("/api/notifications/my")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.WORKER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + saved.getId() + ")]").doesNotExist());
    }

    @Test
    @DisplayName("Получение уведомлений с пагинацией")
    public void testGetMyNotificationsPaged() throws Exception {
        User worker = getUserForRole(Role.WORKER);
        
        // Создаем 15 уведомлений
        for (int i = 0; i < 15; i++) {
            Notification notification = new Notification();
            notification.setUser(worker);
            notification.setTitle("Уведомление " + i);
            notification.setMessage("Сообщение " + i);
            notification.setType(NotificationType.INFO);
            notificationRepository.save(notification);
        }

        mockMvc.perform(get("/api/notifications/my/paged")
                .param("page", "0")
                .param("size", "10")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.WORKER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(10)));
    }

    @Test
    @DisplayName("Попытка прочитать чужие уведомления")
    public void testCannotAccessOtherUsersNotifications() throws Exception {
        User worker = getUserForRole(Role.WORKER);
        User admin = getUserForRole(Role.ADMIN);
        
        // Создаем уведомление для worker'а
        Notification notification = new Notification();
        notification.setUser(worker);
        notification.setTitle("Для worker");
        notification.setMessage("Сообщение");
        notification.setType(NotificationType.INFO);
        notificationRepository.save(notification);

        // Admin пытается получить свои уведомления (должен получить пустой список)
        mockMvc.perform(get("/api/notifications/my")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Отметить несуществующее уведомление как прочитанное")
    public void testMarkNonExistentNotificationAsRead() throws Exception {
        mockMvc.perform(put("/api/notifications/99999/read")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.WORKER)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Создание уведомления разных типов")
    public void testCreateNotificationsOfDifferentTypes() throws Exception {
        User worker = getUserForRole(Role.WORKER);
        
        NotificationType[] types = NotificationType.values();
        
        for (NotificationType type : types) {
            NotificationRequestDTO dto = new NotificationRequestDTO();
            dto.setUserId(worker.getId());
            dto.setTitle("Уведомление типа " + type.name());
            dto.setMessage("Сообщение");
            dto.setType(type);

            mockMvc.perform(post("/api/notifications")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(asJsonString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.type", is(type.name())));
        }
    }
}

