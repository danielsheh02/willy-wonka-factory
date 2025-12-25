package com.example.demo.controllers;

import com.example.demo.dto.request.NotificationRequestDTO;
import com.example.demo.dto.response.NotificationResponseDTO;
import com.example.demo.security.UserDetailsImpl;
import com.example.demo.services.NotificationService;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // Получить все уведомления текущего пользователя
    @GetMapping("/my")
    public ResponseEntity<List<NotificationResponseDTO>> getMyNotifications(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        List<NotificationResponseDTO> notifications = notificationService.getUserNotifications(userDetails.getId());
        return ResponseEntity.ok(notifications);
    }

    // Получить уведомления пользователя с пагинацией
    @GetMapping("/my/paged")
    public ResponseEntity<Page<NotificationResponseDTO>> getMyNotificationsPaged(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<NotificationResponseDTO> notifications = notificationService.getUserNotificationsPaged(
                userDetails.getId(), page, size);
        return ResponseEntity.ok(notifications);
    }

    // Получить непрочитанные уведомления текущего пользователя
    @GetMapping("/my/unread")
    public ResponseEntity<List<NotificationResponseDTO>> getMyUnreadNotifications(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        List<NotificationResponseDTO> notifications = notificationService.getUnreadNotifications(userDetails.getId());
        return ResponseEntity.ok(notifications);
    }

    // Получить количество непрочитанных уведомлений
    @GetMapping("/my/unread/count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        long count = notificationService.getUnreadCount(userDetails.getId());
        return ResponseEntity.ok(Map.of("count", count));
    }

    // Создать уведомление (для администраторов/системы)
    @PostMapping
    public ResponseEntity<?> createNotification(@RequestBody NotificationRequestDTO dto) {
        Optional<NotificationResponseDTO> notificationOpt = notificationService.createNotification(dto);
        if (notificationOpt.isPresent()) {
            return ResponseEntity.ok(notificationOpt.get());
        } else {
            return ResponseEntity.badRequest().body("Invalid userId");
        }
    }

    // Отметить уведомление как прочитанное
    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        Optional<NotificationResponseDTO> notificationOpt = notificationService.markAsRead(id);
        if (notificationOpt.isPresent()) {
            return ResponseEntity.ok(notificationOpt.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Отметить все уведомления как прочитанные
    @PutMapping("/my/read-all")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        notificationService.markAllAsRead(userDetails.getId());
        return ResponseEntity.ok().build();
    }

    // Удалить уведомление
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.noContent().build();
    }
}

