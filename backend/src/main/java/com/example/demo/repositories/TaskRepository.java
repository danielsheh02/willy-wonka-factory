package com.example.demo.repositories;

import com.example.demo.models.Task;
import com.example.demo.models.TaskStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {
    long countByUserIdAndStatusNot(long id, TaskStatus taskStatus);
}
