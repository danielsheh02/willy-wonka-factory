package com.example.demo.dto.response;

import com.example.demo.models.TicketStatus;
import java.time.LocalDateTime;

public class GoldenTicketResponseDTO {
    private Long id;
    private String ticketNumber;
    private TicketStatus status;
    private Long excursionId;
    private String excursionName;
    private LocalDateTime excursionStartTime;
    private String holderName;
    private String holderEmail;
    private String holderPhone;
    private LocalDateTime generatedAt;
    private LocalDateTime bookedAt;
    private LocalDateTime usedAt;
    private LocalDateTime expiresAt;

    public GoldenTicketResponseDTO() {
    }

    public GoldenTicketResponseDTO(Long id, String ticketNumber, TicketStatus status,
                                  Long excursionId, String excursionName, LocalDateTime excursionStartTime,
                                  String holderName, String holderEmail, String holderPhone,
                                  LocalDateTime generatedAt, LocalDateTime bookedAt,
                                  LocalDateTime usedAt, LocalDateTime expiresAt) {
        this.id = id;
        this.ticketNumber = ticketNumber;
        this.status = status;
        this.excursionId = excursionId;
        this.excursionName = excursionName;
        this.excursionStartTime = excursionStartTime;
        this.holderName = holderName;
        this.holderEmail = holderEmail;
        this.holderPhone = holderPhone;
        this.generatedAt = generatedAt;
        this.bookedAt = bookedAt;
        this.usedAt = usedAt;
        this.expiresAt = expiresAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public void setTicketNumber(String ticketNumber) {
        this.ticketNumber = ticketNumber;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public Long getExcursionId() {
        return excursionId;
    }

    public void setExcursionId(Long excursionId) {
        this.excursionId = excursionId;
    }

    public String getExcursionName() {
        return excursionName;
    }

    public void setExcursionName(String excursionName) {
        this.excursionName = excursionName;
    }

    public LocalDateTime getExcursionStartTime() {
        return excursionStartTime;
    }

    public void setExcursionStartTime(LocalDateTime excursionStartTime) {
        this.excursionStartTime = excursionStartTime;
    }

    public String getHolderName() {
        return holderName;
    }

    public void setHolderName(String holderName) {
        this.holderName = holderName;
    }

    public String getHolderEmail() {
        return holderEmail;
    }

    public void setHolderEmail(String holderEmail) {
        this.holderEmail = holderEmail;
    }

    public String getHolderPhone() {
        return holderPhone;
    }

    public void setHolderPhone(String holderPhone) {
        this.holderPhone = holderPhone;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public LocalDateTime getBookedAt() {
        return bookedAt;
    }

    public void setBookedAt(LocalDateTime bookedAt) {
        this.bookedAt = bookedAt;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(LocalDateTime usedAt) {
        this.usedAt = usedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}

