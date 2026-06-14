package com.pos.backend.controller;

import com.pos.backend.dto.CheckoutRequest;
import com.pos.backend.entity.Sale;
import com.pos.backend.service.SaleService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sales")
public class SaleController {

    private final SaleService saleService;

    public SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    // Process a checkout order: POST http://localhost:8085/api/sales/checkout
    @PostMapping("/checkout")
    public ResponseEntity<Sale> checkout(@RequestBody CheckoutRequest request) {
        // Read the logged in cashier's name automatically from the security context token
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        
        Sale completeInvoice = saleService.processCheckout(request, currentUsername);
        return ResponseEntity.ok(completeInvoice);
    }
}