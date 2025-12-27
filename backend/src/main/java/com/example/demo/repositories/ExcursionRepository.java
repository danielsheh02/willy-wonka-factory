package com.example.demo.repositories;

import com.example.demo.models.Excursion;
import com.example.demo.models.ExcursionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExcursionRepository extends JpaRepository<Excursion, Long> {
    
    List<Excursion> findByGuideId(Long guideId);
    
    List<Excursion> findByStatus(ExcursionStatus status);
    
    List<Excursion> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);
    
    List<Excursion> findByStatusAndStartTimeAfter(ExcursionStatus status, LocalDateTime startTime);
    
    // Проверка занятости гида в определенный период времени
    @Query("SELECT e FROM Excursion e WHERE e.guide.id = :guideId " +
           "AND e.status IN ('CONFIRMED', 'IN_PROGRESS') " +
           "AND EXISTS (SELECT r FROM ExcursionRoute r WHERE r.excursion.id = e.id " +
           "AND r.startTime < :endTime " +
           "AND FUNCTION('TIMESTAMPADD', MINUTE, r.durationMinutes, r.startTime) > :startTime)")
    List<Excursion> findGuideConflicts(
        @Param("guideId") Long guideId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );
}

