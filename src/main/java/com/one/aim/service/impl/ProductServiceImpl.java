package com.one.aim.service.impl;

import com.one.aim.bo.*;
import com.one.aim.constants.ErrorCodes;
import com.one.aim.helper.ProductHelper;
import com.one.aim.mapper.ProductMapper;
import com.one.aim.repo.*;
import com.one.aim.rq.ProductRq;
import com.one.aim.rs.ProductCardRs;
import com.one.aim.rs.ProductDetailsRs;
import com.one.aim.rs.ProductRs;
import com.one.aim.rs.ReviewRs;
import com.one.aim.rs.data.ProductDataRs;
import com.one.aim.rs.data.ProductDataRsList;
import com.one.aim.service.FileService;
import com.one.aim.service.NotificationService;
import com.one.aim.service.ProductService;
import com.one.aim.service.RecommendationService;
import com.one.utils.AuthUtils;
import com.one.utils.Utils;
import com.one.vm.core.BaseDataRs;
import com.one.vm.core.BaseRs;
import com.one.vm.utils.ResponseUtils;

import com.one.vm.utils.SlugUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepo productRepo;
    private final SellerRepo sellerRepo;
    private final FileService fileService;
    private final UserActivityService userActivityService;
    private final CategoryRepo categoryRepo;
    private final OrderItemBORepo orderItemBORepo;
    private final CartRepo cartRepo;
    private final NotificationService notificationService;
    private final RecommendationService recommendationService;
    private final ReviewRepository reviewRepo;
    private final ProductMapper productMapper;
    private final ImageProcessingService imageProcessingService;

    @Value("${app.frontend.product.url}")
    private String productFrontUrl;

    // ======================================================================
// SELLER: ADD PRODUCT (WITH ENHANCED NOTIFICATION)
// ======================================================================
    @Override
    @Transactional
    public BaseRs addProduct(ProductRq rq) {
        try {
            validateSellerAccess();

            List<String> errors = ProductHelper.validateProduct(rq);
            if (!errors.isEmpty()) {
                return ResponseUtils.failure(ErrorCodes.EC_INVALID_INPUT, errors);
            }

            Long sellerId = AuthUtils.getLoggedUserId();
            SellerBO seller = sellerRepo.findById(sellerId)
                    .orElseThrow(() -> new RuntimeException("Seller not found"));

            if (rq.getStock() == null || rq.getStock() < 1) {
                return ResponseUtils.failure(ErrorCodes.EC_INVALID_INPUT, "Stock must be at least 1");
            }

            if (rq.getPrice() == null || rq.getPrice() <= 0) {
                return ResponseUtils.failure(ErrorCodes.EC_INVALID_INPUT, "Price must be greater than 0");
            }

            ProductBO bo = new ProductBO();
            bo.setSeller(seller);
            bo.setName(rq.getName());

            // ---------- SLUG ----------
            String slug = SlugUtils.from(rq.getName());
            int counter = 1;
            while (productRepo.existsBySlug(slug)) {
                slug = SlugUtils.from(rq.getName()) + "-" + counter++;
            }
            bo.setSlug(slug);

            // ---------- BASIC FIELDS ----------
            bo.setDescription(rq.getDescription());
            bo.setPrice(rq.getPrice());
            bo.setStock(rq.getStock());
            bo.setBrand(rq.getBrand());
            bo.setOnSale(rq.isOnSale());
            bo.setNewArrival(true);
            bo.setBestSeller(false);
            bo.setSpecificationsJson(rq.getSpecificationsJson());

            // ---------- CATEGORY ----------
            if (rq.getCategoryId() != null) {
                CategoryBO category = categoryRepo.findById(rq.getCategoryId())
                        .orElseThrow(() -> new RuntimeException("Category not found"));
                bo.setCategoryId(category.getId());
                bo.setCategoryName(category.getName());
            } else {
                if (Utils.isEmpty(rq.getCustomCategoryName())) {
                    return ResponseUtils.failure(ErrorCodes.EC_INVALID_INPUT, "Category name is required");
                }

                CategoryBO category = categoryRepo
                        .findByNameIgnoreCase(rq.getCustomCategoryName())
                        .orElseGet(() -> categoryRepo.save(
                                CategoryBO.builder()
                                        .name(rq.getCustomCategoryName().trim())
                                        .slug(SlugUtils.from(rq.getCustomCategoryName()))
                                        .active(false)
                                        .build()
                        ));

                bo.setCategoryId(category.getId());
                bo.setCategoryName(category.getName());
            }

            // ---------- IMAGES ----------
            List<MultipartFile> images = rq.getImages();
            if (images == null || images.isEmpty()) {
                return ResponseUtils.failure("NO_IMAGE", "Product must have at least one image");
            }

            if (images.size() > 5) {
                return ResponseUtils.failure("TOO_MANY_IMAGES", "Maximum 5 images allowed");
            }

            List<Long> imageFileIds = new ArrayList<>();

            for (MultipartFile file : images) {
                if (file == null || file.isEmpty()) continue;

                if (!List.of("image/jpeg", "image/png", "image/jpg").contains(file.getContentType())) {
                    return ResponseUtils.failure("INVALID_FILE_TYPE", "Only JPG and PNG allowed");
                }

                if (file.getSize() > 5 * 1024 * 1024) {
                    return ResponseUtils.failure("FILE_TOO_LARGE", "Max 5MB per image allowed");
                }

                InputStream processed = imageProcessingService.processProductImage(file);

                FileBO uploaded = fileService.uploadFile(
                        processed,
                        "product_" + System.currentTimeMillis() + ".png",
                        "image/png"
                );

                imageFileIds.add(uploaded.getId());
            }

            if (imageFileIds.isEmpty()) {
                return ResponseUtils.failure("NO_IMAGE", "At least one valid image required");
            }

            // ---------- THUMBNAIL ----------
            Long thumbnailFileId = imageFileIds.get(0);
            bo.setImageFileIds(imageFileIds);
            bo.setThumbnailFileId(thumbnailFileId);

            productRepo.save(bo);

            // ---------- ACTIVITY LOG ----------
            userActivityService.log(
                    sellerId,
                    "PRODUCT_CREATED",
                    "Created product: " + bo.getName()
            );

            // ---------- ENHANCED NOTIFICATION TO ADMINS ----------
            notificationService.notifyAdmins(
                    "PRODUCT_ADDED",
                    "New Product Added",
                    "", // Description will be built in service
                    seller,    // Seller info
                    bo,        // Product info
                    null,
                    "/admin/products/" + bo.getSlug()
            );

            return ResponseUtils.success(
                    new ProductDataRs(
                            "Product created successfully",
                            productMapper.toProductRs(bo)
                    )
            );

        } catch (Exception e) {
            log.error("addProduct() failed", e);
            return ResponseUtils.failure(ErrorCodes.EC_INTERNAL_ERROR, e.getMessage());
        }
    }

    // ======================================================================
// SELLER: UPDATE PRODUCT (WITH CHANGE TRACKING)
// ======================================================================
    @Override
    @Transactional
    public BaseRs updateProduct(ProductRq rq) {
        try {
        	String role = AuthUtils.getLoggedUserRole();
        	if (!"ADMIN".equalsIgnoreCase(role)) {
        	    validateSellerAccess(); // only validate seller
        	}
            //validateSellerAccess();

            if (rq.getDocId() == null) {
                return ResponseUtils.failure(ErrorCodes.EC_REQUIRED_DOCID, "Product ID required");
            }
            
            
            Long productId = Long.valueOf(rq.getDocId());
            ProductBO product = productRepo.findById(productId).orElse(null);

            if (product == null) {
                return ResponseUtils.failure(ErrorCodes.EC_PRODUCT_NOT_FOUND, "Product not found");
            }
            Long loggedUserId = AuthUtils.getLoggedUserId();
            
            //Long sellerId = AuthUtils.getLoggedUserId();
            if (!"ADMIN".equalsIgnoreCase(role) && !product.getSeller().getId().equals(loggedUserId)) {
                return ResponseUtils.failure(ErrorCodes.EC_UNAUTHORIZED, "Unauthorized");
            }

            Map<String, String> changes = new LinkedHashMap<>();

            // =================================================
            // ACTIVE TOGGLE ONLY
            // =================================================
            if (rq.getActive() != null &&
                    rq.getName() == null &&
                    rq.getDescription() == null &&
                    rq.getPrice() == null &&
                    rq.getStock() == null &&
                    rq.getCategoryId() == null &&
                    rq.getCustomCategoryName() == null &&
                    (rq.getImages() == null || rq.getImages().isEmpty())) {

                if (product.isActive() != rq.getActive()) {
                    changes.put("Status", product.isActive() ? "Active → Inactive" : "Inactive → Active");
                    product.setActive(rq.getActive());
                }

                productRepo.save(product);

                if (!product.isActive()) {
                    cartRepo.deleteByProduct_Id(product.getId());
                }

                sendProductUpdateNotification(product, changes);

                return ResponseUtils.success(
                        new ProductDataRs("Product status updated",
                                productMapper.toProductRs(product))
                );
            }

            // =================================================
            // TRACK ALL CHANGES
            // =================================================
            if (Utils.isNotEmpty(rq.getName()) && !rq.getName().equals(product.getName())) {
                changes.put("Name", product.getName() + " → " + rq.getName());
                product.setName(rq.getName());
                product.setSlug(SlugUtils.from(rq.getName()));
            }

            if (Utils.isNotEmpty(rq.getDescription())
                    && !rq.getDescription().equals(product.getDescription())) {
                changes.put("Description", "Updated");
                product.setDescription(rq.getDescription());
            }

            if (rq.getPrice() != null && rq.getPrice() > 0
                    && !rq.getPrice().equals(product.getPrice())) {
                changes.put("Price", "₹" + product.getPrice() + " → ₹" + rq.getPrice());
                product.setPrice(rq.getPrice());
            }

            boolean stockChanged = false;
            if (rq.getStock() != null && !rq.getStock().equals(product.getStock())) {
                changes.put("Stock", product.getStock() + " → " + rq.getStock());
                product.setStock(rq.getStock());
                product.updateLowStock();
                stockChanged = true;
            }

            if (rq.isOnSale() != product.isOnSale()) {
                changes.put("Sale Status", product.isOnSale() ? "On Sale → Regular" : "Regular → On Sale");
                product.setOnSale(rq.isOnSale());
            }

            // =================================================
            // CATEGORY CHANGES
            // =================================================
            if (rq.getCategoryId() != null &&
                    !rq.getCategoryId().equals(product.getCategoryId())) {

                CategoryBO category = categoryRepo.findById(rq.getCategoryId())
                        .orElseThrow(() -> new RuntimeException("Category not found"));

                changes.put("Category", product.getCategoryName() + " → " + category.getName());
                product.setCategoryId(category.getId());
                product.setCategoryName(category.getName());

            } else if (Utils.isNotEmpty(rq.getCustomCategoryName())
                    && !rq.getCustomCategoryName().equals(product.getCategoryName())) {

                changes.put("Category", product.getCategoryName() + " → " + rq.getCustomCategoryName());
                product.setCategoryId(null);
                product.setCategoryName(rq.getCustomCategoryName());
            }

            if (Utils.isNotEmpty(rq.getSpecificationsJson())
                    && !rq.getSpecificationsJson().equals(product.getSpecificationsJson())) {
                changes.put("Specifications", "Updated");
                product.setSpecificationsJson(rq.getSpecificationsJson());
            }

            // =================================================
            // IMAGE CHANGES
            // =================================================
            List<MultipartFile> images = rq.getImages();
            if (images != null && !images.isEmpty()) {
                if (images.size() > 5) {
                    return ResponseUtils.failure("TOO_MANY_IMAGES", "Maximum 5 images allowed");
                }

                List<Long> newImageIds = new ArrayList<>();

                for (MultipartFile file : images) {
                    if (file.isEmpty()) continue;

                    if (!List.of("image/jpeg", "image/png", "image/jpg")
                            .contains(file.getContentType())) {
                        return ResponseUtils.failure("INVALID_FILE_TYPE", "Only JPG and PNG allowed");
                    }

                    if (file.getSize() > 5 * 1024 * 1024) {
                        return ResponseUtils.failure("FILE_TOO_LARGE", "Max 5MB per image allowed");
                    }

                    InputStream processed = imageProcessingService.processProductImage(file);

                    FileBO uploaded = fileService.uploadFile(
                            processed,
                            "product_" + System.currentTimeMillis() + ".png",
                            "image/png"
                    );

                    newImageIds.add(uploaded.getId());
                }

                if (newImageIds.isEmpty()) {
                    return ResponseUtils.failure("NO_IMAGE", "At least one valid image required");
                }

                product.setImageFileIds(newImageIds);
                product.setThumbnailFileId(newImageIds.get(0));
                changes.put("Images", "Updated (" + newImageIds.size() + " images)");
            }

            productRepo.save(product);

            // =================================================
            // CART SYNC
            // =================================================
            if (stockChanged) {
                if (product.getStock() <= 0) {
                    cartRepo.deleteByProduct_Id(product.getId());
                } else {
                    cartRepo.findAllByProduct_Id(product.getId()).stream()
                            .filter(c -> c.getQuantity() > product.getStock())
                            .forEach(c -> {
                                c.setEnabled(false);
                                cartRepo.save(c);
                            });
                }
            }

            // =================================================
            // SEND NOTIFICATION WITH CHANGES
            // =================================================
            sendProductUpdateNotification(product, changes);

            return ResponseUtils.success(
                    new ProductDataRs("Product updated successfully",
                            productMapper.toProductRs(product))
            );

        } catch (Exception e) {
            log.error("updateProduct() failed", e);
            return ResponseUtils.failure(ErrorCodes.EC_INTERNAL_ERROR, e.getMessage());
        }
    }


    // ======================================================================
    // SELLER: IMAGE UPLOAD
    // ======================================================================
    @Override
    @Transactional
    public BaseRs uploadProductImages(Long productId, List<MultipartFile> files) {
        //validateSellerAccess();

        try {
        	String role = AuthUtils.getLoggedUserRole();
        	if (!"ADMIN".equalsIgnoreCase(role)) {
        		validateSellerAccess();
        	}
        	
            ProductBO bo = productRepo.findById(productId).orElse(null);
            if (bo == null) {
                return ResponseUtils.failure(ErrorCodes.EC_PRODUCT_NOT_FOUND, "Product not found");
            }

            Long loggedUserId = AuthUtils.getLoggedUserId();
            
            //Long sellerId = AuthUtils.findLoggedInUser().getDocId();
            if (!"ADMIN".equalsIgnoreCase(role) && !bo.getSeller().getId().equals(loggedUserId)) {
                return ResponseUtils.failure(ErrorCodes.EC_UNAUTHORIZED, "Unauthorized image upload");
            }

            if (files == null || files.isEmpty()) {
                return ResponseUtils.failure(ErrorCodes.EC_INVALID_INPUT, "No images provided");
            }

            if (bo.getImageFileIds().size() + files.size() > 5) {
                return ResponseUtils.failure("TOO_MANY_IMAGES", "Maximum 5 images allowed");
            }

            List<Long> newImageIds = new ArrayList<>();

            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;

                if (!List.of("image/jpeg", "image/png").contains(file.getContentType())) {
                    return ResponseUtils.failure("INVALID_FILE_TYPE", "Only JPG and PNG allowed");
                }

                if (file.getSize() > 2 * 1024 * 1024) {
                    return ResponseUtils.failure("FILE_TOO_LARGE", "Max 2MB allowed");
                }

                InputStream processed =
                        imageProcessingService.processProductImage(file);

                FileBO uploaded = fileService.uploadFile(
                        processed,
                        "product_" + System.currentTimeMillis() + ".png",
                        "image/png"
                );

                bo.getImageFileIds().add(uploaded.getId());
                newImageIds.add(uploaded.getId());
            }

            productRepo.save(bo);

            return ResponseUtils.success(
                    new BaseDataRs("Images uploaded successfully", newImageIds)
            );

        } catch (Exception e) {
            log.error("uploadProductImages() failed", e);
            return ResponseUtils.failure(ErrorCodes.EC_INTERNAL_ERROR, e.getMessage());
        }
    }



    // ======================================================================
    // SELLER: GET PRODUCT IMAGES
    // ======================================================================
    @Override
    public BaseRs getProductImages(Long productId) {
        validateSellerAccess();

        try {
            ProductBO bo = productRepo.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            List<Long> ordered = new ArrayList<>();

            if (bo.getThumbnailFileId() != null) {
                ordered.add(bo.getThumbnailFileId());
            }

            for (Long id : bo.getImageFileIds()) {
                if (!id.equals(bo.getThumbnailFileId())) {
                    ordered.add(id);
                }
            }

            List<String> urls = ordered.stream()
                    .map(id -> "/api/files/public/" + id + "/view")
                    .toList();

            return ResponseUtils.success(
                    new BaseDataRs("Product images retrieved", urls)
            );

        } catch (Exception e) {
            log.error("getProductImages() failed", e);
            return ResponseUtils.failure(ErrorCodes.EC_INTERNAL_ERROR, e.getMessage());
        }
    }


    // ======================================================================
    // SELLER: DELETE PRODUCT IMAGE
    // ======================================================================
    @Override
    @Transactional
    public BaseRs deleteProductImage(Long productId, Long imageId) {
        try {
        	String role = AuthUtils.getLoggedUserRole();
            //Long sellerId = AuthUtils.getLoggedUserId();
        	Long loggedUserId = AuthUtils.getLoggedUserId();

            ProductBO product = productRepo.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            if (!"ADMIN".equalsIgnoreCase(role) && !product.getSeller().getId().equals(loggedUserId) ) {
                return ResponseUtils.failure(ErrorCodes.EC_UNAUTHORIZED, "Unauthorized");
            }

            List<Long> images = product.getImageFileIds();

            if (images == null || images.isEmpty()) {
                return ResponseUtils.failure("NO_IMAGES", "No images found");
            }

            if (!images.contains(imageId)) {
                return ResponseUtils.failure("IMAGE_NOT_FOUND", "Image not found");
            }

            if (images.size() <= 1) {
                return ResponseUtils.failure("LAST_IMAGE", "At least one image must remain");
            }

            //  Safe removal (object, not index)
            images.remove(Long.valueOf(imageId));

            //  First image is ALWAYS thumbnail
            product.setImageFileIds(images);
            product.setThumbnailFileId(images.get(0));

            productRepo.save(product);

            //  Delete physical file AFTER DB update
            fileService.deleteFileById(String.valueOf(imageId));

            return ResponseUtils.success("Image deleted successfully");

        } catch (Exception e) {
            log.error("deleteProductImage() failed", e);
            return ResponseUtils.failure(ErrorCodes.EC_INTERNAL_ERROR, e.getMessage());
        }
    }



    // ======================================================================
    // SELLER: DELETE PRODUCT
    // ======================================================================
    @Override
    @Transactional
    public BaseRs deleteProduct(Long productId) {
        validateSellerAccess();

        try {
            ProductBO bo = productRepo.findById(productId).orElse(null);

            if (bo == null) {
                return ResponseUtils.failure(ErrorCodes.EC_PRODUCT_NOT_FOUND, "Product not found");
            }

            Long loggedUserId = AuthUtils.getLoggedUserId();
            String role = AuthUtils.getLoggedUserRole();
            //Long sellerId = AuthUtils.getLoggedUserId();
            if (!bo.getSeller().getId().equals(loggedUserId) && !"ADMIN".equalsIgnoreCase(role)) {
                return ResponseUtils.failure(ErrorCodes.EC_UNAUTHORIZED, "Unauthorized");
            }

            bo.setActive(false);
            productRepo.save(bo);

            userActivityService.log(loggedUserId,
                    "PRODUCT_DELETED",
                    "Deleted product: " + bo.getName());

            return ResponseUtils.success("Product deleted successfully");

        } catch (Exception e) {
            log.error("deleteProduct() failed", e);
            return ResponseUtils.failure(ErrorCodes.EC_INTERNAL_ERROR, e.getMessage());
        }
    }

    // ======================================================================
    // SELLER: LIST OWN PRODUCTS
    // ======================================================================
    @Override
    public BaseRs listSellerProducts(boolean showInactive) {
        Long sellerId = AuthUtils.findLoggedInUser().getDocId();

        List<ProductBO> products = productRepo.findBySellerId(sellerId);

        if (!showInactive) {
            products = products.stream()
                    .filter(ProductBO::isActive)
                    .toList();
        }

        for (ProductBO p : products) {
            Integer sold = orderItemBORepo.countProductSales(p.getId());
            p.setSoldItem(sold == null ? 0 : sold);
        }

        return ResponseUtils.success(products);
    }

    @Override
    public BaseRs listAdminProducts(int page, int size, String sortBy, String direction) throws Exception {

        // 1 — Build sorting
        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        // 2 — Query products
        Page<ProductBO> productsPage = productRepo.findByActiveTrue(pageable);


        // 3 — Convert to result list
        List<Map<String, Object>> result = productsPage.getContent()
                .stream()
                .map(p -> {
                    Map<String, Object> map = new LinkedHashMap<>();

                    map.put("productId", p.getId());
                    map.put("name", p.getName());
                    map.put("description", p.getDescription());
                    map.put("price", p.getPrice());
                    map.put("stock", p.getStock());
                    map.put("categoryName", p.getCategoryName());
                    map.put("categoryId", p.getCategoryId());
                    map.put("slug", p.getSlug());
                    map.put("active", p.isActive());
                    map.put("createdAt", p.getCreatedAt());
                    map.put("updatedAt", p.getUpdatedAt());

                    // Images
                    List<String> imageUrls = p.getImageFileIds()
                            .stream()
                            .map(id -> "/api/files/public/" + id + "/view")
                            .toList();
                    map.put("imageUrls", imageUrls);

                    // Seller info
                    if (p.getSeller() != null) {
                        SellerBO s = p.getSeller();
                        map.put("sellerId", s.getSellerId());
                        map.put("sellerName", s.getFullName());
                        map.put("sellerEmail", s.getEmail());
                        map.put("sellerVerified", s.isVerified());
                    }

                    return map;
                })
                .toList();

        // 4 — Build final response body
        Map<String, Object> responseData = new LinkedHashMap<>();
        responseData.put("products", result);
        responseData.put("page", productsPage.getNumber());
        responseData.put("size", productsPage.getSize());
        responseData.put("totalElements", productsPage.getTotalElements());
        responseData.put("totalPages", productsPage.getTotalPages());
        responseData.put("isLast", productsPage.isLast());
        responseData.put("isFirst", productsPage.isFirst());

        // 5 — Build BaseRs wrapper
        BaseDataRs dataRs = new BaseDataRs("Admin product list fetched", responseData);

        BaseRs response = new BaseRs();
        response.setStatus("SUCCESS");
        response.setData(dataRs);

        return response;
    }


    // ======================================================================
    // SELLER: PRODUCT DETAILS FOR EDITING
    // ======================================================================
    @Override
    public ProductRs getProductDetailsForSeller(Long productId) throws Exception {
        ProductBO bo = productRepo.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Long sellerId = AuthUtils.findLoggedInUser().getDocId();
        String role = AuthUtils.getLoggedUserRole();

        if (!role.equalsIgnoreCase("ADMIN")
                && !bo.getSeller().getId().equals(sellerId)) {
            throw new RuntimeException("Unauthorized access");
        }

        return productMapper.toProductRs(bo);
    }

    // ======================================================================
    // PUBLIC: PRODUCT DETAILS PAGE
    // ======================================================================
    @Override
    public ProductDetailsRs getProductDetails(String slug) {

        ProductBO product = productRepo.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (!product.isActive()) {
            throw new RuntimeException("Product is not available");
        }

        // ONLY product data here (NO reviews)
        return productMapper.toDetails(product);
    }

    @Override
    public Page<ReviewRs> getProductReviewsBySlug(String slug, Pageable pageable) {

        return reviewRepo.findByProduct_Slug(slug, pageable)
                .map(productMapper::toReviewRs);
    }







    // ======================================================================
    // PUBLIC: SHARE PRODUCT MESSAGE
    // ======================================================================
    @Override
    public String getShareableProduct(String slug) {
        try {
            ProductBO bo = productRepo.findBySlug(slug).orElse(null);

            if (bo == null) return "Product not found";

            return buildShareMessage(bo);

        } catch (Exception e) {
            log.error("getShareableProduct() failed", e);
            return "Error generating share message";
        }
    }

    // ======================================================================
    // PUBLIC: GENERAL PRODUCT LIST
    // ======================================================================
    @Override
    public BaseRs listProducts(int offset, int limit) {
        try {
            if (limit <= 0) limit = 20;
            if (offset < 0) offset = 0;

            int page = offset / limit;
            Pageable pageable = PageRequest.of(page, limit);

            Page<ProductBO> pageData =
                    productRepo.findByActiveTrueOrderByCreatedAtDesc(pageable);

            return ResponseUtils.success(
                    new ProductDataRsList(
                            "Products retrieved successfully",
                            productMapper.toProductRsList(pageData.getContent())
                    )
            );

        } catch (Exception e) {
            log.error("listProducts() failed", e);
            return ResponseUtils.failure(
                    ErrorCodes.EC_INTERNAL_ERROR,
                    e.getMessage()
            );
        }
    }


    // ======================================================================
    // PUBLIC: CATEGORY PRODUCTS
    // ======================================================================
    @Override
    public BaseRs getProductsByCategory(String category, int offset, int limit) {
        try {
            if (Utils.isEmpty(category)) {
                return ResponseUtils.failure(ErrorCodes.EC_INVALID_INPUT, "Category is required");
            }

            Pageable pageable = PageRequest.of(offset / limit, limit);
            Page<ProductBO> page = productRepo.findByCategoryNameIgnoreCase(category, pageable);

            if (page.isEmpty()) {
                return ResponseUtils.failure(ErrorCodes.EC_NO_RECORDS_FOUND, "No products found for this category");
            }

            return ResponseUtils.success(
                    new ProductDataRsList("Products retrieved successfully",
                            productMapper.toProductRsList(page.getContent())));  // ← CHANGED

        } catch (Exception e) {
            log.error("getProductsByCategory() failed", e);
            return ResponseUtils.failure(ErrorCodes.EC_INTERNAL_ERROR, e.getMessage());
        }
    }


    // ======================================================================
    // PUBLIC: SEARCH PRODUCTS
    // ======================================================================
    @Override
    public BaseRs searchProducts(String name, int offset, int limit) {
        try {
            Pageable pageable = PageRequest.of(offset, limit);
            Page<ProductBO> page = productRepo.findByNameContainingIgnoreCase(name, pageable);

            return ResponseUtils.success(
                    new ProductDataRsList("Products retrieved",
                            productMapper.toProductRsList(page.getContent())));  // ← CHANGED

        } catch (Exception e) {
            log.error("searchProducts() failed", e);
            return ResponseUtils.failure(ErrorCodes.EC_INTERNAL_ERROR, e.getMessage());
        }
    }


    // ======================================================================
    // PUBLIC: HOMEPAGE PRODUCT LIST
    // ======================================================================
    @Override
    public Page<ProductCardRs> getProducts(String category, int page, int size, String sort) {

        if (page < 0) page = 0;
        if (size <= 0) size = 12;
        if (size > 50) size = 50;

        Sort sortSpec;

        if (sort != null && sort.contains(",")) {
            String[] sortParts = sort.split(",");
            sortSpec = "asc".equalsIgnoreCase(sortParts[1])
                    ? Sort.by(sortParts[0]).ascending()
                    : Sort.by(sortParts[0]).descending();
        } else {
            sortSpec = Sort.by("createdAt").descending();
        }

        Pageable pageable = PageRequest.of(page, size, sortSpec);

        Page<ProductBO> pageData =
                (category == null || category.isBlank())
                        ? productRepo.findByActiveTrueOrderByCreatedAtDesc(pageable)
                        : productRepo.findByActiveTrueAndCategoryNameIgnoreCase(category, pageable);

        return pageData.map(productMapper::toCardRs);
    }


    // ======================================================================
    // PUBLIC: HOMEPAGE SEARCH PRODUCTS
    // ======================================================================
    @Override
    public Page<ProductCardRs> searchProducts(String q, String category, int page, int size) throws Exception {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<ProductBO> pageData;

        if (category == null || category.isBlank()) {
            pageData = productRepo.findByActiveTrueAndNameContainingIgnoreCase(q, pageable);
        } else {
            pageData = productRepo.findByActiveTrueAndNameContainingIgnoreCaseAndCategoryNameIgnoreCase(
                    q, category, pageable
            );
        }

        return pageData.map(productMapper::toCardRs);  // ← CHANGED
    }

    // ======================================================================
    // PUBLIC: SHOP FILTER SYSTEM (CATEGORY, PRICE, BRAND, RATING, SORT)
    // ======================================================================
    @Override
    public Page<ProductCardRs> filterProducts(
            String category,
            String brand,
            Integer minPrice,
            Integer maxPrice,
            Integer rating,
            String sort,
            int page,
            int size
    ) {
        Sort sortSpec = Sort.by(sort.split(",")[0]);
        if (sort.endsWith("asc")) {
            sortSpec = sortSpec.ascending();
        } else {
            sortSpec = sortSpec.descending();
        }

        Pageable pageable = PageRequest.of(page, size, sortSpec);

        List<String> categories = null;
        if (category != null && !category.isBlank()) {
            categories = Arrays.stream(category.split(","))
                    .map(String::toLowerCase)
                    .toList();
        }

        List<String> brands = null;
        if (brand != null && !brand.isBlank()) {
            brands = Arrays.stream(brand.split(","))
                    .map(String::toLowerCase)
                    .toList();
        }

        Page<ProductBO> pageData = productRepo.filterProducts(
                categories, brands, minPrice, maxPrice, rating, pageable
        );

        return pageData.map(productMapper::toCardRs);  // ← CHANGED
    }

    @Override
    public Page<ProductCardRs> getBestSellers(int page, int size, String sort) {

        Pageable pageable = PageRequest.of(page, size, parseSort(sort));

        return productRepo.findByActiveTrueAndBestSellerTrue(pageable)
                .map(productMapper::toCardRs);
    }

    @Override
    public Page<ProductCardRs> getNewArrivals(int page, int size, String sort) {

        LocalDateTime fromDate = LocalDateTime.now().minusDays(30);
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));

        return productRepo.findNewArrivals(fromDate, pageable)
                .map(productMapper::toCardRs);
    }


    @Override
    public Page<ProductCardRs> getSaleProducts(int page, int size, String sort) {

        Pageable pageable = PageRequest.of(page, size, parseSort(sort));

        return productRepo.findByActiveTrueAndOnSaleTrue(pageable)
                .map(productMapper::toCardRs);
    }

    @Override
    @Transactional
    public BaseRs reorderProductImages(Long productId, List<Long> imageIds) {
        try {
            Long sellerId = AuthUtils.getLoggedUserId();

            ProductBO product = productRepo.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            if (!product.getSeller().getId().equals(sellerId)) {
                return ResponseUtils.failure(ErrorCodes.EC_UNAUTHORIZED, "Unauthorized");
            }

            List<Long> existing = product.getImageFileIds();

            if (existing == null || existing.isEmpty()) {
                return ResponseUtils.failure("NO_IMAGES", "No images found");
            }

            if (existing.size() != imageIds.size()
                    || !existing.containsAll(imageIds)) {
                return ResponseUtils.failure("INVALID_ORDER", "Invalid image order");
            }

            //  Order defines thumbnail
            product.setImageFileIds(new ArrayList<>(imageIds));
            product.setThumbnailFileId(imageIds.get(0));

            productRepo.save(product);

            return ResponseUtils.success("Images reordered successfully");

        } catch (Exception e) {
            log.error("reorderProductImages() failed", e);
            return ResponseUtils.failure(ErrorCodes.EC_INTERNAL_ERROR, e.getMessage());
        }
    }





    // ======================================================================
    // INTERNAL HELPERS
    // ======================================================================
//    private String generateSlug(String name) {
//        String base = name.toLowerCase()
//                .replaceAll("[^a-z0-9]+", "-")
//                .replaceAll("^-|-$", "");
//        return base + "-" + UUID.randomUUID().toString().substring(0, 8);
//    }
//
//    private String generateUniqueSlug(String name) {
//        String slug = generateSlug(name);
//        while (productRepo.existsBySlug(slug)) {
//            slug = generateSlug(name);
//        }
//        return slug;
//    }

    private String buildShareMessage(ProductBO product) {
        return "Check out this product: " + product.getName()
                + "\n" + productFrontUrl + product.getSlug();
    }

    private SellerBO validateSellerAccess() {

        Long id = AuthUtils.getLoggedUserId();
        String role = AuthUtils.getLoggedUserRole();
        
        if ("ADMIN".equalsIgnoreCase(role)) {
            //return seller;
        	return null;
        }

        if (!"SELLER".equalsIgnoreCase(role)) {
            throw new RuntimeException("Only seller can access this resource.");
        }

        SellerBO seller = sellerRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Seller not found"));

        if (!seller.isEmailVerified()) {
            throw new RuntimeException("Please verify your email before performing this action.");
        }

        if (!seller.isVerified()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Your seller account is still pending admin approval."
            );
        }

        if (seller.isLocked()) {
            throw new RuntimeException("Your seller account has been locked by admin.");
        }

        return seller;
    }

    // ======================================================================
// HELPER: SEND PRODUCT UPDATE NOTIFICATION
// ======================================================================
    private void sendProductUpdateNotification(ProductBO product, Map<String, String> changes) {
        if (changes.isEmpty()) return;

        notificationService.notifyAdmins(
                "PRODUCT_UPDATED",
                "Product Updated",
                "", // Changes will be formatted in service
                product.getSeller(),
                product,
                null,
                "/admin/products/" + product.getSlug()
        );
    }

    private Sort parseSort(String sort) {

        // Default sort
        if (sort == null || sort.isBlank()) {
            return Sort.by("createdAt").descending();
        }

        String[] parts = sort.split(",");

        // Safety fallback
        if (parts.length != 2) {
            return Sort.by("createdAt").descending();
        }

        String field = parts[0];
        String direction = parts[1];

        // Whitelist allowed sort fields (VERY IMPORTANT)
        Set<String> allowedFields = Set.of(
                "price",
                "salePrice",
                "createdAt",
                "totalSold"
        );

        if (!allowedFields.contains(field)) {
            return Sort.by("createdAt").descending();
        }

        return "asc".equalsIgnoreCase(direction)
                ? Sort.by(field).ascending()
                : Sort.by(field).descending();
    }

    private FileBO saveProcessedImage(byte[] image) throws Exception {
        return fileService.uploadFile(
                new ByteArrayInputStream(image),
                "product_" + System.currentTimeMillis() + ".png",
                "image/png"
        );
    }



}
