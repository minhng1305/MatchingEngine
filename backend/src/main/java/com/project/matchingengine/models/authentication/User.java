package com.project.matchingengine.models.authentication;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {
    @Id
    private UUID userId;
    private String password;
    private String email;

    public User(UUID userId, String password, String email) {
        this.userId = userId;
        this.password = password;
        this.email = email;
    }

    public User() {
        // Default no-arg constructor
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) { this.userId = userId; }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) { this.password = password; }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) { this.email = email; }
}