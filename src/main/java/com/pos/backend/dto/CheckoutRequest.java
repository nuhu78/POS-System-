package com.pos.backend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CheckoutRequest {
    private String paymentMethod;
    private BigDecimal discountAmount;
    private BigDecimal amountPaid;
    private List<CheckoutItemRequest> items;
}