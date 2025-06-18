package com.example.demo.models;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @ManyToMany(mappedBy = "foremans")
    @JsonIgnore
    private Set<Workshop> workshops;

    public User() {
    }

    public User(String username, Role role, Set<Workshop> workshops) {
        this.username = username;
        this.role = role;
        this.workshops = workshops;
    }

    public Set<Workshop> getWorkshops() {
        return workshops;
    }

    public void setWorkshops(Set<Workshop> workshops) {
        this.workshops = workshops;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public Role getRole() {
        return role;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
