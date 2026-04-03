package com.foodbridge.web.dto;

public record RegisterRequest(String name, String role, String phone, String email, String password) {
}
