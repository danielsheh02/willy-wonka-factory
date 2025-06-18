package com.example.demo.dto;

import java.util.Set;

import com.example.demo.models.Role;

public class UserRequestDTO {
    private String username;
    private Role role;
    private Set<Long> workshopIds;

    public Set<Long> getWorkshopsIds() {
        return workshopIds;
    }

    public void setWorkshopsIds(Set<Long> workshopIds) {
        this.workshopIds = workshopIds;
    }

    public String getUsername() {
        return username;
    }

    public Role getRole() {
        return role;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}