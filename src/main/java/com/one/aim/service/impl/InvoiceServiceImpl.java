package com.one.aim.service.impl;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import java.util.Base64;

import com.itextpdf.html2pdf.HtmlConverter;
import com.one.aim.bo.*;
import com.one.aim.repo.InvoiceRepo;
import com.one.aim.service.AdminSettingService;
import com.one.aim.service.FileService;
import com.one.utils.AuthUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import com.one.aim.repo.OrderRepo;
import com.one.aim.service.InvoiceService;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepo invoiceRepo;
    private final OrderRepo orderRepo;
    private final FileService fileService;
    private final ResourceLoader resourceLoader;
    private final AdminSettingService adminSettingService;



    @Value("${invoice.template.path}")
    private String templatePath;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy");

    // ==============================================================  SAFE HELPERS
    private String safe(String s) {
        return s == null ? "" : s
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private UserBO safeUser(UserBO u) { return u == null ? new UserBO() : u; }
    private AddressBO safeAddress(AddressBO a) { return a == null ? new AddressBO() : a; }

    private String sanitize(String category) {
        return Optional.ofNullable(category)
                .orElse("")
                .toLowerCase()
                .replace(" ", "_")
                .replaceAll("[^a-z0-9_]", "");
    }

    // ==============================================================  HSN CODE MAPPING
    private String getHsnCode(String category) {
        Map<String, String> hsnMap = Map.of(
                "electronics", "8517",
                "fashion", "6203",
                "grocery", "1006",
                "home_garden", "9403",
                "sports_outdoors", "9506",
                "toys_games", "9503",
                "health_beauty", "3304"
        );
        return hsnMap.getOrDefault(sanitize(category), "9999");
    }

    // ==============================================================  CUSTOMER SECTIONS
    private String buildFullCustomerSection(UserBO user, AddressBO addr) {
        user = safeUser(user);
        addr = safeAddress(addr);

        return """
            <div class="info-box">
                <h3>Bill To</h3>
                <p><strong>%s</strong></p>
                <p>%s</p>
                <p>%s, %s - %s</p>
                <p>%s</p>
                <p><strong>Phone:</strong> %s</p>
                <p><strong>Email:</strong> %s</p>
            </div>
        """.formatted(
                safe(user.getFullName()),
                safe(addr.getStreet()),
                safe(addr.getCity()),
                safe(addr.getState()),
                safe(addr.getZip()),
                safe(addr.getCountry()),
                safe(addr.getPhone()),
                safe(user.getEmail())
        );
    }

    private String buildMaskedCustomerSection(AddressBO addr) {
        addr = safeAddress(addr);

        return """
            <div class="info-box">
                <h3>Bill To</h3>
                <p><strong>Customer</strong></p>
                <p>%s - %s</p>
                <p><strong>Phone:</strong> %s</p>
            </div>
        """.formatted(
                safe(addr.getCity()),
                safe(addr.getZip()),
                safe(addr.getPhone())
        );
    }

    // ==============================================================  SELLER BLOCK
    private String buildMultiSellerSection(OrderBO order) {
        if (order == null || order.getOrderItems() == null)
            return "<div class='info-box'><h3>Seller Details</h3><p>Not Available</p></div>";

        String role = AuthUtils.getLoggedUserRole();

        Map<Long, SellerBO> sellers = new LinkedHashMap<>();
        for (OrderItemBO item : order.getOrderItems()) {
            if (item != null && item.getProduct() != null) {
                SellerBO s = item.getProduct().getSeller();
                if (s != null) sellers.put(s.getId(), s);
            }
        }

        if (sellers.isEmpty())
            return "<div class='info-box'><h3>Seller Details</h3><p>Not Available</p></div>";

        StringBuilder sb = new StringBuilder();
        sb.append("<div class='info-box'><h3>Sold By</h3>");

        for (SellerBO s : sellers.values()) {
            sb.append("<p><strong>").append(safe(s.getFullName())).append("</strong></p>");
            sb.append("<p>GST: ").append(safe(s.getGst())).append("</p>");
            sb.append("<p>Email: ").append(safe(s.getEmail())).append("</p>");

            if ("ADMIN".equalsIgnoreCase(role)) {
                sb.append("<p>Phone: ").append(safe(s.getPhoneNo())).append("</p>");
            }
            sb.append("<br>");
        }

        sb.append("</div>");
        return sb.toString();
    }

    // ==============================================================  BUILD TAX BREAKDOWN
    private String buildTaxBreakdown(OrderBO order) {
        Map<String, TaxSummary> categoryTaxes = new LinkedHashMap<>();

        for (OrderItemBO item : order.getOrderItems()) {
            String category = sanitize(item.getProductCategory());
            double taxPercent = getTaxPercent(category);

            long itemTotal = item.getTotalPrice();
            long itemTax = Math.round(itemTotal * taxPercent / 100);

            categoryTaxes.computeIfAbsent(category, k -> new TaxSummary())
                    .addItem(itemTotal, itemTax, taxPercent);
        }

        if (categoryTaxes.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("<div class='tax-breakdown'>");
        sb.append("<h4>Tax Breakdown (Category-wise)</h4>");

        for (Map.Entry<String, TaxSummary> entry : categoryTaxes.entrySet()) {
            TaxSummary summary = entry.getValue();
            sb.append("<div class='tax-breakdown-item'>");
            sb.append("<span>").append(entry.getKey().replace("_", " ").toUpperCase())
                    .append(" (").append(String.format("%.2f", summary.taxPercent)).append("% GST)</span>");
            sb.append("<span>₹ ").append(summary.totalTax).append("</span>");
            sb.append("</div>");
        }

        sb.append("</div>");
        return sb.toString();
    }

    // Tax Summary Helper Class
    private static class TaxSummary {
        long totalAmount = 0;
        long totalTax = 0;
        double taxPercent = 0;

        void addItem(long amount, long tax, double percent) {
            this.totalAmount += amount;
            this.totalTax += tax;
            this.taxPercent = percent; // Last one wins (should be same for category)
        }
    }

    // ==============================================================  CALCULATE CATEGORY TAX
    private double getTaxPercent(String category) {
        return adminSettingService.getDoubleValue(
                "tax_" + category,
                adminSettingService.getDoubleValue("default_tax_percent", 0)
        );
    }

    // ==============================================================  USER/ADMIN FULL HTML
    @Override
    public String downloadInvoiceHtml(String orderId) throws Exception {

        OrderBO order = orderRepo.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        String html = loadTemplate();
        UserBO user = safeUser(order.getUser());
        AddressBO addr = safeAddress(order.getShippingAddress());

        // Build order items with category-based tax
        long subTotal = 0;
        long totalTax = 0;
        StringBuilder rows = new StringBuilder();
        int index = 1;

        for (OrderItemBO item : order.getOrderItems()) {
            String category = sanitize(item.getProductCategory());
            double taxPercent = getTaxPercent(category);

            long itemSubtotal = item.getUnitPrice() * item.getQuantity();
            long itemTax = Math.round(itemSubtotal * taxPercent / 100);
            long itemTotal = itemSubtotal + itemTax;

            subTotal += itemSubtotal;
            totalTax += itemTax;

            rows.append("""
                <tr>
                  <td>%d</td>
                  <td>
                    <div class="item-name">%s</div>
                    <div class="item-category">%s</div>
                  </td>
                  <td>%s</td>
                  <td>₹ %d</td>
                  <td>%d</td>
                  <td>
                    %.2f%%<br>
                    <span class="tax-info">₹ %d</span>
                  </td>
                  <td>₹ %d</td>
                </tr>
            """.formatted(
                    index++,
                    safe(item.getProductName()),
                    safe(item.getProductCategory()),
                    getHsnCode(item.getProductCategory()),
                    item.getUnitPrice(),
                    item.getQuantity(),
                    taxPercent,
                    itemTax,
                    itemTotal
            ));
        }

        // Payment method details
        String paymentDetails = "";
        String paymentMethod = order.getPaymentMethod();
        String paymentStatus = order.getPaymentStatus();
        String paymentStatusClass = getPaymentStatusClass(paymentStatus);

        if ("COD".equalsIgnoreCase(paymentMethod)) {
            paymentDetails = "<p><strong>Note:</strong> Payment will be collected at the time of delivery.</p>";
        } else if ("PAID".equalsIgnoreCase(paymentStatus)) {
            paymentDetails = "<p><strong>Transaction ID:</strong> " + safe(order.getPaymentId()) + "</p>";
            paymentDetails += "<p><strong>Payment Date:</strong> " +
                    (order.getPaymentTime() != null ? order.getPaymentTime().format(DATE_FORMAT) : "N/A") + "</p>";
        }

        // Tax rows for summary
        String taxRows = "<div class='summary-row tax'>" +
                "<span class='summary-label'>Total GST (Incl. All Categories)</span>" +
                "<span class='summary-value'>₹ " + totalTax + "</span>" +
                "</div>";

        // Delivery charge
        long deliveryCharge = order.getDeliveryCharge() != null ? order.getDeliveryCharge() : 0;
        String deliveryChargeStr = deliveryCharge == 0 ? "FREE" : "₹ " + deliveryCharge;

        // Discount
        String discountSection = "";
        long discount = order.getDiscountAmount() != null ? order.getDiscountAmount() : 0;
        if (discount > 0) {
            discountSection = "<div class='summary-row discount'>" +
                    "<span class='summary-label'>Discount Applied</span>" +
                    "<span class='summary-value'>- ₹ " + discount + "</span>" +
                    "</div>";
        }

        // Payment charge
        String paymentChargeSection = "";
        long paymentCharge = order.getPaymentCharge() != null ? order.getPaymentCharge() : 0;
        if (paymentCharge > 0) {
            paymentChargeSection = "<div class='summary-row payment-charge'>" +
                    "<span class='summary-label'>Payment Processing Fee</span>" +
                    "<span class='summary-value'>₹ " + paymentCharge + "</span>" +
                    "</div>";
        }

        long grandTotal = subTotal + totalTax + deliveryCharge + paymentCharge - discount;

        return html
                .replace("@@invoiceNo@@", safe(order.getInvoiceno()))
                .replace("@@orderId@@", safe(order.getOrderId()))
                .replace("@@orderDate@@", order.getOrderTime() == null ?
                        "" : order.getOrderTime().format(DATE_FORMAT))
                .replace("@@customerSection@@", buildFullCustomerSection(user, addr))
                .replace("@@sellerSection@@", buildMultiSellerSection(order))
                .replace("@@paymentMethod@@", safe(paymentMethod))
                .replace("@@paymentStatus@@", safe(paymentStatus))
                .replace("@@paymentStatusClass@@", paymentStatusClass)
                .replace("@@paymentDetails@@", paymentDetails)
                .replace("@@orderItems@@", rows.toString())
                .replace("@@taxBreakdown@@", buildTaxBreakdown(order))
                .replace("@@subTotal@@", String.valueOf(subTotal))
                .replace("@@taxRows@@", taxRows)
                .replace("@@deliveryCharge@@", deliveryChargeStr)
                .replace("@@discountSection@@", discountSection)
                .replace("@@paymentChargeSection@@", paymentChargeSection)
                .replace("@@totalAmount@@", String.valueOf(grandTotal));
    }

    // ==============================================================  SELLER HTML (FILTERED)
    @Override
    public String downloadSellerInvoiceHtml(String orderId, Long sellerDbId) throws Exception {

        OrderBO order = orderRepo.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        List<OrderItemBO> sellerItems = order.getOrderItems().stream()
                .filter(oi -> oi.getProduct() != null
                        && oi.getProduct().getSeller() != null
                        && oi.getProduct().getSeller().getId().equals(sellerDbId))
                .toList();

        if (sellerItems.isEmpty()) return null;

        String html = loadTemplate();
        AddressBO addr = safeAddress(order.getShippingAddress());

        long subTotal = 0;
        long totalTax = 0;
        StringBuilder rows = new StringBuilder();
        int index = 1;

        for (OrderItemBO item : sellerItems) {
            String category = sanitize(item.getProductCategory());
            double taxPercent = getTaxPercent(category);

            long itemSubtotal = item.getUnitPrice() * item.getQuantity();
            long itemTax = Math.round(itemSubtotal * taxPercent / 100);
            long itemTotal = itemSubtotal + itemTax;

            subTotal += itemSubtotal;
            totalTax += itemTax;

            rows.append("""
                <tr>
                  <td>%d</td>
                  <td>
                    <div class="item-name">%s</div>
                    <div class="item-category">%s</div>
                  </td>
                  <td>%s</td>
                  <td>₹ %d</td>
                  <td>%d</td>
                  <td>
                    %.2f%%<br>
                    <span class="tax-info">₹ %d</span>
                  </td>
                  <td>₹ %d</td>
                </tr>
            """.formatted(
                    index++,
                    safe(item.getProductName()),
                    safe(item.getProductCategory()),
                    getHsnCode(item.getProductCategory()),
                    item.getUnitPrice(),
                    item.getQuantity(),
                    taxPercent,
                    itemTax,
                    itemTotal
            ));
        }

        String taxRows = "<div class='summary-row tax'>" +
                "<span class='summary-label'>Total GST</span>" +
                "<span class='summary-value'>₹ " + totalTax + "</span>" +
                "</div>";

        long deliveryCharge = order.getDeliveryCharge() != null ? order.getDeliveryCharge() : 0;
        String deliveryChargeStr = deliveryCharge == 0 ? "FREE" : "₹ " + deliveryCharge;

        long grandTotal = subTotal + totalTax + deliveryCharge;

        return html
                .replace("@@logoPath@@", getLogoPath())
                .replace("@@invoiceNo@@", safe(order.getInvoiceno()))
                .replace("@@orderId@@", safe(order.getOrderId()))
                .replace("@@orderDate@@", order.getOrderTime() == null ?
                        "" : order.getOrderTime().format(DATE_FORMAT))
                .replace("@@customerSection@@", buildMaskedCustomerSection(addr))
                .replace("@@sellerSection@@", "")
                .replace("@@paymentMethod@@", safe(order.getPaymentMethod()))
                .replace("@@paymentStatus@@", safe(order.getPaymentStatus()))
                .replace("@@paymentStatusClass@@", getPaymentStatusClass(order.getPaymentStatus()))
                .replace("@@paymentDetails@@", "")
                .replace("@@orderItems@@", rows.toString())
                .replace("@@taxBreakdown@@", buildTaxBreakdown(order))
                .replace("@@subTotal@@", String.valueOf(subTotal))
                .replace("@@taxRows@@", taxRows)
                .replace("@@deliveryCharge@@", deliveryChargeStr)
                .replace("@@discountSection@@", "")
                .replace("@@paymentChargeSection@@", "")
                .replace("@@totalAmount@@", String.valueOf(grandTotal));
    }

    private String getPaymentStatusClass(String status) {
        if (status == null) return "pending";

        String s = status.toLowerCase();
        if (s.contains("paid") || s.contains("success")) return "paid";
        if (s.contains("cod")) return "cod";
        return "pending";
    }

    // ==============================================================  PDF GENERATION
    @Override
    public byte[] downloadInvoicePdf(String orderId) throws Exception {

        InvoiceBO invoice = invoiceRepo.findByOrder_OrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        return fileService.getContentFromGridFS(
                String.valueOf(invoice.getInvoiceFileId()));
    }

    @Override
    public byte[] downloadSellerInvoicePdf(String orderId, Long sellerDbId) throws Exception {

        String html = downloadSellerInvoiceHtml(orderId, sellerDbId);
        if (html == null) return null;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HtmlConverter.convertToPdf(html, out);
        return out.toByteArray();
    }

    @Override
    public InvoiceBO generateInvoice(String orderId) throws Exception {

        OrderBO order = orderRepo.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (invoiceRepo.findByOrder_OrderId(orderId).isPresent()) {
            return invoiceRepo.findByOrder_OrderId(orderId).get();
        }

        String html = downloadInvoiceHtml(orderId);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HtmlConverter.convertToPdf(html, out);

        FileBO file = fileService.uploadBytes(
                out.toByteArray(),
                order.getInvoiceno() + ".pdf"
        );

        InvoiceBO invoice = InvoiceBO.builder()
                .order(order)
                .user(order.getUser())
                .invoiceFileId(file.getId())
                .invoiceNumber(order.getInvoiceno())
                .build();

        return invoiceRepo.save(invoice);
    }

    @Override
    public InvoiceBO getInvoiceByOrderId(String orderId) {
        return invoiceRepo.findByOrder_OrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));
    }

    @Override
    public List<InvoiceBO> getAllInvoicesForAdmin() {
        return invoiceRepo.findAll();
    }

    @Override
    public List<InvoiceBO> getInvoicesForUser(Long userId) {
        return invoiceRepo.findByUser_Id(userId);
    }

    @Override
    public List<InvoiceBO> getInvoicesForSeller(Long sellerDbId) {
        return invoiceRepo.findInvoicesForSeller(sellerDbId);
    }

    @Override
    public byte[] downloadAdminInvoice(String orderId) throws Exception {
        String html = downloadInvoiceHtml(orderId);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HtmlConverter.convertToPdf(html, out);
        return out.toByteArray();
    }

    private String loadTemplate() throws Exception {
        Resource resource = resourceLoader.getResource("classpath:" + templatePath);
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String getLogoPath() throws Exception {
        Resource resource = resourceLoader.getResource("classpath:static/image/logo.jpg");
        try (InputStream inputStream = resource.getInputStream()) {
            byte[] imageBytes = inputStream.readAllBytes();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            return "data:image/jpeg;base64," + base64Image;
        }
    }

}