package com.one.aim.service.impl;

import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

import com.one.aim.bo.AdminSettingsBO;
import com.one.aim.bo.FileBO;
import com.one.aim.bo.SellerBO;
import com.one.aim.mapper.SellerMapper;
import com.one.aim.repo.AdminSettingsRepo;
import com.one.aim.repo.FileRepo;
import com.one.aim.repo.SellerRepo;
import com.one.aim.rs.SellerRs;
import com.one.aim.service.AdminSettingService;
import com.one.aim.service.EmailService;
import com.one.utils.UrlUtils;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.lowagie.text.Document;

import java.io.ByteArrayOutputStream;


import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminSettingServiceImpl implements AdminSettingService {

    private final AdminSettingsRepo repo;
    private final SellerRepo sellerRepo;
    private final EmailService emailService;
    private final FileRepo fileRepo;
    private final SellerMapper sellerMapper;

    private final UrlUtils urlUtils;

    @PostConstruct
    public void init() {
        initDefaultSettings();
    }

    @Override
    public String get(String key) {
        log.info("Fetching admin setting for key: {}", key);
        return repo.findByKey(key)
                .map(AdminSettingsBO::getValue)
                .orElse(null);
    }

    @Override
    public AdminSettingsBO save(String key, String value) {
        log.info("Saving admin setting: {} = {}", key, value);

        AdminSettingsBO s = repo.findByKey(key)
                .orElse(AdminSettingsBO.builder().key(key).build());

        s.setValue(value);
        return repo.save(s);
    }

    @Override
    public Map<String, String> getAll() {
        log.info("Fetching all admin settings");
        return repo.findAll().stream()
                .collect(Collectors.toMap(
                        AdminSettingsBO::getKey,
                        AdminSettingsBO::getValue
                ));
    }

    @Override
    public void initDefaultSettings() {

        initSettingIfMissing("platform_name", "OneAim Store");
        initSettingIfMissing("contact_email", "support@aimdev.com");
        initSettingIfMissing("default_language", "en");
        initSettingIfMissing("default_currency", "INR");
        initSettingIfMissing("time_zone", "Asia/Kolkata");

        initSettingIfMissing("feature_reviews_enabled", "true");
        initSettingIfMissing("feature_wishlist_enabled", "true");
        initSettingIfMissing("feature_seller_applications", "true");

        initSettingIfMissing("accepted_payment_methods", "COD,UPI,CARD");
        initSettingIfMissing("payout_schedule_days", "7");
        initSettingIfMissing("transaction_fee_percent", "0");

        initSettingIfMissing("default_shipping_provider", "INDIA_POST");
        initSettingIfMissing("default_shipping_rate", "50");
        initSettingIfMissing("delivery_regions", "INDIA");

        // CATEGORY-TAX (DO NOT OVERRIDE ADMIN UPDATED VALUES)
        initSettingIfMissing("default_tax_percent", "0");
        initSettingIfMissing("delivery_charges_fixed", "50");

        initSettingIfMissing("tax_electronics", "18");
        initSettingIfMissing("shipping_electronics", "100");

        initSettingIfMissing("tax_fashion", "5");
        initSettingIfMissing("shipping_fashion", "50");

        initSettingIfMissing("tax_grocery", "0");
        initSettingIfMissing("shipping_grocery", "20");

        initSettingIfMissing("terms_url", "");
        initSettingIfMissing("privacy_url", "");
        initSettingIfMissing("return_url", "");

        initSettingIfMissing("notify_new_order", "true");
        initSettingIfMissing("notify_user_activity", "false");

        initSettingIfMissing("admin_user_roles", "ADMIN,MANAGER");
        initSettingIfMissing("session_timeout_minutes", "30");

        initSettingIfMissing("global_discount_percent", "0");
        initSettingIfMissing("enable_discount_engine", "false");
    }


    @Override
    public String verifySeller(String idOrCode, Boolean status) {
        SellerBO seller;

        if (idOrCode.matches("\\d+")) {
            seller = sellerRepo.findById(Long.parseLong(idOrCode))
                    .orElseThrow(() -> new RuntimeException("Seller not found"));
        } else {
            seller = sellerRepo.findBySellerId(idOrCode)
                    .orElseThrow(() -> new RuntimeException("Seller not found"));
        }

        if (Boolean.TRUE.equals(status)) {
            if (!seller.isVerified() || seller.isRejected()) {
                seller.setVerified(true);
                seller.setRejected(false);
                seller.setLocked(false);

                emailService.sendSellerApprovalEmail(
                        seller.getEmail(),
                        seller.getFullName()
                );

                sellerRepo.save(seller);
                return "Seller approved successfully. Email sent.";
            }
            return "Seller already verified.";
        }

        seller.setVerified(false);
        seller.setRejected(true);
        seller.setLocked(true);

        sellerRepo.save(seller);

        try {
            emailService.sendSellerRejectionEmail(
                    seller.getEmail(),
                    seller.getFullName()
            );
        } catch (Exception e) {
            log.warn("Failed to send rejection email to {} : {}", seller.getEmail(), e.getMessage());
        }

        return "Seller rejected successfully. Email sent.";
    }

    @Override
    public List<SellerRs> getUnverifiedSellers() {
        return sellerRepo.findAll().stream()
                .filter(s -> !s.isVerified() && !s.isRejected())
                .map(sellerMapper::mapToSellerRs)
                .collect(Collectors.toList());
    }

    @Override
    public List<SellerRs> getVerifiedSellers() {
        return sellerRepo.findAll().stream()
                .filter(SellerBO::isVerified)
                .map(sellerMapper::mapToSellerRs)
                .collect(Collectors.toList());
    }

    @Override
    public List<SellerRs> getRejectedSellers() {
        return sellerRepo.findAll().stream()
                .filter(SellerBO::isRejected)
                .map(sellerMapper::mapToSellerRs)
                .collect(Collectors.toList());
    }

    @Override
    public byte[] getSellerDocumentsZip(String sellerId) {
        SellerBO seller = sellerRepo.findBySellerId(sellerId)
                .orElseThrow(() -> new RuntimeException("Seller not found"));

        List<Long> fileIds = new ArrayList<>();

        if (seller.getImageFileId() != null)
            fileIds.add(seller.getImageFileId());

        if (fileIds.isEmpty()) {
            throw new RuntimeException("No documents found for seller.");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (ZipOutputStream zos = new ZipOutputStream(baos)) {

            for (Long fileId : fileIds) {

                FileBO fileBo = fileRepo.findById(fileId).orElse(null);
                if (fileBo == null) continue;

                zos.putNextEntry(new ZipEntry(fileBo.getName()));
                zos.write(fileBo.getInputstream());
                zos.closeEntry();
            }

        } catch (Exception e) {
            log.error("Error preparing ZIP: {}", e.getMessage());
            throw new RuntimeException("Failed to prepare seller documents", e);
        }

        return baos.toByteArray();
    }

    @Override
    public int getGlobalDiscount() {
        String value = get("global_discount_percent");
        try {
            return value != null ? Integer.parseInt(value) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public boolean isDiscountEngineEnabled() {
        String value = get("enable_discount_engine");
        return "true".equalsIgnoreCase(value);
    }

    @Override
    public double getDefaultTaxPercent() {
        try {
            return Double.parseDouble(get("default_tax_percent"));
        } catch (Exception e) {
            return 0.0;
        }
    }

    @Override
    public double getDeliveryChargeDefault() {
        try {
            return Double.parseDouble(get("delivery_charges_fixed"));
        } catch (Exception e) {
            return 0.0;
        }
    }

    @Override
    public int getDefaultReturnPolicyDays() {
        try {
            return Integer.parseInt(get("return_policy_days"));
        } catch (Exception e) {
            return 7; // safe fallback
        }
    }

    @Override
    public double getDoubleValue(String key, double defaultValue) {
        try {
            String val = get(key);
            return val != null ? Double.parseDouble(val) : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    @Override
    public long getLongValue(String key, long defaultValue) {
        try {
            String val = get(key);
            return val != null ? Long.parseLong(val) : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    @Override
    public boolean getBooleanValue(String key, boolean defaultValue) {
        try {
            String val = get(key);
            return val != null ? Boolean.parseBoolean(val) : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }


    @Override
    public byte[] getSellerDetailsPdf(String sellerId) {
        log.info("📥 Generating Resume PDF for sellerId: {}", sellerId);

        SellerBO seller = sellerRepo.findBySellerId(sellerId)
                .orElseThrow(() -> new RuntimeException("Seller not found"));

        log.info("🧾 Seller → name: {}, email: {}, phone: {}, imageFileId: {}",
                seller.getFullName(), seller.getEmail(), seller.getPhoneNo(), seller.getImageFileId());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            Document doc = new Document();
            PdfWriter.getInstance(doc, baos);
            doc.open();
            log.info("🖨 PDF document opened");

            // ---------- Embed Seller Image (DB → fallback to Disk) ----------
            if (seller.getImageFileId() != null) {
                Long imageId = seller.getImageFileId();
                log.info("🖼 Seller has imageFileId linked → {}", imageId);

                FileBO imageFile = fileRepo.findById(imageId).orElse(null);

                if (imageFile != null && imageFile.getInputstream() != null && imageFile.getInputstream().length > 0) {
                    log.info("🖼 Embedding image from DB → name: {}", imageFile.getName());
                    try {
                        Image img = Image.getInstance(imageFile.getInputstream());
                        img.scaleToFit(100, 100);
                        img.setAlignment(Image.ALIGN_CENTER);
                        doc.add(img);
                        log.info("✔ Image embedded from DB successfully");
                    } catch (Exception imgErr) {
                        log.error("🔥 Failed to embed DB image for fileId {} → {}", imageId, imgErr.getMessage());
                    }
                } else {
                    log.warn("⚠ DB image is missing/empty for fileId {}, trying disk fallback...", imageId);

                    try {
                        String diskPath = urlUtils.privateFile(seller.getImageFileId());// stored path in DB (example: "/api/files/private/2/view")
                        if (diskPath != null) {
                            // convert to actual local file system path if stored
                            String localDiskPath = diskPath.startsWith("/") ? diskPath.substring(1) : diskPath;
                            log.info("🗂 Disk fallback path → {}", localDiskPath);

                            Image img = Image.getInstance(localDiskPath);
                            img.scaleToFit(100, 100);
                            img.setAlignment(Image.ALIGN_CENTER);
                            doc.add(img);
                            log.info("✔ Image embedded using disk fallback");
                        } else {
                            log.warn("⚠ No valid disk path stored for seller {}", seller.getSellerId());
                        }
                    } catch (Exception diskErr) {
                        log.error("🔥 Disk fallback failed for fileId {} → {}", imageId, diskErr.getMessage());
                    }
                }
            } else {
                log.info("ℹ No imageFileId linked to seller {}", seller.getSellerId());
            }

            // ---------- Resume Text Section ----------
            doc.add(new Paragraph("\n"));

            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
            Paragraph title = new Paragraph(seller.getFullName(), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);

            doc.add(new Paragraph("\n"));

            Font sectionFont = new Font(Font.HELVETICA, 12, Font.BOLD);
            doc.add(new Paragraph("Contact Information", sectionFont));
            doc.add(new Paragraph("Email: " + seller.getEmail()));
            doc.add(new Paragraph("Phone: " + seller.getPhoneNo()));

            doc.add(new Paragraph("\n"));

            doc.add(new Paragraph("Business Details", sectionFont));
            doc.add(new Paragraph("GSTIN: " + seller.getGst()));
            doc.add(new Paragraph("PAN: " + seller.getPanCard()));
            doc.add(new Paragraph("Aadhaar: " + seller.getAdhaar()));

            doc.add(new Paragraph("\n"));

//            doc.add(new Paragraph("Account Status", sectionFont));
//            doc.add(new Paragraph("Verified: " + seller.isVerified()));
//            doc.add(new Paragraph("Locked: " + seller.isLocked()));
//            doc.add(new Paragraph("Rejected: " + seller.isRejected()));

            doc.add(new Paragraph("\n"));

            Font footerFont = new Font(Font.HELVETICA, 10, Font.ITALIC);
            Paragraph footer = new Paragraph("Created On: " + seller.getCreatedAt(), footerFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            doc.add(footer);

            doc.close();
            log.info("📤 PDF generation complete ({} bytes)", baos.size());

        } catch (Exception e) {
            log.error("🔥 PDF GENERATION FAILED → {}", e.getMessage());
            throw new RuntimeException("Failed to generate PDF", e);
        }

        return baos.toByteArray();
    }



    private void initSettingIfMissing(String key, String defaultValue) {
        if (repo.findByKey(key).isEmpty()) {
            save(key, defaultValue);
        }
    }


}
