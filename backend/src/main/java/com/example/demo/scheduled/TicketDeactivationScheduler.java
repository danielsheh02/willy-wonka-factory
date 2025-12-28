package com.example.demo.scheduled;

import com.example.demo.services.GoldenTicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TicketDeactivationScheduler {

    @Autowired
    private GoldenTicketService ticketService;

    /**
     * Каждые 5 минут проверяем и деактивируем билеты для начавшихся экскурсий
     */
    @Scheduled(fixedRate = 300000) // 5 минут = 300000 мс
    public void deactivateTicketsForStartedExcursions() {
        try {
            int deactivated = ticketService.deactivateTicketsForStartedExcursions();
            if (deactivated > 0) {
                System.out.println("[Ticket Deactivation] Деактивировано " + deactivated + " билетов для начавшихся экскурсий");
            }
        } catch (Exception e) {
            System.err.println("[Ticket Deactivation Error] " + e.getMessage());
        }
    }

    /**
     * Каждый час проверяем и деактивируем истекшие билеты
     */
    @Scheduled(fixedRate = 3600000) // 1 час = 3600000 мс
    public void deactivateExpiredTickets() {
        try {
            int deactivated = ticketService.deactivateExpiredTickets();
            if (deactivated > 0) {
                System.out.println("[Ticket Expiration] Деактивировано " + deactivated + " истекших билетов");
            }
        } catch (Exception e) {
            System.err.println("[Ticket Expiration Error] " + e.getMessage());
        }
    }
}

