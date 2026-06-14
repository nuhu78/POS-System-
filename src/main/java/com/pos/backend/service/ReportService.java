package com.pos.backend.service;

import com.pos.backend.dto.SalesReportSummary;
import com.pos.backend.repository.SaleRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private final SaleRepository saleRepository;

    public ReportService(SaleRepository saleRepository) {
        this.saleRepository = saleRepository;
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')") // 🚨 Guard analytics from basic cashiers
    public SalesReportSummary generateSummary(LocalDateTime start, LocalDateTime end) {
        // Fetch raw metrics from repository layers
        BigDecimal totalRevenue = saleRepository.calculateTotalRevenue(start, end);
        Long totalTransactions = saleRepository.countTotalTransactions(start, end);

        // Calculate Average Order Value (AOV = Total Revenue / Total Transactions)
        BigDecimal averageOrderValue = BigDecimal.ZERO;
        if (totalTransactions > 0) {
            averageOrderValue = totalRevenue.divide(BigDecimal.valueOf(totalTransactions), 2, RoundingMode.HALF_UP);
        }

        // Map the raw dynamic Object array from database records to a structured payment breakdown map
        List<Object[]> rawBreakdown = saleRepository.getPaymentMethodBreakdownRaw(start, end);
        Map<String, BigDecimal> paymentBreakdown = new HashMap<>();
        for (Object[] row : rawBreakdown) {
            String method = (String) row[0];
            BigDecimal sum = (BigDecimal) row[1];
            paymentBreakdown.put(method, sum);
        }

        String periodString = start.toLocalDate() + " to " + end.toLocalDate();
        return new SalesReportSummary(periodString, totalRevenue, totalTransactions, averageOrderValue, paymentBreakdown);
    }
}