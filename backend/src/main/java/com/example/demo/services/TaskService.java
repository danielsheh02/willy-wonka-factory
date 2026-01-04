package com.example.demo.services;

import com.example.demo.dto.request.TaskFilterRequestDTO;
import com.example.demo.dto.request.TaskRequestDTO;
import com.example.demo.exceptions.WorkerOverloadedException;
import com.example.demo.models.*;
import com.example.demo.models.specifications.TaskSpecification;
import com.example.demo.repositories.TaskRepository;
import com.example.demo.repositories.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TaskService {
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    public static final int MAX_ALLOWED_TASKS = 5;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository, NotificationService notificationService) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    public Iterable<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    public Optional<Task> getTaskById(Long id) {
        return taskRepository.findById(id);
    }

    public Page<Task> getAllTasksPaged(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return taskRepository.findAll(pageable);
    }

    public Page<Task> filterTasksPaged(TaskFilterRequestDTO dto, int page, int size) {
        Specification<Task> spec = TaskSpecification.withFilters(
                dto.getName(),
                dto.getCreatedAfter(),
                dto.getCreatedBefore(),
                dto.getCompletedAfter(),
                dto.getCompletedBefore(),
                dto.getUserId(),
                dto.getStatus());

        Pageable pageable = PageRequest.of(page, size);
        return taskRepository.findAll(spec, pageable);
    }

    public List<Task> filterTasks(TaskFilterRequestDTO dto) {
        Specification<Task> spec = TaskSpecification.withFilters(
                dto.getName(),
                dto.getCreatedAfter(),
                dto.getCreatedBefore(),
                dto.getCompletedAfter(),
                dto.getCompletedBefore(),
                dto.getUserId(),
                dto.getStatus());
        return taskRepository.findAll(spec);
    }

    public Optional<Task> createTask(TaskRequestDTO dto) {
        User user = null;
        if (dto.getUserId() != null) {
            Optional<User> userOpt = userRepository.findById(dto.getUserId());
            if (userOpt.isEmpty())
                return Optional.empty();
            user = userOpt.get();

            long activeTasks = taskRepository.countByUserIdAndStatusNot(user.getId(), TaskStatus.COMPLETED);
            if (activeTasks >= MAX_ALLOWED_TASKS && !dto.isForce()) {
                throw new WorkerOverloadedException("User has reached the task limit");
            }
        }

        Task task = new Task();
        task.setName(dto.getName());
        task.setDescription(dto.getDescription());
        
        // Если рабочий не указан, автоматически ставим статус NOT_ASSIGNED
        if (user == null) {
            task.setStatus(TaskStatus.NOT_ASSIGNED);
        } else {
            task.setStatus(dto.getStatus());
        }
        
        task.setUser(user);

        if (task.getStatus() == TaskStatus.COMPLETED) {
            task.setCompletedAt(LocalDateTime.now());
        }

        Task savedTask = taskRepository.save(task);

        // Создаем уведомление для пользователя о новой задаче
        if (user != null) {
            notificationService.createTaskAssignedNotification(user, savedTask.getId(), savedTask.getName());
        }

        return Optional.of(savedTask);
    }

    public Optional<Task> updateTask(Long id, TaskRequestDTO dto) {
        Optional<Task> taskOpt = taskRepository.findById(id);
        if (taskOpt.isEmpty())
            return Optional.empty();

        Task task = taskOpt.get();

        task.setName(dto.getName());
        task.setDescription(dto.getDescription());

        User user = null;
        if (dto.getUserId() != null) {
            Optional<User> userOpt = userRepository.findById(dto.getUserId());
            if (userOpt.isEmpty())
                return Optional.empty();
            user = userOpt.get();

            if (user.getId() != task.getUser().getId()) {
                long activeTasks = taskRepository.countByUserIdAndStatusNot(user.getId(), TaskStatus.COMPLETED);
                if (activeTasks >= MAX_ALLOWED_TASKS && !dto.isForce()) {
                    throw new WorkerOverloadedException("User has reached the task limit");
                }
            }
        }

        if (task.getStatus() != TaskStatus.COMPLETED && dto.getStatus() == TaskStatus.COMPLETED) {
            task.setCompletedAt(LocalDateTime.now());
        }

        // Проверяем, изменился ли пользователь
        boolean userChanged = (task.getUser() == null && user != null) || 
                              (task.getUser() != null && user != null && !task.getUser().getId().equals(user.getId()));

        task.setStatus(dto.getStatus());
        task.setUser(user);

        Task savedTask = taskRepository.save(task);

        // Создаем уведомление если задача переназначена на другого пользователя
        if (userChanged && user != null) {
            notificationService.createTaskAssignedNotification(user, savedTask.getId(), savedTask.getName());
        } else if (user != null && !userChanged) {
            // Если пользователь тот же, отправляем уведомление об обновлении
            notificationService.createTaskUpdatedNotification(user, savedTask.getId(), savedTask.getName());
        }

        return Optional.of(savedTask);
    }

    public void deleteTask(Long id) {
        taskRepository.deleteById(id);
    }

    /**
     * Взять задачу себе (для рабочих)
     */
    public Optional<Task> assignTaskToMe(Long taskId, Long userId) {
        Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty()) {
            return Optional.empty();
        }

        Task task = taskOpt.get();
        
        // Проверяем, что задача не назначена
        if (task.getUser() != null) {
            throw new RuntimeException("Задача уже назначена другому рабочему");
        }

        // Находим пользователя
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }
        User user = userOpt.get();

        // Проверяем лимит активных задач
        long activeTasks = taskRepository.countByUserIdAndStatusNot(userId, TaskStatus.COMPLETED);
        if (activeTasks >= MAX_ALLOWED_TASKS) {
            throw new WorkerOverloadedException("Вы достигли лимита активных задач (" + MAX_ALLOWED_TASKS + ")");
        }

        // Назначаем задачу и меняем статус на IN_PROGRESS
        task.setUser(user);
        task.setStatus(TaskStatus.IN_PROGRESS);

        Task savedTask = taskRepository.save(task);

        // Создаем уведомление
        notificationService.createTaskAssignedNotification(user, savedTask.getId(), savedTask.getName());

        return Optional.of(savedTask);
    }

    /**
     * Отказаться от задачи (для рабочих)
     */
    public Optional<Task> unassignTask(Long taskId, Long userId) {
        Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty()) {
            return Optional.empty();
        }

        Task task = taskOpt.get();
        
        // Проверяем, что задача назначена на этого пользователя
        if (task.getUser() == null || !task.getUser().getId().equals(userId)) {
            throw new RuntimeException("Эта задача не назначена на вас");
        }

        // Снимаем назначение и меняем статус на NOT_ASSIGNED
        task.setUser(null);
        task.setStatus(TaskStatus.NOT_ASSIGNED);

        Task savedTask = taskRepository.save(task);

        return Optional.of(savedTask);
    }

}
