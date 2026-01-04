package com.example.demo.models;

import com.example.demo.utils.DateTimeUtils;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "excursions")
public class Excursion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "participants_count", nullable = false)
    private Integer participantsCount;

    @ManyToOne
    @JoinColumn(name = "guide_id", nullable = false)
    private User guide;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExcursionStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "excursion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExcursionRoute> routes = new ArrayList<>();

    @PrePersist
    public void onCreate() {
        createdAt = DateTimeUtils.nowUTC();
        if (status == null) {
            status = ExcursionStatus.DRAFT;
        }
    }

    // Constructors
    public Excursion() {
    }

    public Excursion(String name, LocalDateTime startTime, Integer participantsCount, User guide) {
        this.name = name;
        this.startTime = startTime;
        this.participantsCount = participantsCount;
        this.guide = guide;
        this.status = ExcursionStatus.DRAFT;
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

    public Integer getParticipantsCount() {
        return participantsCount;
    }

    public void setParticipantsCount(Integer participantsCount) {
        this.participantsCount = participantsCount;
    }

    public User getGuide() {
        return guide;
    }

    public void setGuide(User guide) {
        this.guide = guide;
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

    public List<ExcursionRoute> getRoutes() {
        return routes;
    }

    public void setRoutes(List<ExcursionRoute> routes) {
        this.routes = routes;
    }
}

