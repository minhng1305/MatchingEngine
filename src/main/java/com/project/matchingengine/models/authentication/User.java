package com.project.matchingengine.models.authentication;

import java.util.UUID;

public class User {
    private UUID userId;
    private String username;
    private String password;
    private String email;

    public User(UUID userId, String username, String password, String email) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.email = email;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }
}