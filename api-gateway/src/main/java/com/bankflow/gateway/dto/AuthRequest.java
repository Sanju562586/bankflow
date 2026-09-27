package com.bankflow.gateway.dto;

import java.util.List;

public class AuthRequest {
    private String username;
    private String password;
    private String role = "ROLE_CUSTOMER";

    public AuthRequest() {}

    public AuthRequest(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = role != null ? role : "ROLE_CUSTOMER";
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
