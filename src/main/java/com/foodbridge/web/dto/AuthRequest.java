package com.foodbridge.web.dto;

public record AuthRequest(String email, String password, String role) {
}
