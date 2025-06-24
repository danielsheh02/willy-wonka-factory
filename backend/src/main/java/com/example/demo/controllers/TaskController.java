package com.example.demo.controllers;

import com.example.demo.dto.request.TaskFilterRequestDTO;
import com.example.demo.dto.request.TaskRequestDTO;
import com.example.demo.models.Task;
import com.example.demo.models.TaskStatus;
import com.example.demo.services.TaskService;

import java.util.List;
import java.util.Optional;

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
        Optional<Task> taskOpt = taskService.createTask(dto);
        if (taskOpt.isPresent()) {
            return ResponseEntity.ok(taskOpt.get());
        } else {
            return ResponseEntity.badRequest().body("Invalid userId");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTask(@PathVariable Long id, @RequestBody TaskRequestDTO dto) {
        return taskService.updateTask(id, dto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
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
}
