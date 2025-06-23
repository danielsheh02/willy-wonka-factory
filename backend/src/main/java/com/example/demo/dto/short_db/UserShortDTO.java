package com.example.demo.dto.short_db;

import com.example.demo.models.Role;

public class UserShortDTO {
    private Long id;
    private String username;
    private Role role;
    private Boolean isBanned;

    public UserShortDTO() {
    }

    public UserShortDTO(Long id, String username, Role role, Boolean isBanned) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.isBanned = isBanned;
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

    public Boolean getIsBanned() {
        return isBanned;
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

    public void setIsBanned(Boolean isBanned) {
        this.isBanned = isBanned;
    }
}