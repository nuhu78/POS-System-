package com.pos.backend.dto;

import lombok.Data;

@Data
public class CheckoutItemRequest {
    private Long productId;
    private Integer quantity;
}