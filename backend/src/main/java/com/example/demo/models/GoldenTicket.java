package com.example.demo.models;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;

@Entity
@Table(name = "golden_tickets")
public class GoldenTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_number", nullable = false, unique = true, length = 10)
    private String ticketNumber; // Уникальный короткий номер билета (например: GW4A7K2M)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status;

    @ManyToOne
    @JoinColumn(name = "excursion_id")
    @JsonIgnoreProperties({"routes", "guide"})
    private Excursion excursion; // Экскурсия, на которую забронирован билет (nullable)

    @Column(name = "holder_name", length = 255)
    private String holderName; // Имя владельца билета (опционально)

    @Column(name = "holder_email", length = 255)
    private String holderEmail; // Email владельца (опционально)

    @Column(name = "holder_phone", length = 50)
    private String holderPhone; // Телефон владельца (опционально)

    @Column(name = "generated_at", nullable = false, updatable = false)
    private LocalDateTime generatedAt; // Время генерации

    @Column(name = "booked_at")
    private LocalDateTime bookedAt; // Время бронирования

    @Column(name = "used_at")
    private LocalDateTime usedAt; // Время использования (после экскурсии)

    @Column(name = "expires_at")
    private LocalDateTime expiresAt; // Время истечения (опционально)

    @PrePersist
    protected void onCreate() {
        generatedAt = LocalDateTime.now();
        if (status == null) {
            status = TicketStatus.ACTIVE;
        }
    }

    // Constructors
    public GoldenTicket() {
    }

    public GoldenTicket(String ticketNumber) {
        this.ticketNumber = ticketNumber;
        this.status = TicketStatus.ACTIVE;
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

    public Excursion getExcursion() {
        return excursion;
    }

    public void setExcursion(Excursion excursion) {
        this.excursion = excursion;
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

