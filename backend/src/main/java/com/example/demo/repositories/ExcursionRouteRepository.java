package com.example.demo.repositories;

import com.example.demo.models.ExcursionRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExcursionRouteRepository extends JpaRepository<ExcursionRoute, Long> {
    
    List<ExcursionRoute> findByExcursionId(Long excursionId);
    
    List<ExcursionRoute> findByWorkshopId(Long workshopId);
    
    // Найти все маршруты для конкретного цеха в заданный период времени
    @Query("SELECT r FROM ExcursionRoute r WHERE r.workshop.id = :workshopId " +
           "AND r.excursion.status IN ('CONFIRMED', 'IN_PROGRESS') " +
           "AND r.startTime < :endTime " +
           "AND FUNCTION('TIMESTAMPADD', MINUTE, r.durationMinutes, r.startTime) > :startTime")
    List<ExcursionRoute> findConflictingRoutes(
        @Param("workshopId") Long workshopId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );
    
    // Получить суммарное количество участников в цехе в определенное время
    @Query("SELECT COALESCE(SUM(r.excursion.participantsCount), 0) FROM ExcursionRoute r " +
           "WHERE r.workshop.id = :workshopId " +
           "AND r.excursion.status IN ('CONFIRMED', 'IN_PROGRESS') " +
           "AND r.startTime < :endTime " +
           "AND FUNCTION('TIMESTAMPADD', MINUTE, r.durationMinutes, r.startTime) > :startTime")
    Integer getTotalParticipantsInWorkshop(
        @Param("workshopId") Long workshopId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );
}

