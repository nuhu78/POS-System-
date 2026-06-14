package com.pos.backend.service;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @Transactional // CRITICAL: Rolls back database rows if an item is out of stock or unregistered
    public Sale processCheckout(CheckoutRequest request, String cashierUsername) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("Transaction Rejected: Shopping cart cannot be empty.");
        }

        Sale sale = new Sale();
        
        // 1. Generate unique invoice sequence tracking token
        String invoiceNum = "INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        sale.setInvoiceNumber(invoiceNum);
        sale.setTransactionTimestamp(LocalDateTime.now());
        sale.setPaymentMethod(request.getPaymentMethod());
        sale.setProcessedBy(cashierUsername);
        
        BigDecimal discount = request.getDiscountAmount() != null ? request.getDiscountAmount() : BigDecimal.ZERO;
        sale.setDiscountAmount(discount);

        BigDecimal subTotal = BigDecimal.ZERO;
        List<SaleItem> saleItems = new ArrayList<>();

        // 2. Loop through item lines using optimized barcode index scanning lookup
        for (CheckoutItemRequest itemReq : request.getItems()) {
            
            // Query using physical barcode string instead of database internal ID
            Product product = productRepository.findByBarcode(itemReq.getBarcode())
                    .orElseThrow(() -> new RuntimeException("Transaction Rejected: Scanned barcode not registered in inventory: " 
                            + itemReq.getBarcode()));

            // Stock availability check validation
            if (product.getStockQuantity() < itemReq.getQuantity()) {
                throw new RuntimeException("Transaction Rejected: Insufficient stock available for product: " + product.getName() 
                        + " (Available: " + product.getStockQuantity() + ", Requested: " + itemReq.getQuantity() + ")");
            }

            // Deduct stock balance from database entity state
            product.setStockQuantity(product.getStockQuantity() - itemReq.getQuantity());
            productRepository.save(product);

            // Compute line item prices safely using financial math structures
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

        // Financial Underpayment Protection Check
        BigDecimal paidAmount = request.getAmountPaid() != null ? request.getAmountPaid() : BigDecimal.ZERO;
        if (paidAmount.compareTo(sale.getGrandTotal()) < 0) {
            throw new RuntimeException("Transaction Rejected: Insufficient payment matching. Grand Total is " 
                    + sale.getGrandTotal() + " but only " + paidAmount + " was tendered.");
        }

        sale.setAmountPaid(paidAmount);
        
        // Compute precise change values safely
        BigDecimal change = paidAmount.subtract(sale.getGrandTotal()); 
        sale.setChangeAmount(change);

        sale.setItems(saleItems);

        // 4. Save entire object graph atomically into PostgreSQL
        return saleRepository.save(sale);
    }
    @Transactional
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')") // 🚨 OVERRIDE GUARD: Cashiers are completely blocked!
    public void voidSale(Long saleId) {
        // 1. Find the transaction invoice history
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new RuntimeException("Void Rejected: Transaction ID " + saleId + " not found."));

        // 2. Loop through the sale's items and return them back to the inventory stock
        for (SaleItem item : sale.getItems()) {
            Product product = item.getProduct();
            
            // Re-increment stock count by adding the returned quantity back
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            productRepository.save(product);
        }

        // 3. Remove the transaction cluster from our records
        saleRepository.delete(sale);
    }
}