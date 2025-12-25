package com.example.demo.services;

import com.example.demo.dto.request.NotificationRequestDTO;
import com.example.demo.dto.response.NotificationResponseDTO;
import com.example.demo.models.Notification;
import com.example.demo.models.NotificationType;
import com.example.demo.models.User;
import com.example.demo.repositories.NotificationRepository;
import com.example.demo.repositories.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    private NotificationResponseDTO toDTO(Notification notification) {
        return new NotificationResponseDTO(
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getType(),
                notification.getIsRead(),
                notification.getCreatedAt(),
                notification.getRelatedEntityId(),
                notification.getRelatedEntityType()
        );
    }

    public List<NotificationResponseDTO> getUserNotifications(Long userId) {
        System.out.println(notificationRepository.findAll());
        System.out.println("getUserNotifications: " + userId);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public Page<NotificationResponseDTO> getUserNotificationsPaged(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toDTO);
    }

    public List<NotificationResponseDTO> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(userId, false)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsRead(userId, false);
    }

    public Optional<NotificationResponseDTO> createNotification(NotificationRequestDTO dto) {
        Optional<User> userOpt = userRepository.findById(dto.getUserId());
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }

        Notification notification = new Notification();
        notification.setUser(userOpt.get());
        notification.setTitle(dto.getTitle());
        notification.setMessage(dto.getMessage());
        notification.setType(dto.getType() != null ? dto.getType() : NotificationType.INFO);
        notification.setRelatedEntityId(dto.getRelatedEntityId());
        notification.setRelatedEntityType(dto.getRelatedEntityType());

        Notification saved = notificationRepository.save(notification);
        return Optional.of(toDTO(saved));
    }

    public Optional<NotificationResponseDTO> markAsRead(Long notificationId) {
        Optional<Notification> notificationOpt = notificationRepository.findById(notificationId);
        if (notificationOpt.isEmpty()) {
            return Optional.empty();
        }

        Notification notification = notificationOpt.get();
        notification.setIsRead(true);
        Notification saved = notificationRepository.save(notification);
        return Optional.of(toDTO(saved));
    }

    public void markAllAsRead(Long userId) {
        List<Notification> notifications = notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(userId, false);
        notifications.forEach(notification -> notification.setIsRead(true));
        notificationRepository.saveAll(notifications);
    }

    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
    }

    // Метод для создания уведомления о новой задаче
    public void createTaskAssignedNotification(User user, Long taskId, String taskName) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle("Новая задача назначена");
        notification.setMessage("Вам назначена новая задача: " + taskName);
        notification.setType(NotificationType.TASK_ASSIGNED);
        notification.setRelatedEntityId(taskId);
        notification.setRelatedEntityType("TASK");
        notificationRepository.save(notification);
    }

    // Метод для создания уведомления об обновлении задачи
    public void createTaskUpdatedNotification(User user, Long taskId, String taskName) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle("Задача обновлена");
        notification.setMessage("Задача обновлена: " + taskName);
        notification.setType(NotificationType.TASK_UPDATED);
        notification.setRelatedEntityId(taskId);
        notification.setRelatedEntityType("TASK");
        notificationRepository.save(notification);
    }
}

