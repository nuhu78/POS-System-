package com.pos.backend.dto;

import lombok.Data;

@Data
public class CheckoutItemRequest {
    private String barcode; // Captures physical scanner gun input
    private Integer quantity;
}