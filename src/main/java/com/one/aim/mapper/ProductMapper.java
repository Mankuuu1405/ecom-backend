package com.one.aim.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.one.aim.bo.ProductBO;
import com.one.aim.bo.ReviewBO;
import com.one.aim.rs.*;
import com.one.aim.service.FileService;
import com.one.utils.UrlUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class ProductMapper {


    private final FileService fileService;
    private final UrlUtils urlUtils;

    // ================================================
    // PRODUCT CARD (For Shop/Listing Pages)
    // ================================================
    public ProductCardRs toCardRs(ProductBO bo) {
        if (bo == null) return null;

        ProductCardRs rs = new ProductCardRs();

        rs.setDocId(String.valueOf(bo.getId()));
        rs.setName(bo.getName());
        rs.setSlug(bo.getSlug());
        rs.setBrand(bo.getBrand());
        rs.setPrice(bo.getPrice());
        rs.setCategoryName(bo.getCategoryName());
        rs.setCategoryId(bo.getCategoryId());

        // Main Image
        if (bo.getImageFileIds() != null && !bo.getImageFileIds().isEmpty()) {
            Long fileId = bo.getImageFileIds().get(0);
            rs.setImage(urlUtils.publicFile(fileId));
        }

        // Stock Status
        int stock = bo.getStock() == null ? 0 : bo.getStock();
        rs.setInStock(stock > 0);

        // Short Description (60 chars max)
        String desc = bo.getDescription();
        if (desc != null) {
            rs.setShortDescription(desc.length() > 60 ? desc.substring(0, 60) + "..." : desc);
        }

        // Ratings (Dynamic)
        rs.setAverageRating(
                bo.getReviewCount() != null && bo.getReviewCount() > 0
                        ? bo.getAverageRating()
                        : null
        );
        rs.setReviewCount(bo.getReviewCount() != null ? bo.getReviewCount() : 0L);

        return rs;
    }

    // ================================================
    // PRODUCT DETAILS (For Product Detail Page)
    // ================================================
    public ProductDetailsRs toDetails(ProductBO product) {

        ProductDetailsRs rs = new ProductDetailsRs();

        rs.setId(product.getId());
        rs.setName(product.getName());
        rs.setDescription(product.getDescription());
        rs.setPrice(product.getPrice());
        rs.setStock(product.getStock());
        rs.setBrand(product.getBrand());
        rs.setSlug(product.getSlug());
        rs.setCategoryName(product.getCategoryName());
        rs.setCategoryId(product.getCategoryId());
        rs.setActive(product.isActive());
        rs.setLowStock(product.isLowStock());
        rs.setInStock(product.getStock() != null && product.getStock() > 0);

        // Images
        List<String> imageUrls = product.getImageFileIds().stream()
                .map(urlUtils::publicFile)
                .toList();
        rs.setImages(imageUrls);

        // Product specifications
        Map<String, String> details = new LinkedHashMap<>();
        if (product.getMaterial() != null) details.put("Material", product.getMaterial());
        if (product.getSole() != null) details.put("Sole", product.getSole());
        if (product.getClosure() != null) details.put("Closure", product.getClosure());
        if (product.getWeight() != null) details.put("Weight", product.getWeight());
        if (product.getColor() != null) details.put("Color", product.getColor());
        if (product.getSize() != null) details.put("Size", product.getSize());
        if (product.getBrand() != null) details.put("Brand", product.getBrand());

        if (product.getSpecificationsJson() != null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, String> jsonSpecs = mapper.readValue(
                        product.getSpecificationsJson(),
                        new TypeReference<Map<String, String>>() {}
                );
                details.putAll(jsonSpecs);
            } catch (Exception e) {
                log.error("Error parsing specifications JSON", e);
            }
        }
        rs.setDetails(details);

        // Rating summary
        rs.setAverageRating(
                product.getReviewCount() != null && product.getReviewCount() > 0
                        ? product.getAverageRating()
                        : null
        );

        rs.setReviewCount(
                product.getReviewCount() != null ? product.getReviewCount() : 0L
        );


        // Rating distribution (placeholder for now)
        rs.setRatingDistribution(Collections.emptyList());

        return rs;
    }


    // ================================================
    // PRODUCT RS (For Seller Dashboard / Admin)
    // ================================================
    public ProductRs toProductRs(ProductBO bo) {
        if (bo == null) {
            log.warn("ProductBO is NULL");
            return null;
        }

        ProductRs rs = new ProductRs();

        rs.setDocId(String.valueOf(bo.getId()));
        rs.setName(bo.getName());
        rs.setDescription(bo.getDescription());
        rs.setPrice(bo.getPrice() == null ? 0.0 : bo.getPrice());
        rs.setStock(bo.getStock() == null ? 0 : bo.getStock());
        rs.setBrand(bo.getBrand());
        rs.setCategoryName(bo.getCategoryName());
        rs.setCategoryId(bo.getCategoryId());
        rs.setSlug(bo.getSlug());
        rs.setActive(bo.isActive());
        rs.setFeatured(bo.isFeatured());

        // Product Details
        rs.setMaterial(bo.getMaterial());
        rs.setSole(bo.getSole());
        rs.setClosure(bo.getClosure());
        rs.setWeight(bo.getWeight());
        rs.setColor(bo.getColor());
        rs.setSize(bo.getSize());
        rs.setSpecificationsJson(bo.getSpecificationsJson());

        // Ratings
        rs.setAverageRating(bo.getAverageRating());
        rs.setReviewCount(bo.getReviewCount());

        // Images
        if (bo.getImageFileIds() != null && !bo.getImageFileIds().isEmpty()) {
            List<String> imageUrls = bo.getImageFileIds().stream()
                    .map(urlUtils::publicFile)
                    .collect(Collectors.toList());
            rs.setImages(imageUrls);

            if (!imageUrls.isEmpty()) {
                rs.setImage(imageUrls.get(0));
            }
        }

        rs.setInStock(bo.getStock() != null && bo.getStock() > 0);
        rs.setCreatedAt(bo.getCreatedAt());
        rs.setUpdatedAt(bo.getUpdatedAt());

        return rs;
    }

    // ================================================
    // REVIEW MAPPER
    // ================================================
    private ReviewRs toReviewRs(ReviewBO review) {
        ReviewRs rs = new ReviewRs();
        rs.setId(review.getId());
        rs.setUserName(review.getUser().getFullName());
        rs.setUserAvatar(null); // Set if you have user avatar
        rs.setDate(review.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        rs.setRating(review.getRating());
        rs.setComment(review.getComment());
        rs.setLikes(review.getLikes());
        rs.setDislikes(review.getDislikes());
        rs.setVerifiedPurchase(review.isVerified());
        return rs;
    }

    // ================================================
    // RATING DISTRIBUTION
    // ================================================
    private List<RatingDistributionRs> calculateRatingDistribution(
            List<ReviewBO> reviews, Long totalCount) {

        if (totalCount == null || totalCount == 0) {
            return Arrays.asList(
                    new RatingDistributionRs(5, 0, 0),
                    new RatingDistributionRs(4, 0, 0),
                    new RatingDistributionRs(3, 0, 0),
                    new RatingDistributionRs(2, 0, 0),
                    new RatingDistributionRs(1, 0, 0)
            );
        }

        Map<Integer, Long> ratingCounts = reviews.stream()
                .collect(Collectors.groupingBy(ReviewBO::getRating, Collectors.counting()));

        List<RatingDistributionRs> distribution = new ArrayList<>();
        for (int stars = 5; stars >= 1; stars--) {
            int count = ratingCounts.getOrDefault(stars, 0L).intValue();
            int percentage = (int) Math.round((count * 100.0) / totalCount);
            distribution.add(new RatingDistributionRs(stars, count, percentage));
        }

        return distribution;
    }

    // ================================================
    // BATCH MAPPING
    // ================================================
    public List<ProductRs> toProductRsList(List<ProductBO> bos) {
        if (bos == null || bos.isEmpty()) {
            return Collections.emptyList();
        }
        return bos.stream()
                .map(this::toProductRs)
                .collect(Collectors.toList());
    }

    public List<ProductCardRs> toCardRsList(List<ProductBO> bos) {
        if (bos == null || bos.isEmpty()) {
            return Collections.emptyList();
        }
        return bos.stream()
                .map(this::toCardRs)
                .collect(Collectors.toList());
    }
}