package com.example.demo.dto;

import java.util.Set;

import com.example.demo.models.Role;

public class UserRequestDTO {
    private String username;
    private String password;
    private Role role;
    private Set<Long> workshopIds;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

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