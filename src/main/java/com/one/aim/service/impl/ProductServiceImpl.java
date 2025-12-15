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
import com.one.aim.rs.data.ProductDataRs;
import com.one.aim.rs.data.ProductDataRsList;
import com.one.aim.service.FileService;
import com.one.aim.service.NotificationService;
import com.one.aim.service.ProductService;
import com.one.aim.service.RecommendationService;
import com.one.exception.BaseException;
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

    @Value("${app.frontend.product.url}")
    private String productFrontUrl;

    // ======================================================================
    // SELLER: ADD PRODUCT
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
            // ========================
// SLUG GENERATION (HERE)
// ========================
            String slug = SlugUtils.from(rq.getName());
            int counter = 1;

            while (productRepo.existsBySlug(slug)) {
                slug = SlugUtils.from(rq.getName()) + "-" + counter++;
            }

            bo.setSlug(slug);
            bo.setDescription(rq.getDescription());
            bo.setPrice(rq.getPrice());
            bo.setStock(rq.getStock());
            bo.setBrand(rq.getBrand());

            bo.setSpecificationsJson(rq.getSpecificationsJson());


            // =====================
// CATEGORY HANDLING
// =====================
            if (rq.getCategoryId() != null) {

                CategoryBO category = categoryRepo.findById(rq.getCategoryId())
                        .orElseThrow(() ->
                                new RuntimeException("Category not found")
                        );

                bo.setCategoryId(category.getId());
                bo.setCategoryName(category.getName());

            } else {

                // Seller selected "Other"
                if (Utils.isEmpty(rq.getCustomCategoryName())) {
                    return ResponseUtils.failure(
                            ErrorCodes.EC_INVALID_INPUT,
                            "Category name is required"
                    );
                }

                // Check if category already exists
                Optional<CategoryBO> existing =
                        categoryRepo.findByNameIgnoreCase(rq.getCustomCategoryName());

                CategoryBO category;
                if (existing.isPresent()) {
                    category = existing.get();
                } else {
                    // Create new category (inactive by default)
                    category = CategoryBO.builder()
                            .name(rq.getCustomCategoryName().trim())
                            .slug(SlugUtils.from(rq.getCustomCategoryName()))
                            .active(false) // admin approval required
                            .build();

                    categoryRepo.save(category);
                }

                bo.setCategoryId(category.getId());
                bo.setCategoryName(category.getName());
            }


            // Images
            List<MultipartFile> images = rq.getImages();
            if (images != null && !images.isEmpty()) {

                if (images.size() > 5) {
                    return ResponseUtils.failure("TOO_MANY_IMAGES", "Maximum 5 images allowed");
                }

                for (MultipartFile file : images) {
                    if (file.isEmpty()) continue;

                    if (!List.of("image/jpeg", "image/png").contains(file.getContentType())) {
                        return ResponseUtils.failure("INVALID_FILE_TYPE", "Only JPG and PNG images are allowed");
                    }

                    if (file.getSize() > 2 * 1024 * 1024) {
                        return ResponseUtils.failure("FILE_TOO_LARGE", "File size must be <= 2MB");
                    }

                    FileBO uploaded = fileService.uploadAndReturnFile(file);
                    bo.getImageFileIds().add(uploaded.getId());
                }
            }

            if (bo.getImageFileIds().isEmpty()) {
                return ResponseUtils.failure("NO_IMAGE", "Product must have at least one image");
            }

            productRepo.save(bo);

            userActivityService.log(sellerId, "PRODUCT_CREATED", "Created product: " + bo.getName());

            notificationService.notifyAdmins(
                    "PRODUCT_ADDED",
                    "New Product Added",
                    bo.getName() + " added successfully!",
                    null,
                    bo,
                    null,
                    "/admin/products/" + bo.getSlug()
            );

            return ResponseUtils.success(new ProductDataRs("Product created successfully",
                    productMapper.toProductRs(bo)));

        } catch (Exception e) {
            log.error("addProduct() failed", e);
            return ResponseUtils.failure(ErrorCodes.EC_INTERNAL_ERROR, e.getMessage());
        }
    }

    // ======================================================================
    // SELLER: UPDATE PRODUCT
    // ======================================================================
    @Override
    @Transactional
    public BaseRs updateProduct(ProductRq rq) {
        try {
            validateSellerAccess();

            if (rq.getDocId() == null) {
                return ResponseUtils.failure(ErrorCodes.EC_REQUIRED_DOCID, "Product ID required");
            }

            Long productId = Long.valueOf(rq.getDocId());
            ProductBO product = productRepo.findById(productId).orElse(null);

            if (product == null) {
                return ResponseUtils.failure(ErrorCodes.EC_PRODUCT_NOT_FOUND, "Product not found");
            }

            Long sellerId = AuthUtils.findLoggedInUser().getDocId();
            if (!product.getSeller().getId().equals(sellerId)) {
                return ResponseUtils.failure(ErrorCodes.EC_UNAUTHORIZED, "Unauthorized");
            }

            // Active toggle only
            if (rq.getActive() != null &&
                    rq.getName() == null &&
                    rq.getDescription() == null &&
                    rq.getPrice() == null &&
                    rq.getStock() == null &&
                    rq.getCategoryId() == null &&
                    rq.getCustomCategoryName() == null) {

                product.setActive(rq.getActive());
                productRepo.save(product);

                if (!product.isActive()) {
                    cartRepo.deleteByProduct_Id(product.getId());
                }

                return ResponseUtils.success(new ProductDataRs("Product status updated",
                        productMapper.toProductRs(product)));
            }

            // Full update
            if (Utils.isNotEmpty(rq.getName())) {
                product.setName(rq.getName());
                product.setSlug(SlugUtils.from(rq.getName()));
            }

            if (Utils.isNotEmpty(rq.getDescription())) {
                product.setDescription(rq.getDescription());
            }

            if (rq.getPrice() != null && rq.getPrice() > 0) {
                product.setPrice(rq.getPrice());
            }

            boolean stockChanged = false;

            if (rq.getStock() != null && rq.getStock() >= 0) {
                product.setStock(rq.getStock());
                product.updateLowStock();
                stockChanged = true;
            }

            if (rq.getCategoryId() != null) {
                CategoryBO category = categoryRepo.findById(rq.getCategoryId()).orElse(null);
                if (category == null) {
                    return ResponseUtils.failure(ErrorCodes.EC_RECORD_NOT_FOUND, "Category not found");
                }
                product.setCategoryId(category.getId());
                product.setCategoryName(category.getName());
            } else if (Utils.isNotEmpty(rq.getCustomCategoryName())) {
                product.setCategoryId(null);
                product.setCategoryName(rq.getCustomCategoryName());
            }

            // Add images
            if (rq.getImages() != null && !rq.getImages().isEmpty()) {
                int existing = product.getImageFileIds().size();
                int incoming = rq.getImages().size();

                if (existing + incoming > 5) {
                    return ResponseUtils.failure("TOO_MANY_IMAGES", "You can only upload up to 5 images");
                }

                for (MultipartFile file : rq.getImages()) {
                    if (file.isEmpty()) continue;

                    if (!List.of("image/jpeg", "image/png").contains(file.getContentType())) {
                        return ResponseUtils.failure("INVALID_FILE_TYPE", "Only JPG and PNG allowed");
                    }

                    if (file.getSize() > 2 * 1024 * 1024) {
                        return ResponseUtils.failure("FILE_TOO_LARGE", "Max size 2MB allowed");
                    }

                    FileBO uploaded = fileService.uploadAndReturnFile(file);
                    product.getImageFileIds().add(uploaded.getId());
                }
            }

            productRepo.save(product);

            // Cart sync
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

            notificationService.notifyAdmins(
                    "PRODUCT_UPDATED",
                    "Product Updated",
                    product.getName(),
                    null,
                    product,
                    null,
                    "/admin/products/" + product.getId()
            );

            return ResponseUtils.success(new ProductDataRs("Product updated successfully",
                    productMapper.toProductRs(product)));

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
        validateSellerAccess();

        try {
            ProductBO bo = productRepo.findById(productId).orElse(null);

            if (bo == null) {
                return ResponseUtils.failure(ErrorCodes.EC_PRODUCT_NOT_FOUND, "Product not found");
            }

            Long sellerId = AuthUtils.findLoggedInUser().getDocId();
            if (!bo.getSeller().getId().equals(sellerId)) {
                return ResponseUtils.failure(ErrorCodes.EC_UNAUTHORIZED, "Unauthorized image upload");
            }

            List<Long> imageIds = new ArrayList<>();

            for (MultipartFile f : files) {
                if (!f.isEmpty()) {
                    FileBO uploaded = fileService.uploadAndReturnFile(f);
                    bo.getImageFileIds().add(uploaded.getId());
                    imageIds.add(uploaded.getId());
                }
            }

            productRepo.save(bo);

            userActivityService.log(sellerId, "PRODUCT_IMAGE_UPLOADED",
                    "Uploaded images for product: " + bo.getName());

            return ResponseUtils.success(new BaseDataRs("Images uploaded successfully", imageIds));

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
            if (productId == null || productId <= 0) {
                return ResponseUtils.failure(ErrorCodes.EC_INVALID_INPUT, "Invalid product ID");
            }

            ProductBO bo = productRepo.findById(productId).orElse(null);

            if (bo == null) {
                return ResponseUtils.failure(ErrorCodes.EC_PRODUCT_NOT_FOUND, "Product not found");
            }

            List<String> imageUrls = bo.getImageFileIds().stream()
                    .map(id -> "/api/files/public/" + id + "/view")
                    .toList();

            return ResponseUtils.success(new BaseDataRs("Product images retrieved", imageUrls));

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
            Long sellerId = AuthUtils.findLoggedInUser().getDocId();

            ProductBO product = productRepo.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            if (!product.getSeller().getId().equals(sellerId)) {
                return ResponseUtils.failure(
                        ErrorCodes.EC_UNAUTHORIZED, "You are not allowed to modify this product"
                );
            }

            List<Long> imgList = product.getImageFileIds();

            if (imgList.isEmpty()) {
                return ResponseUtils.failure(ErrorCodes.EC_IMAGE_NOT_FOUND, "No images found");
            }

            if (imgList.size() == 1) {
                return ResponseUtils.failure("LAST_IMAGE", "At least one image must remain");
            }

            if (!imgList.contains(imageId)) {
                return ResponseUtils.failure(ErrorCodes.EC_IMAGE_NOT_FOUND, "Image does not belong to this product");
            }

            imgList.remove(imageId);
            productRepo.save(product);

            fileService.deleteFileById(String.valueOf(imageId));

            userActivityService.log(sellerId,
                    "PRODUCT_IMAGE_DELETED",
                    "Deleted image " + imageId + " from product: " + product.getName());

            return ResponseUtils.success("Image deleted successfully");

        } catch (Exception e) {
            log.error("deleteProductImage() failed", e);
            return ResponseUtils.failure(ErrorCodes.EC_INTERNAL_ERROR, "Failed to delete product image");
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

            Long sellerId = AuthUtils.getLoggedUserId();
            if (!bo.getSeller().getId().equals(sellerId)) {
                return ResponseUtils.failure(ErrorCodes.EC_UNAUTHORIZED, "Unauthorized");
            }

            bo.setActive(false);
            productRepo.save(bo);

            userActivityService.log(sellerId,
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
        Page<ProductBO> productsPage = productRepo.findAll(pageable);

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

        return productMapper.toDetails(product);
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

            Pageable pageable = PageRequest.of(offset / limit, limit);
            Page<ProductBO> pageData = productRepo.findByActiveTrue(pageable);

            return ResponseUtils.success(
                    new ProductDataRsList("Products retrieved successfully",
                            productMapper.toProductRsList(pageData.getContent())));  // ← CHANGED

        } catch (Exception e) {
            log.error("listProducts() failed", e);
            return ResponseUtils.failure(ErrorCodes.EC_INTERNAL_ERROR, e.getMessage());
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
    public Page<ProductCardRs> getProducts(String category, int page, int size, String sort) throws Exception {
        Sort sortSpec;
        try {
            String[] sortParts = sort.split(",");
            sortSpec = sortParts[1].equalsIgnoreCase("asc")
                    ? Sort.by(sortParts[0]).ascending()
                    : Sort.by(sortParts[0]).descending();
        } catch (Exception e) {
            sortSpec = Sort.by("createdAt").descending();
        }

        Pageable pageable = PageRequest.of(page, size, sortSpec);

        Page<ProductBO> pageData =
                (category == null || category.isBlank())
                        ? productRepo.findByActiveTrue(pageable)
                        : productRepo.findByActiveTrueAndCategoryNameIgnoreCase(category, pageable);

        return pageData.map(productMapper::toCardRs);  // ← CHANGED
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

}
