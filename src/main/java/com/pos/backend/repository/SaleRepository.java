package com.pos.backend.repository;

import com.pos.backend.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {

    // 1. Calculate sum total revenue between two timestamps
    @Query("SELECT COALESCE(SUM(s.grandTotal), 0) FROM Sale s WHERE s.transactionTimestamp BETWEEN :start AND :end")
    BigDecimal calculateTotalRevenue(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 2. Count total number of orders in a date range
    @Query("SELECT COUNT(s) FROM Sale s WHERE s.transactionTimestamp BETWEEN :start AND :end")
    Long countTotalTransactions(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 3. Get raw rows grouping payment types with their totals
    @Query("SELECT s.paymentMethod, SUM(s.grandTotal) FROM Sale s WHERE s.transactionTimestamp BETWEEN :start AND :end GROUP BY s.paymentMethod")
    List<Object[]> getPaymentMethodBreakdownRaw(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}