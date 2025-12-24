package com.one.aim.service.impl;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Service
public class ImageProcessingService {

    // ============================================
    // IMAGE PROCESSING RULES
    // ============================================
    private static final int TARGET_SIZE = 800;           // 800x800px square
    private static final int MIN_DIMENSION = 400;         // Minimum width/height
    private static final int MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final String[] ALLOWED_FORMATS = {"jpg", "jpeg", "png"};
    private static final int CATEGORY_SIZE = 600;


    /**
     * Process product image WITHOUT padding
     * - Validates dimensions (min 400px)
     * - Creates 800x800 square
     * - Scales to fill entire canvas (no padding)
     * - May crop to maintain aspect ratio
     */
    public InputStream processProductImage(MultipartFile file) throws IOException {

        // ============================================
        // 1. VALIDATE FILE SIZE
        // ============================================
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IOException("File size exceeds 5MB limit");
        }

        // ============================================
        // 2. VALIDATE FILE TYPE
        // ============================================
        String contentType = file.getContentType();
        if (contentType == null || !isValidImageType(contentType)) {
            throw new IOException("Invalid file type. Only JPG and PNG allowed");
        }

        // ============================================
        // 3. READ AND VALIDATE IMAGE
        // ============================================
        BufferedImage originalImage;
        try {
            originalImage = ImageIO.read(file.getInputStream());
        } catch (Exception e) {
            throw new IOException("Failed to read image file: " + e.getMessage());
        }

        if (originalImage == null) {
            throw new IOException("Invalid or corrupted image file");
        }

        // ============================================
        // 4. VALIDATE DIMENSIONS
        // ============================================
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        if (originalWidth < MIN_DIMENSION || originalHeight < MIN_DIMENSION) {
            throw new IOException(String.format(
                    "Image dimensions too small. Minimum %dx%d pixels required. Current: %dx%d",
                    MIN_DIMENSION, MIN_DIMENSION, originalWidth, originalHeight
            ));
        }

        // ============================================
        // 5. CREATE SQUARE CANVAS (NO BACKGROUND)
        // ============================================
        BufferedImage squareImage = new BufferedImage(
                TARGET_SIZE,
                TARGET_SIZE,
                BufferedImage.TYPE_INT_RGB
        );

        Graphics2D g2d = squareImage.createGraphics();

        // Enable high-quality rendering
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING,
                RenderingHints.VALUE_COLOR_RENDER_QUALITY);

        // ============================================
        // 6. CALCULATE SCALING TO FILL ENTIRE CANVAS
        // ============================================
        // Scale to cover entire canvas (may crop to fit)
        double scaleX = (double) TARGET_SIZE / originalWidth;
        double scaleY = (double) TARGET_SIZE / originalHeight;

        // Use larger scale to ensure image fills entire canvas
        double scale = Math.max(scaleX, scaleY);

        int newWidth = (int) (originalWidth * scale);
        int newHeight = (int) (originalHeight * scale);

        // Center the scaled image
        int x = (TARGET_SIZE - newWidth) / 2;
        int y = (TARGET_SIZE - newHeight) / 2;

        // ============================================
        // 7. DRAW THE SCALED IMAGE (FILLS ENTIRE CANVAS)
        // ============================================
        g2d.drawImage(originalImage, x, y, newWidth, newHeight, null);
        g2d.dispose();

        // ============================================
        // 8. CONVERT TO PNG OUTPUT STREAM
        // ============================================
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            boolean written = ImageIO.write(squareImage, "png", baos);
            if (!written) {
                throw new IOException("Failed to write processed image");
            }
        } catch (Exception e) {
            throw new IOException("Error writing processed image: " + e.getMessage());
        }

        log.info("✅ Processed image: {} ({}x{}) -> 800x800 square (NO PADDING)",
                file.getOriginalFilename(), originalWidth, originalHeight);

        return new ByteArrayInputStream(baos.toByteArray());
    }

    /**
     * Validate if content type is allowed
     */
    private boolean isValidImageType(String contentType) {
        return contentType.equals("image/jpeg")
                || contentType.equals("image/jpg")
                || contentType.equals("image/png");
    }

    /**
     * Get image dimensions without processing
     */
    public Dimension getImageDimensions(MultipartFile file) throws IOException {
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                throw new IOException("Invalid image file");
            }
            return new Dimension(image.getWidth(), image.getHeight());
        } catch (Exception e) {
            throw new IOException("Failed to read image dimensions: " + e.getMessage());
        }
    }

    /**
     * Validate image meets requirements without processing
     */
    public void validateImageRequirements(MultipartFile file) throws IOException {

        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IOException("File size exceeds 5MB limit");
        }

        // Check file type
        String contentType = file.getContentType();
        if (contentType == null || !isValidImageType(contentType)) {
            throw new IOException("Invalid file type. Only JPG and PNG allowed");
        }

        // Check dimensions
        Dimension dim = getImageDimensions(file);
        if (dim.width < MIN_DIMENSION || dim.height < MIN_DIMENSION) {
            throw new IOException(String.format(
                    "Image too small. Minimum %dx%d pixels required. Current: %dx%d",
                    MIN_DIMENSION, MIN_DIMENSION, dim.width, dim.height
            ));
        }
    }

    public MultipartFile processCategoryImage(MultipartFile file) throws IOException {

        BufferedImage original = ImageIO.read(file.getInputStream());
        if (original == null) {
            throw new IOException("Invalid image file");
        }

        BufferedImage processed =
                Thumbnails.of(original)
                        .size(CATEGORY_SIZE, CATEGORY_SIZE)
                        .outputQuality(0.85)
                        .asBufferedImage();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(processed, "jpg", baos);

        log.info("✅ Category image processed: {} → {}x{}",
                file.getOriginalFilename(), CATEGORY_SIZE, CATEGORY_SIZE);

        return new MockMultipartFile(
                "file",
                file.getOriginalFilename(),
                "image/jpeg",
                baos.toByteArray()
        );
    }
}