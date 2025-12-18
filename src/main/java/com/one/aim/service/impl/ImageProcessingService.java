package com.one.aim.service.impl;

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

/**
 * Image Processing Utility for Product Images
 * Add this as a new service class
 */
@Slf4j
@Service
public class ImageProcessingService {

    private static final int TARGET_SIZE = 800; // Square size for product images
    private static final Color BACKGROUND_COLOR = new Color(237, 229, 221); // #EDE5DD (beige)

    /**
     * Process image to square format with padding and beige background
     * Call this method before saving product images
     */
    public InputStream processProductImage(InputStream inputStream, String originalFilename) throws IOException {
        try {
            BufferedImage originalImage = ImageIO.read(inputStream);

            if (originalImage == null) {
                throw new IOException("Invalid image file: " + originalFilename);
            }

            // Create square canvas with beige background
            BufferedImage squareImage = new BufferedImage(
                    TARGET_SIZE,
                    TARGET_SIZE,
                    BufferedImage.TYPE_INT_RGB
            );

            Graphics2D g2d = squareImage.createGraphics();

            // Enable high-quality rendering
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            // Fill background with beige color
            g2d.setColor(BACKGROUND_COLOR);
            g2d.fillRect(0, 0, TARGET_SIZE, TARGET_SIZE);

            // Calculate dimensions to fit image with padding
            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();
            int padding = TARGET_SIZE / 10; // 10% padding
            int maxSize = TARGET_SIZE - (2 * padding);

            // Calculate scaling to maintain aspect ratio
            double scale = Math.min(
                    (double) maxSize / originalWidth,
                    (double) maxSize / originalHeight
            );

            int newWidth = (int) (originalWidth * scale);
            int newHeight = (int) (originalHeight * scale);

            // Center the image
            int x = (TARGET_SIZE - newWidth) / 2;
            int y = (TARGET_SIZE - newHeight) / 2;

            // Draw the image
            g2d.drawImage(originalImage, x, y, newWidth, newHeight, null);
            g2d.dispose();

            // Convert to InputStream
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(squareImage, "png", baos);

            log.info("Processed image: {} -> {}x{} square with padding",
                    originalFilename, TARGET_SIZE, TARGET_SIZE);

            return new ByteArrayInputStream(baos.toByteArray());

        } catch (Exception e) {
            log.error("Error processing image {}: {}", originalFilename, e.getMessage());
            // Return original stream if processing fails
            throw new IOException("Failed to process image: " + e.getMessage());
        }
    }

    /**
     * Process MultipartFile directly
     */
    public InputStream processProductImage(MultipartFile file) throws IOException {
        return processProductImage(file.getInputStream(), file.getOriginalFilename());
    }
}