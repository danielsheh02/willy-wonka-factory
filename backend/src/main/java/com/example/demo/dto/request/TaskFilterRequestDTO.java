package com.example.demo.dto.request;

import java.time.LocalDateTime;

import com.example.demo.models.TaskStatus;

public class TaskFilterRequestDTO {
    private String name;
    private LocalDateTime createdAfter;
    private LocalDateTime createdBefore;
    private LocalDateTime completedAfter;
    private LocalDateTime completedBefore;
    private Long userId;
    private TaskStatus status;

    public TaskFilterRequestDTO(String name, LocalDateTime createdAfter, LocalDateTime createdBefore,
            LocalDateTime completedAfter, LocalDateTime completedBefore, Long userId, TaskStatus status) {
        this.name = name;
        this.createdAfter = createdAfter;
        this.createdBefore = createdBefore;
        this.completedAfter = completedAfter;
        this.completedBefore = completedBefore;
        this.userId = userId;
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getCreatedAfter() {
        return createdAfter;
    }

    public void setCreatedAfter(LocalDateTime createdAfter) {
        this.createdAfter = createdAfter;
    }

    public LocalDateTime getCreatedBefore() {
        return createdBefore;
    }

    public void setCreatedBefore(LocalDateTime createdBefore) {
        this.createdBefore = createdBefore;
    }

    public LocalDateTime getCompletedAfter() {
        return completedAfter;
    }

    public void setCompletedAfter(LocalDateTime completedAfter) {
        this.completedAfter = completedAfter;
    }

    public LocalDateTime getCompletedBefore() {
        return completedBefore;
    }

    public void setCompletedBefore(LocalDateTime completedBefore) {
        this.completedBefore = completedBefore;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }
}
