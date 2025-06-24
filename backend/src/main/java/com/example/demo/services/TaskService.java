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
    public static final int MAX_ALLOWED_TASKS = 5;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
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
        task.setStatus(dto.getStatus());
        task.setUser(user);

        if (dto.getStatus() == TaskStatus.COMPLETED) {
            task.setCompletedAt(LocalDateTime.now());
        }

        return Optional.of(taskRepository.save(task));
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

            long activeTasks = taskRepository.countByUserIdAndStatusNot(user.getId(), TaskStatus.COMPLETED);
            if (activeTasks >= MAX_ALLOWED_TASKS && !dto.isForce()) {
                throw new WorkerOverloadedException("User has reached the task limit");
            }
        }

        if (task.getStatus() != TaskStatus.COMPLETED && dto.getStatus() == TaskStatus.COMPLETED) {
            task.setCompletedAt(LocalDateTime.now());
        }

        task.setStatus(dto.getStatus());
        task.setUser(user);

        return Optional.of(taskRepository.save(task));
    }

    public void deleteTask(Long id) {
        taskRepository.deleteById(id);
    }

}
