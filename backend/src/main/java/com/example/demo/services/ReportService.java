package com.example.demo.services;

import com.example.demo.dto.response.ReportStatisticsDTO;
import com.example.demo.models.*;
import com.example.demo.repositories.*;
import com.example.demo.utils.DateTimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    @Autowired
    private TaskRepository taskRepository;
    
    @Autowired
    private ExcursionRepository excursionRepository;
    
    @Autowired
    private EquipmentRepository equipmentRepository;
    
    @Autowired
    private GoldenTicketRepository ticketRepository;
    
    @Autowired
    private ExcursionRouteRepository routeRepository;
    
    @Autowired
    private UserRepository userRepository;

    public ReportStatisticsDTO generateReport(LocalDateTime startDate, LocalDateTime endDate) {
        if (!startDate.isBefore(endDate)) {
            throw new IllegalArgumentException("startDate must be strictly before endDate");
        }
        ReportStatisticsDTO report = new ReportStatisticsDTO();
        
        // Статистика по задачам
        List<Task> allTasks = (List<Task>) taskRepository.findAll();
        List<Task> periodTasks = allTasks.stream()
            .filter(t -> t.getCreatedAt() != null && 
                        t.getCreatedAt().isAfter(startDate) && 
                        t.getCreatedAt().isBefore(endDate))
            .collect(Collectors.toList());
        
        report.setTotalTasks((long) periodTasks.size());
        report.setCompletedTasks(periodTasks.stream()
            .filter(t -> t.getStatus() == TaskStatus.COMPLETED)
            .count());
        report.setInProgressTasks(periodTasks.stream()
            .filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS)
            .count());
        report.setNotAssignedTasks(periodTasks.stream()
            .filter(t -> t.getStatus() == TaskStatus.NOT_ASSIGNED)
            .count());
        
        if (report.getTotalTasks() > 0) {
            report.setCompletionRate((double) report.getCompletedTasks() / report.getTotalTasks() * 100);
        } else {
            report.setCompletionRate(0.0);
        }
        
        // Топ рабочих по выполненным задачам
        Map<Long, List<Task>> tasksByUser = periodTasks.stream()
            .filter(t -> t.getUser() != null)
            .collect(Collectors.groupingBy(t -> t.getUser().getId()));
        
        List<ReportStatisticsDTO.WorkerTaskStats> topWorkers = tasksByUser.entrySet().stream()
            .map(entry -> {
                Long userId = entry.getKey();
                List<Task> userTasks = entry.getValue();
                User user = userRepository.findById(userId).orElse(null);
                if (user == null) return null;
                
                long completed = userTasks.stream()
                    .filter(t -> t.getStatus() == TaskStatus.COMPLETED)
                    .count();
                
                return new ReportStatisticsDTO.WorkerTaskStats(
                    userId,
                    user.getUsername(),
                    completed,
                    (long) userTasks.size()
                );
            })
            .filter(Objects::nonNull)
            .sorted((a, b) -> Long.compare(b.getCompletedTasks(), a.getCompletedTasks()))
            .limit(5)
            .collect(Collectors.toList());
        report.setTopWorkers(topWorkers);
        
        // Статистика по экскурсиям
        List<Excursion> allExcursions = excursionRepository.findAll();
        List<Excursion> periodExcursions = allExcursions.stream()
            .filter(e -> e.getCreatedAt() != null && 
                        e.getCreatedAt().isAfter(startDate) && 
                        e.getCreatedAt().isBefore(endDate))
            .collect(Collectors.toList());
        
        report.setTotalExcursions((long) periodExcursions.size());
        report.setCompletedExcursions(periodExcursions.stream()
            .filter(e -> e.getStatus() == ExcursionStatus.COMPLETED)
            .count());
        report.setUpcomingExcursions(periodExcursions.stream()
            .filter(e -> e.getStartTime().isAfter(DateTimeUtils.nowUTC()))
            .count());
        
        // Считаем реальное количество посетителей по забронированным билетам
        Set<Long> periodExcursionIds = periodExcursions.stream()
            .map(e -> e.getId())
            .collect(Collectors.toSet());
        long totalParticipants = ticketRepository.findAll().stream()
            .filter(t -> t.getStatus() == TicketStatus.BOOKED && 
                        t.getExcursion() != null && 
                        periodExcursionIds.contains(t.getExcursion().getId()))
            .count();
        report.setTotalParticipants(totalParticipants);
        
        // Популярные цеха
        List<ExcursionRoute> allRoutes = routeRepository.findAll();
        Map<Long, Long> workshopVisits = allRoutes.stream()
            .filter(r -> r.getExcursion().getCreatedAt() != null &&
                        r.getExcursion().getCreatedAt().isAfter(startDate) &&
                        r.getExcursion().getCreatedAt().isBefore(endDate))
            .collect(Collectors.groupingBy(
                r -> r.getWorkshop().getId(),
                Collectors.counting()
            ));
        
        List<ReportStatisticsDTO.WorkshopPopularity> popularWorkshops = workshopVisits.entrySet().stream()
            .map(entry -> {
                Long workshopId = entry.getKey();
                ExcursionRoute route = allRoutes.stream()
                    .filter(r -> r.getWorkshop().getId().equals(workshopId))
                    .findFirst()
                    .orElse(null);
                if (route == null) return null;
                
                return new ReportStatisticsDTO.WorkshopPopularity(
                    workshopId,
                    route.getWorkshop().getName(),
                    entry.getValue()
                );
            })
            .filter(Objects::nonNull)
            .sorted((a, b) -> Long.compare(b.getVisitCount(), a.getVisitCount()))
            .limit(5)
            .collect(Collectors.toList());
        report.setPopularWorkshops(popularWorkshops);
        
        // Статистика по оборудованию
        List<Equipment> allEquipment = (List<Equipment>) equipmentRepository.findAll();
        report.setTotalEquipment((long) allEquipment.size());
        report.setWorkingEquipment(allEquipment.stream()
            .filter(e -> e.getStatus() == EquipmentStatus.WORKING)
            .count());
        report.setUnderRepairEquipment(allEquipment.stream()
            .filter(e -> e.getStatus() == EquipmentStatus.UNDER_REPAIR)
            .count());
        report.setBrokenEquipment(allEquipment.stream()
            .filter(e -> e.getStatus() == EquipmentStatus.BROKEN)
            .count());
        
        OptionalDouble avgHealth = allEquipment.stream()
            .filter(e -> Objects.nonNull(e.getHealth()))
            .mapToInt(Equipment::getHealth)
            .average();
        report.setAverageHealth(avgHealth.isPresent() ? avgHealth.getAsDouble() : 0.0);
        
        // Статистика по золотым билетам
        List<GoldenTicket> allTickets = ticketRepository.findAll();
        List<GoldenTicket> periodTickets = allTickets.stream()
            .filter(t -> t.getGeneratedAt() != null && 
                        t.getGeneratedAt().isAfter(startDate) && 
                        t.getGeneratedAt().isBefore(endDate))
            .collect(Collectors.toList());
        
        report.setTotalTickets((long) periodTickets.size());
        report.setActiveTickets(periodTickets.stream()
            .filter(t -> t.getStatus() == TicketStatus.ACTIVE)
            .count());
        report.setBookedTickets(periodTickets.stream()
            .filter(t -> t.getStatus() == TicketStatus.BOOKED)
            .count());
        report.setUsedTickets(periodTickets.stream()
            .filter(t -> t.getStatus() == TicketStatus.USED)
            .count());
        
        // Детальные данные для таблиц
        report.setTasksData(convertTasksToMap(periodTasks));
        report.setExcursionsData(convertExcursionsToMap(periodExcursions));
        report.setEquipmentData(convertEquipmentToMap(allEquipment));
        
        return report;
    }
    
    private List<Map<String, Object>> convertTasksToMap(List<Task> tasks) {
        return tasks.stream().map(task -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", task.getId());
            map.put("name", task.getName());
            map.put("description", task.getDescription());
            map.put("status", task.getStatus().name());
            map.put("username", task.getUser() != null ? task.getUser().getUsername() : "Не назначена");
            map.put("createdAt", task.getCreatedAt());
            map.put("completedAt", task.getCompletedAt());
            return map;
        }).collect(Collectors.toList());
    }
    
    private List<Map<String, Object>> convertExcursionsToMap(List<Excursion> excursions) {
        return excursions.stream().map(exc -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", exc.getId());
            map.put("name", exc.getName());
            map.put("startTime", exc.getStartTime());
            map.put("participantsCount", exc.getParticipantsCount());
            map.put("guideName", exc.getGuide().getUsername());
            map.put("status", exc.getStatus().name());
            map.put("workshopsCount", exc.getRoutes().size());
            return map;
        }).collect(Collectors.toList());
    }
    
    private List<Map<String, Object>> convertEquipmentToMap(List<Equipment> equipment) {
        return equipment.stream().map(eq -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", eq.getId());
            map.put("name", eq.getName());
            map.put("model", eq.getModel());
            map.put("status", eq.getStatus().name());
            map.put("health", eq.getHealth());
            map.put("temperature", eq.getTemperature());
            map.put("workshopName", eq.getWorkshop() != null ? eq.getWorkshop().getName() : "-");
            return map;
        }).collect(Collectors.toList());
    }
}

