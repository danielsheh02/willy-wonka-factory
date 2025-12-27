package com.example.demo.dto.response;

import com.example.demo.models.ExcursionStatus;

import java.time.LocalDateTime;
import java.util.List;

public class ExcursionResponseDTO {
    private Long id;
    private String name;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer participantsCount;
    private Long guideId;
    private String guideName;
    private ExcursionStatus status;
    private LocalDateTime createdAt;
    private List<ExcursionRouteDTO> routes;

    public ExcursionResponseDTO() {
    }

    public ExcursionResponseDTO(Long id, String name, LocalDateTime startTime, LocalDateTime endTime,
                               Integer participantsCount, Long guideId, String guideName, ExcursionStatus status, 
                               LocalDateTime createdAt, List<ExcursionRouteDTO> routes) {
        this.id = id;
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
        this.participantsCount = participantsCount;
        this.guideId = guideId;
        this.guideName = guideName;
        this.status = status;
        this.createdAt = createdAt;
        this.routes = routes;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Integer getParticipantsCount() {
        return participantsCount;
    }

    public void setParticipantsCount(Integer participantsCount) {
        this.participantsCount = participantsCount;
    }

    public Long getGuideId() {
        return guideId;
    }

    public void setGuideId(Long guideId) {
        this.guideId = guideId;
    }

    public String getGuideName() {
        return guideName;
    }

    public void setGuideName(String guideName) {
        this.guideName = guideName;
    }

    public ExcursionStatus getStatus() {
        return status;
    }

    public void setStatus(ExcursionStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<ExcursionRouteDTO> getRoutes() {
        return routes;
    }

    public void setRoutes(List<ExcursionRouteDTO> routes) {
        this.routes = routes;
    }
}

