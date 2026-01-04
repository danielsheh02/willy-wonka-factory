package com.example.demo.dto.response;

import java.util.List;
import java.util.Map;

public class ReportStatisticsDTO {
    // Статистика по задачам
    private Long totalTasks;
    private Long completedTasks;
    private Long inProgressTasks;
    private Long notAssignedTasks;
    private Double completionRate;
    private List<WorkerTaskStats> topWorkers;
    
    // Статистика по экскурсиям
    private Long totalExcursions;
    private Long completedExcursions;
    private Long upcomingExcursions;
    private Long totalParticipants;
    private List<WorkshopPopularity> popularWorkshops;
    
    // Статистика по оборудованию
    private Long totalEquipment;
    private Long workingEquipment;
    private Long underRepairEquipment;
    private Long brokenEquipment;
    private Double averageHealth;
    
    // Статистика по золотым билетам
    private Long totalTickets;
    private Long activeTickets;
    private Long bookedTickets;
    private Long usedTickets;
    
    // Детальные данные
    private List<Map<String, Object>> tasksData;
    private List<Map<String, Object>> excursionsData;
    private List<Map<String, Object>> equipmentData;
    
    public ReportStatisticsDTO() {}

    // Getters and Setters
    public Long getTotalTasks() { return totalTasks; }
    public void setTotalTasks(Long totalTasks) { this.totalTasks = totalTasks; }
    
    public Long getCompletedTasks() { return completedTasks; }
    public void setCompletedTasks(Long completedTasks) { this.completedTasks = completedTasks; }
    
    public Long getInProgressTasks() { return inProgressTasks; }
    public void setInProgressTasks(Long inProgressTasks) { this.inProgressTasks = inProgressTasks; }
    
    public Long getNotAssignedTasks() { return notAssignedTasks; }
    public void setNotAssignedTasks(Long notAssignedTasks) { this.notAssignedTasks = notAssignedTasks; }
    
    public Double getCompletionRate() { return completionRate; }
    public void setCompletionRate(Double completionRate) { this.completionRate = completionRate; }
    
    public List<WorkerTaskStats> getTopWorkers() { return topWorkers; }
    public void setTopWorkers(List<WorkerTaskStats> topWorkers) { this.topWorkers = topWorkers; }
    
    public Long getTotalExcursions() { return totalExcursions; }
    public void setTotalExcursions(Long totalExcursions) { this.totalExcursions = totalExcursions; }
    
    public Long getCompletedExcursions() { return completedExcursions; }
    public void setCompletedExcursions(Long completedExcursions) { this.completedExcursions = completedExcursions; }
    
    public Long getUpcomingExcursions() { return upcomingExcursions; }
    public void setUpcomingExcursions(Long upcomingExcursions) { this.upcomingExcursions = upcomingExcursions; }
    
    public Long getTotalParticipants() { return totalParticipants; }
    public void setTotalParticipants(Long totalParticipants) { this.totalParticipants = totalParticipants; }
    
    public List<WorkshopPopularity> getPopularWorkshops() { return popularWorkshops; }
    public void setPopularWorkshops(List<WorkshopPopularity> popularWorkshops) { this.popularWorkshops = popularWorkshops; }
    
    public Long getTotalEquipment() { return totalEquipment; }
    public void setTotalEquipment(Long totalEquipment) { this.totalEquipment = totalEquipment; }
    
    public Long getWorkingEquipment() { return workingEquipment; }
    public void setWorkingEquipment(Long workingEquipment) { this.workingEquipment = workingEquipment; }
    
    public Long getUnderRepairEquipment() { return underRepairEquipment; }
    public void setUnderRepairEquipment(Long underRepairEquipment) { this.underRepairEquipment = underRepairEquipment; }
    
    public Long getBrokenEquipment() { return brokenEquipment; }
    public void setBrokenEquipment(Long brokenEquipment) { this.brokenEquipment = brokenEquipment; }
    
    public Double getAverageHealth() { return averageHealth; }
    public void setAverageHealth(Double averageHealth) { this.averageHealth = averageHealth; }
    
    public Long getTotalTickets() { return totalTickets; }
    public void setTotalTickets(Long totalTickets) { this.totalTickets = totalTickets; }
    
    public Long getActiveTickets() { return activeTickets; }
    public void setActiveTickets(Long activeTickets) { this.activeTickets = activeTickets; }
    
    public Long getBookedTickets() { return bookedTickets; }
    public void setBookedTickets(Long bookedTickets) { this.bookedTickets = bookedTickets; }
    
    public Long getUsedTickets() { return usedTickets; }
    public void setUsedTickets(Long usedTickets) { this.usedTickets = usedTickets; }
    
    public List<Map<String, Object>> getTasksData() { return tasksData; }
    public void setTasksData(List<Map<String, Object>> tasksData) { this.tasksData = tasksData; }
    
    public List<Map<String, Object>> getExcursionsData() { return excursionsData; }
    public void setExcursionsData(List<Map<String, Object>> excursionsData) { this.excursionsData = excursionsData; }
    
    public List<Map<String, Object>> getEquipmentData() { return equipmentData; }
    public void setEquipmentData(List<Map<String, Object>> equipmentData) { this.equipmentData = equipmentData; }
    
    // Вложенные классы для статистики
    public static class WorkerTaskStats {
        private Long userId;
        private String username;
        private Long completedTasks;
        private Long totalTasks;
        
        public WorkerTaskStats(Long userId, String username, Long completedTasks, Long totalTasks) {
            this.userId = userId;
            this.username = username;
            this.completedTasks = completedTasks;
            this.totalTasks = totalTasks;
        }
        
        public Long getUserId() { return userId; }
        public String getUsername() { return username; }
        public Long getCompletedTasks() { return completedTasks; }
        public Long getTotalTasks() { return totalTasks; }
    }
    
    public static class WorkshopPopularity {
        private Long workshopId;
        private String workshopName;
        private Long visitCount;
        
        public WorkshopPopularity(Long workshopId, String workshopName, Long visitCount) {
            this.workshopId = workshopId;
            this.workshopName = workshopName;
            this.visitCount = visitCount;
        }
        
        public Long getWorkshopId() { return workshopId; }
        public String getWorkshopName() { return workshopName; }
        public Long getVisitCount() { return visitCount; }
    }
}

