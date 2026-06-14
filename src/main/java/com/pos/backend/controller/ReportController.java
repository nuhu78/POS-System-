package com.pos.backend.controller;

import com.pos.backend.dto.SalesReportSummary;
import com.pos.backend.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    // Fetch report data: GET http://localhost:8085/api/reports/summary?startDate=2026-06-01&endDate=2026-06-30
    @GetMapping("/summary")
    public ResponseEntity<SalesReportSummary> getSalesSummary(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        // Format dates into full day limits (00:00:00 to 23:59:59)
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        SalesReportSummary report = reportService.generateSummary(startDateTime, endDateTime);
        return ResponseEntity.ok(report);
    }
}