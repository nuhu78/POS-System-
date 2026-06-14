package com.pos.backend.service;

import com.pos.backend.dto.CheckoutItemRequest;
import com.pos.backend.dto.CheckoutRequest;
import com.pos.backend.entity.Product;
import com.pos.backend.entity.Sale;
import com.pos.backend.entity.SaleItem;
import com.pos.backend.repository.ProductRepository;
import com.pos.backend.repository.SaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SaleService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;

    public SaleService(SaleRepository saleRepository, ProductRepository productRepository) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
    }

    @Transactional // CRITICAL: Rolls back all database alterations if an exception triggers midway!
    public Sale processCheckout(CheckoutRequest request, String cashierUsername) {
        // Validate that the incoming request contains items to purchase
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("Transaction Rejected: Shopping cart cannot be empty.");
        }

        Sale sale = new Sale();
        
        // 1. Generate unique invoice number tracking sequence
        String invoiceNum = "INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        sale.setInvoiceNumber(invoiceNum);
        sale.setTransactionTimestamp(LocalDateTime.now());
        sale.setPaymentMethod(request.getPaymentMethod());
        sale.setProcessedBy(cashierUsername);
        
        BigDecimal discount = request.getDiscountAmount() != null ? request.getDiscountAmount() : BigDecimal.ZERO;
        sale.setDiscountAmount(discount);

        BigDecimal subTotal = BigDecimal.ZERO;
        List<SaleItem> saleItems = new ArrayList<>();

        // 2. Loop through every requested item line in the cart
        for (CheckoutItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product ID not found: " + itemReq.getProductId()));

            // Stock Check validation
            if (product.getStockQuantity() < itemReq.getQuantity()) {
                throw new RuntimeException("Transaction Rejected: Insufficient stock available for product: " + product.getName() 
                        + " (Available: " + product.getStockQuantity() + ", Requested: " + itemReq.getQuantity() + ")");
            }

            // Deduct stock balance from inventory repository
            product.setStockQuantity(product.getStockQuantity() - itemReq.getQuantity());
            productRepository.save(product);

            // Compute line item prices safely using financial precision models
            BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            subTotal = subTotal.add(itemTotal);

            // Construct relational SaleItem breakdown record
            SaleItem saleItem = new SaleItem();
            saleItem.setSale(sale);
            saleItem.setProduct(product);
            saleItem.setQuantity(itemReq.getQuantity());
            saleItem.setUnitPrice(product.getPrice());
            saleItem.setTotalPrice(itemTotal);
            
            saleItems.add(saleItem);
        }

        // 3. Finalize Master Bill Calculations
        sale.setSubTotal(subTotal);
        BigDecimal grandTotal = subTotal.subtract(discount);
        sale.setGrandTotal(grandTotal.max(BigDecimal.ZERO)); // Enforce floor limit boundary at zero

        // 🚨 FINANCIAL PROTECTION GUARD: Reject payments less than the final bill amount
        BigDecimal paidAmount = request.getAmountPaid() != null ? request.getAmountPaid() : BigDecimal.ZERO;
        if (paidAmount.compareTo(sale.getGrandTotal()) < 0) {
            throw new RuntimeException("Transaction Rejected: Insufficient payment matching. Grand Total is " 
                    + sale.getGrandTotal() + " but only " + paidAmount + " was tendered.");
        }

        sale.setAmountPaid(paidAmount);
        
        // Compute change return values safely
        BigDecimal change = paidAmount.subtract(sale.getGrandTotal()); 
        sale.setChangeAmount(change);

        sale.setItems(saleItems);

        // 4. Save structured relational records inside database
        return saleRepository.save(sale);
    }
}