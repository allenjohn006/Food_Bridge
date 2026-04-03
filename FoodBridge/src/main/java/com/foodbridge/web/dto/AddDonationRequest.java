package com.foodbridge.web.dto;

public record AddDonationRequest(
        int donorId,
        String itemName,
        String quantity,
        String expiryAt,
        Integer requestId
) {
}
