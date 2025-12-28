package com.example.demo.dto.request;

import com.example.demo.models.ExcursionStatus;

import java.time.LocalDateTime;
import java.util.List;

public class ExcursionRequestDTO {
    private Long excursionId; // Для проверки при редактировании (исключаем текущую экскурсию из конфликтов)
    private String name;
    private LocalDateTime startTime;
    private Integer participantsCount;
    private Long guideId;
    private ExcursionStatus status;
    private List<RoutePointDTO> routes; // Для ручного создания маршрута
    private Boolean autoGenerateRoute; // Флаг для автоматического построения маршрута
    private Integer minRequiredWorkshops; // Минимальное количество цехов для посещения (null = максимально возможное)

    public static class RoutePointDTO {
        private Long workshopId;
        private Integer orderNumber;
        private Integer durationMinutes;

        public RoutePointDTO() {
        }

        public RoutePointDTO(Long workshopId, Integer orderNumber, Integer durationMinutes) {
            this.workshopId = workshopId;
            this.orderNumber = orderNumber;
            this.durationMinutes = durationMinutes;
        }

        public Long getWorkshopId() {
            return workshopId;
        }

        public void setWorkshopId(Long workshopId) {
            this.workshopId = workshopId;
        }

        public Integer getOrderNumber() {
            return orderNumber;
        }

        public void setOrderNumber(Integer orderNumber) {
            this.orderNumber = orderNumber;
        }

        public Integer getDurationMinutes() {
            return durationMinutes;
        }

        public void setDurationMinutes(Integer durationMinutes) {
            this.durationMinutes = durationMinutes;
        }
    }

    public ExcursionRequestDTO() {
    }

    // Getters and Setters
    public Long getExcursionId() {
        return excursionId;
    }

    public void setExcursionId(Long excursionId) {
        this.excursionId = excursionId;
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

    public ExcursionStatus getStatus() {
        return status;
    }

    public void setStatus(ExcursionStatus status) {
        this.status = status;
    }

    public List<RoutePointDTO> getRoutes() {
        return routes;
    }

    public void setRoutes(List<RoutePointDTO> routes) {
        this.routes = routes;
    }

    public Boolean getAutoGenerateRoute() {
        return autoGenerateRoute;
    }

    public void setAutoGenerateRoute(Boolean autoGenerateRoute) {
        this.autoGenerateRoute = autoGenerateRoute;
    }

    public Integer getMinRequiredWorkshops() {
        return minRequiredWorkshops;
    }

    public void setMinRequiredWorkshops(Integer minRequiredWorkshops) {
        this.minRequiredWorkshops = minRequiredWorkshops;
    }
}

