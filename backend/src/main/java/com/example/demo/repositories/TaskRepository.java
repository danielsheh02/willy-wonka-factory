package com.example.demo.repositories;

import com.example.demo.models.Task;
import com.example.demo.models.TaskStatus;
import com.example.demo.models.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {
    long countByUserIdAndStatusNot(long id, TaskStatus taskStatus);
    
    /**
     * Найти все задачи пользователя с определенным статусом
     */
    List<Task> findByUserAndStatus(User user, TaskStatus status);
}
