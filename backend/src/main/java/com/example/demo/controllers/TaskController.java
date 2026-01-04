package com.example.demo.controllers;

import com.example.demo.dto.request.TaskFilterRequestDTO;
import com.example.demo.dto.request.TaskRequestDTO;
import com.example.demo.models.Task;
import com.example.demo.models.TaskStatus;
import com.example.demo.services.TaskService;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public Iterable<Task> getAllTasks() {
        return taskService.getAllTasks();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskById(@PathVariable Long id) {
        return taskService.getTaskById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/paged")
    public ResponseEntity<?> getAllTasksPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Task> tasksPage = taskService.getAllTasksPaged(page, size);
        return ResponseEntity.ok(tasksPage);
    }

    @PostMapping("/filter-paged")
    public ResponseEntity<?> filterTasksPaged(
            @RequestBody TaskFilterRequestDTO dto,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(taskService.filterTasksPaged(dto, page, size));
    }

    @PostMapping("/filter")
    public List<Task> filterTasks(@RequestBody TaskFilterRequestDTO dto) {
        return taskService.filterTasks(dto);
    }

    @PostMapping
    public ResponseEntity<?> createTask(@RequestBody TaskRequestDTO dto) {
        try {
            Optional<Task> taskOpt = taskService.createTask(dto);
            if (taskOpt.isPresent()) {
                return ResponseEntity.ok(taskOpt.get());
            } else {
                return ResponseEntity.badRequest().body("Invalid userId");
            }
        } catch (com.example.demo.exceptions.WorkerOverloadedException e) {
            return ResponseEntity.status(409).body("Worker has reached the task limit");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTask(@PathVariable Long id, @RequestBody TaskRequestDTO dto) {
        try {
            return taskService.updateTask(id, dto)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (com.example.demo.exceptions.WorkerOverloadedException e) {
            return ResponseEntity.status(409).body("Worker has reached the task limit");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/statuses")
    public ResponseEntity<String[]> getTaskStatuses() {
        TaskStatus[] statuses = TaskStatus.values();
        String[] statusNames = new String[statuses.length];
        for (int i = 0; i < statuses.length; i++) {
            statusNames[i] = statuses[i].name();
        }
        return ResponseEntity.ok(statusNames);
    }

    /**
     * Взять задачу себе
     */
    @PostMapping("/{id}/assign-to-me")
    public ResponseEntity<?> assignTaskToMe(@PathVariable Long id, @RequestParam Long userId) {
        try {
            return taskService.assignTaskToMe(id, userId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Отказаться от задачи
     */
    @PostMapping("/{id}/unassign")
    public ResponseEntity<?> unassignTask(@PathVariable Long id, @RequestParam Long userId) {
        try {
            return taskService.unassignTask(id, userId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Автоматическое распределение задач
     */
    @PostMapping("/distribute")
    public ResponseEntity<?> distributeTasks(
            @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Object> taskIdsRaw = (List<Object>) request.get("taskIds");
            Boolean force = (Boolean) request.getOrDefault("force", false);
            
            if (taskIdsRaw == null || taskIdsRaw.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Список задач пуст"));
            }
            
            // Преобразуем Integer в Long
            List<Long> taskIds = taskIdsRaw.stream()
                .map(id -> {
                    if (id instanceof Integer) {
                        return ((Integer) id).longValue();
                    } else if (id instanceof Long) {
                        return (Long) id;
                    } else {
                        return Long.parseLong(id.toString());
                    }
                })
                .collect(Collectors.toList());
            
            Map<String, Object> result = taskService.distributeTasksAutomatically(taskIds, force);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Получить все нераспределенные задачи
     */
    @GetMapping("/unassigned")
    public ResponseEntity<List<Task>> getUnassignedTasks() {
        try {
            Iterable<Task> allTasks = taskService.getAllTasks();
            List<Task> result = StreamSupport.stream(allTasks.spliterator(), false)
                .filter(task -> task.getUser() == null && task.getStatus() == TaskStatus.NOT_ASSIGNED)
                .collect(Collectors.toList());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
