package com.foodbridge.web.dto;

public record NgoNeedRequest(int ngoId, String itemName, String quantityNeeded, String notes) {
}
