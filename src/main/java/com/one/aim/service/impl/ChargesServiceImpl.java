package com.one.aim.service.impl;

import com.one.aim.bo.CartBO;
import com.one.aim.bo.ProductBO;
import com.one.aim.repo.CartRepo;
import com.one.aim.repo.ProductRepo;
import com.one.aim.service.AdminSettingService;
import com.one.aim.service.ChargesService;
import com.one.utils.AuthUtils;
import com.one.vm.utils.ResponseUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChargesServiceImpl implements ChargesService {

    private final CartRepo cartRepo;
    private final AdminSettingService adminSettingService;
    private final ProductRepo productRepo;

    /**
     * Calculate all charges for cart items
     * Returns comprehensive breakdown including category-wise taxes
     */
    @Override
    public ResponseEntity<?> calculate(Long userId, Long addressId, String promoCode) throws Exception {

        // Get user ID from auth if not provided
        if (userId == null) {
            userId = AuthUtils.getLoggedUserId();
        }

        // Fetch active cart items
        List<CartBO> cartItems = cartRepo.findAllByUserAddToCart_IdAndEnabled(userId, true);

        if (cartItems == null || cartItems.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "subtotal", 0,
                    "tax", 0,
                    "shipping", 0,
                    "discount", 0,
                    "paymentCharge", 0,
                    "total", 0,
                    "items", List.of()
            ));
        }

        // Initialize totals
        long subtotal = 0;
        long totalTax = 0;
        long totalShipping = 0;

        Map<String, CategoryCharges> categoryBreakdown = new LinkedHashMap<>();
        List<Map<String, Object>> itemDetails = new ArrayList<>();

        // Process each cart item
        for (CartBO cart : cartItems) {
            ProductBO product = cart.getProduct();

            if (product == null || !product.isActive()) {
                continue;
            }

            int quantity = Math.max(cart.getQuantity(), 1);
            long unitPrice =
                    cart.isOnSale()
                            ? cart.getOfferPrice()
                            : cart.getPrice();

            long itemSubtotal = unitPrice * quantity;

            // Get category
            String category = sanitize(product.getCategoryName());

            // Get category-specific tax and shipping
            double taxPercent = getCategoryTaxPercent(category);
            double shippingPerUnit = getCategoryShipping(category);

            // Calculate item charges
            long itemTax = Math.round(itemSubtotal * taxPercent / 100);
            long itemShipping = Math.round(shippingPerUnit * quantity);

            // Accumulate totals
            subtotal += itemSubtotal;
            totalTax += itemTax;
            totalShipping += itemShipping;

            // Track category breakdown
            categoryBreakdown.computeIfAbsent(category, k -> new CategoryCharges())
                    .addItem(itemSubtotal, itemTax, itemShipping, taxPercent);

            // Add to item details
            itemDetails.add(Map.of(
                    "productId", product.getId(),
                    "productName", product.getName(),
                    "category", product.getCategoryName(),
                    "quantity", quantity,
                    "unitPrice", unitPrice,
                    "itemSubtotal", itemSubtotal,
                    "taxPercent", taxPercent,
                    "itemTax", itemTax,
                    "itemShipping", itemShipping,
                    "itemTotal", itemSubtotal + itemTax + itemShipping
            ));
        }

        // Apply free shipping threshold
        long freeShippingThreshold = adminSettingService.getLongValue(
                "free_shipping_min_order_amount", 500
        );

        if (subtotal >= freeShippingThreshold) {
            totalShipping = 0;
        }

        // Calculate discount
        long discountAmount = 0;
        String discountReason = null;

        if (promoCode != null && !promoCode.isBlank()) {
            // TODO: Implement promo code validation
            discountReason = "Promo code: " + promoCode;
        } else {
            // Apply global discount if enabled
            boolean discountEnabled = adminSettingService.getBooleanValue(
                    "enable_discount_engine", false
            );

            if (discountEnabled) {
                long discountPercent = adminSettingService.getLongValue(
                        "global_discount_percent", 0
                );

                if (discountPercent > 0) {
                    discountAmount = Math.round(subtotal * discountPercent / 100);
                    discountReason = "Platform discount (" + discountPercent + "%)";
                }
            }
        }

        // Payment processing charge (for online payments)
        double paymentChargePercent = adminSettingService.getDoubleValue(
                "payment_charge_percent", 0.0
        );
        long paymentCharge = Math.round(subtotal * paymentChargePercent / 100);

        // Calculate grand total
        long grandTotal = subtotal + totalTax + totalShipping + paymentCharge - discountAmount;

        // Build category breakdown for response
        List<Map<String, Object>> categoryDetails = new ArrayList<>();
        for (Map.Entry<String, CategoryCharges> entry : categoryBreakdown.entrySet()) {
            CategoryCharges charges = entry.getValue();
            categoryDetails.add(Map.of(
                    "category", entry.getKey().replace("_", " ").toUpperCase(),
                    "subtotal", charges.subtotal,
                    "taxPercent", charges.taxPercent,
                    "tax", charges.tax,
                    "shipping", charges.shipping,
                    "total", charges.subtotal + charges.tax + charges.shipping
            ));
        }

        // Build comprehensive response
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("subtotal", subtotal);
        response.put("tax", totalTax);
        response.put("shipping", totalShipping);
        response.put("shippingWaived", subtotal >= freeShippingThreshold);
        response.put("freeShippingThreshold", freeShippingThreshold);
        response.put("discount", discountAmount);
        response.put("discountReason", discountReason);
        response.put("paymentCharge", paymentCharge);
        response.put("total", grandTotal);
        response.put("itemCount", cartItems.size());
        response.put("categoryBreakdown", categoryDetails);
        response.put("items", itemDetails);

        // Add savings summary
        long totalSavings = discountAmount + (subtotal >= freeShippingThreshold ?
                adminSettingService.getLongValue("delivery_charges_fixed", 50) : 0);

        if (totalSavings > 0) {
            response.put("savings", Map.of(
                    "total", totalSavings,
                    "discount", discountAmount,
                    "freeShipping", subtotal >= freeShippingThreshold
            ));
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Get category-specific tax percentage
     */
    private double getCategoryTaxPercent(String category) {
        return adminSettingService.getDoubleValue(
                "tax_" + category,
                adminSettingService.getDoubleValue("default_tax_percent", 0)
        );
    }

    /**
     * Get category-specific shipping charge per unit
     */
    private double getCategoryShipping(String category) {
        return adminSettingService.getDoubleValue(
                "shipping_" + category,
                adminSettingService.getDoubleValue("delivery_charges_fixed", 0)
        );
    }

    /**
     * Sanitize category name for settings lookup
     */
    private String sanitize(String category) {
        return Optional.ofNullable(category)
                .orElse("")
                .toLowerCase()
                .replace(" ", "_")
                .replaceAll("[^a-z0-9_]", "");
    }

    /**
     * Helper class to track charges per category
     */
    private static class CategoryCharges {
        long subtotal = 0;
        long tax = 0;
        long shipping = 0;
        double taxPercent = 0;

        void addItem(long itemSubtotal, long itemTax, long itemShipping, double percent) {
            this.subtotal += itemSubtotal;
            this.tax += itemTax;
            this.shipping += itemShipping;
            this.taxPercent = percent; // Last one for category (should be consistent)
        }
    }
}