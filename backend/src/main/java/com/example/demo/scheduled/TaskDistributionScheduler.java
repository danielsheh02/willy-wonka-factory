package com.example.demo.scheduled;

import com.example.demo.models.Task;
import com.example.demo.models.TaskStatus;
import com.example.demo.services.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Планировщик автоматического распределения задач
 * Запускается по расписанию и распределяет нераспределенные задачи между рабочими
 */
@Component
public class TaskDistributionScheduler {

    @Autowired
    private TaskService taskService;

    /**
     * Автоматическое распределение задач
     * Запускается каждый день в 6:00 утра (начало рабочего дня)
     * Cron: секунды минуты часы день месяц день_недели
     */
    @Scheduled(cron = "0 0 6 * * MON-FRI") // Каждый будний день в 6:00
    public void distributeTasksAutomatically() {
        System.out.println("🔄 Запуск автоматического распределения задач...");
        
        try {
            // Получаем все нераспределенные задачи
            Iterable<Task> allTasks = taskService.getAllTasks();
            List<Long> unassignedTaskIds = StreamSupport.stream(allTasks.spliterator(), false)
                .filter(task -> task.getUser() == null && task.getStatus() == TaskStatus.NOT_ASSIGNED)
                .map(Task::getId)
                .collect(Collectors.toList());
            
            if (unassignedTaskIds.isEmpty()) {
                System.out.println("ℹ️ Нет нераспределенных задач");
                return;
            }
            
            System.out.println("📋 Найдено нераспределенных задач: " + unassignedTaskIds.size());
            
            // Распределяем задачи без принудительного режима
            Map<String, Object> result = taskService.distributeTasksAutomatically(unassignedTaskIds, false);
            
            if (result.get("success").equals(true)) {
                System.out.println("✅ Распределение завершено успешно");
                System.out.println("   Распределено: " + result.get("distributedCount"));
                System.out.println("   Пропущено: " + result.get("skippedCount"));
            } else {
                System.out.println("⚠️ Распределение завершено с предупреждениями");
                System.out.println("   " + result.get("message"));
            }
            
        } catch (Exception e) {
            System.err.println("❌ Ошибка при автоматическом распределении задач: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Дополнительное распределение в середине дня (если появились новые задачи)
     * Запускается каждый будний день в 14:00
     */
    @Scheduled(cron = "0 0 14 * * MON-FRI")
    public void distributeTasksMidday() {
        System.out.println("🔄 Дополнительное распределение задач (14:00)...");
        distributeTasksAutomatically(); // Используем ту же логику
    }
}

