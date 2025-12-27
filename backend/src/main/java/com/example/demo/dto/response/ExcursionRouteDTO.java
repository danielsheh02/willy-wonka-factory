package com.example.demo.dto.response;

import java.time.LocalDateTime;

public class ExcursionRouteDTO {
    private Long id;
    private Long workshopId;
    private String workshopName;
    private Integer orderNumber;
    private LocalDateTime startTime;
    private Integer durationMinutes;

    public ExcursionRouteDTO() {
    }

    public ExcursionRouteDTO(Long id, Long workshopId, String workshopName, Integer orderNumber, 
                            LocalDateTime startTime, Integer durationMinutes) {
        this.id = id;
        this.workshopId = workshopId;
        this.workshopName = workshopName;
        this.orderNumber = orderNumber;
        this.startTime = startTime;
        this.durationMinutes = durationMinutes;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWorkshopId() {
        return workshopId;
    }

    public void setWorkshopId(Long workshopId) {
        this.workshopId = workshopId;
    }

    public String getWorkshopName() {
        return workshopName;
    }

    public void setWorkshopName(String workshopName) {
        this.workshopName = workshopName;
    }

    public Integer getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(Integer orderNumber) {
        this.orderNumber = orderNumber;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }
}

