package com.example.demo.repositories;

import com.example.demo.models.GoldenTicket;
import com.example.demo.models.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GoldenTicketRepository extends JpaRepository<GoldenTicket, Long> {
    
    Optional<GoldenTicket> findByTicketNumber(String ticketNumber);
    
    List<GoldenTicket> findByStatus(TicketStatus status);
    
    List<GoldenTicket> findByExcursionId(Long excursionId);
    
    boolean existsByTicketNumber(String ticketNumber);
    
    // Найти все билеты, у которых истек срок действия
    @Query("SELECT t FROM GoldenTicket t WHERE t.status = 'ACTIVE' " +
           "AND t.expiresAt IS NOT NULL AND t.expiresAt < :now")
    List<GoldenTicket> findExpiredTickets(@Param("now") LocalDateTime now);
    
    // Найти все забронированные билеты на экскурсии, которые уже начались
    @Query("SELECT t FROM GoldenTicket t WHERE t.status = 'BOOKED' " +
           "AND t.excursion IS NOT NULL AND t.excursion.startTime < :now")
    List<GoldenTicket> findTicketsForStartedExcursions(@Param("now") LocalDateTime now);
    
    // Подсчитать количество забронированных билетов на конкретную экскурсию
    long countByExcursionIdAndStatus(Long excursionId, TicketStatus status);
}

