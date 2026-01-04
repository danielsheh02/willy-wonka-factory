package com.example.demo.scheduled;

import com.example.demo.services.ExcursionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Планировщик для автоматического обновления статусов экскурсий
 */
@Component
public class ExcursionStatusScheduler {

    @Autowired
    private ExcursionService excursionService;

    /**
     * Проверяет и обновляет статусы экскурсий каждую минуту
     */
    @Scheduled(fixedRate = 60000) // Каждую минуту (60000 миллисекунд)
    public void updateExcursionStatuses() {
        try {
            Map<String, Integer> result = excursionService.updateExcursionStatuses();
            
            int startedCount = result.getOrDefault("started", 0);
            int completedCount = result.getOrDefault("completed", 0);
            
            if (startedCount > 0 || completedCount > 0) {
                System.out.println("[ExcursionStatusScheduler UTC] Обновлено статусов: " + 
                                 startedCount + " начато, " + completedCount + " завершено");
            }
        } catch (Exception e) {
            System.err.println("[ExcursionStatusScheduler Error] " + e.getMessage());
        }
    }
}

