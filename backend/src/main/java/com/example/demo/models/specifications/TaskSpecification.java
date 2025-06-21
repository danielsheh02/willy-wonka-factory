package com.example.demo.models.specifications;

import com.example.demo.models.Task;
import com.example.demo.models.TaskStatus;

import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import com.example.demo.models.Task_;
import com.example.demo.models.User_;

import java.time.LocalDateTime;

public class TaskSpecification {
    public static Specification<Task> withFilters(
            String name,
            LocalDateTime createdAfter,
            LocalDateTime createdBefore,
            LocalDateTime completedAfter,
            LocalDateTime completedBefore,
            Long userId,
            TaskStatus status) {
        return (Root<Task> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            Predicate predicate = cb.conjunction();

            if (name != null && !name.isEmpty()) {
                predicate = cb.and(predicate, cb.like(cb.lower(root.get(Task_.name)), "%" + name.toLowerCase() + "%"));
            }

            if (createdAfter != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get(Task_.createdAt), createdAfter));
            }

            if (createdBefore != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get(Task_.createdAt), createdBefore));
            }

            if (completedAfter != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get(Task_.completedAt), completedAfter));
            }

            if (completedBefore != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get(Task_.completedAt), completedBefore));
            }

            if (userId != null) {
                predicate = cb.and(predicate, cb.equal(root.get(Task_.user).get(User_.id), userId));
            }

            if (status != null) {
                predicate = cb.and(predicate, cb.equal(root.get(Task_.status), status));
            }

            return predicate;
        };
    }
}
