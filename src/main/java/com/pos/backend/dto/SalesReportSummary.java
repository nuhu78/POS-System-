package com.pos.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

@Data
@AllArgsConstructor
public class SalesReportSummary {
    private String reportingPeriod;
    private BigDecimal totalRevenue;
    private Long totalTransactionCount;
    private BigDecimal averageOrderValue;
    private Map<String, BigDecimal> paymentMethodBreakdown;
}