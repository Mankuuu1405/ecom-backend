package com.one.aim.service.impl;

import com.one.aim.bo.ProductBO;
import com.one.aim.bo.ReviewBO;
import com.one.aim.bo.UserBO;
import com.one.aim.repo.OrderRepo;
import com.one.aim.repo.ProductRepo;
import com.one.aim.repo.ReviewRepository;
import com.one.aim.repo.UserRepo;
import com.one.aim.rq.ReviewRq;
import com.one.aim.rq.ReviewUpdateRq;
import com.one.aim.rs.ReviewRs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepo;
    private final ProductRepo productRepo;
    private final UserRepo userRepo;
    private final ProductRatingService productRatingService;
    private final OrderRepo orderRepo;

    // ======================================================
    // ADD REVIEW
    // ======================================================
    @Transactional
    public void addReview(ReviewRq rq) {

        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        UserBO user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ProductBO product = productRepo.findById(rq.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // CHECK PURCHASE
        boolean hasPurchased =
                orderRepo.hasUserPurchasedProduct(user.getId(), product.getId());

        if (!hasPurchased) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can review this product only after delivery"
            );
        }

        // UPSERT REVIEW (ADD OR UPDATE)
        ReviewBO review =
                reviewRepo.findByUser_IdAndProduct_Id(user.getId(), product.getId())
                        .orElseGet(() -> {
                            ReviewBO r = new ReviewBO();
                            r.setUser(user);
                            r.setProduct(product);
                            r.setVerified(true);
                            return r;
                        });


        review.setRating(rq.getRating());
        review.setComment(rq.getComment());

        reviewRepo.save(review);

        productRatingService.updateProductRating(product.getId());
    }




    // ======================================================
    // UPDATE REVIEW
    // ======================================================
    @Transactional
    public void updateReview(ReviewUpdateRq rq) {

        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        ReviewBO review = reviewRepo.findById(rq.getId())
                .orElseThrow(() -> new RuntimeException("Review not found"));

        if (!review.getUser().getEmail().equals(email)) {
            throw new RuntimeException("You can only update your own reviews");
        }

        review.setRating(rq.getRating());
        review.setComment(rq.getComment());

        reviewRepo.save(review);

        productRatingService.updateProductRating(
                review.getProduct().getId()
        );
    }

    // ======================================================
    // DELETE REVIEW
    // ======================================================
    @Transactional
    public void deleteReview(Long id) {

        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        ReviewBO review = reviewRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        if (!review.getUser().getEmail().equals(email)) {
            throw new RuntimeException("You can only delete your own reviews");
        }

        Long productId = review.getProduct().getId();

        reviewRepo.delete(review);

        productRatingService.updateProductRating(productId);
    }

    // ======================================================
    // GET REVIEWS BY PRODUCT
    // ======================================================
    public Page<ReviewRs> getReviewsByProductSlug(String slug, int page, int size) {

        ProductBO product = productRepo.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Pageable pageable = PageRequest.of(
                page, size, Sort.by("createdAt").descending()
        );

        Page<ReviewBO> reviews =
                reviewRepo.findByProduct_Id(product.getId(), pageable);

        return reviews.map(this::toRs);
    }

    // ======================================================
    // GET USER REVIEWS
    // ======================================================
    public Page<ReviewRs> getUserReviews(int page, int size) {

        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        UserBO user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Pageable pageable = PageRequest.of(
                page, size, Sort.by("createdAt").descending()
        );

        Page<ReviewBO> reviews =
                reviewRepo.findByUser_Id(user.getId(), pageable);

        return reviews.map(this::toRs);
    }

    // ======================================================
    // LIKE / DISLIKE
    // ======================================================
    @Transactional
    public void likeReview(Long reviewId) {

        ReviewBO review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        review.setLikes(
                review.getLikes() == null ? 1 : review.getLikes() + 1
        );

        reviewRepo.save(review);
    }

    @Transactional
    public void dislikeReview(Long reviewId) {

        ReviewBO review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        review.setDislikes(
                review.getDislikes() == null ? 1 : review.getDislikes() + 1
        );

        reviewRepo.save(review);
    }

    private void recalculateProductRating(ProductBO product) {

        Double avg = reviewRepo.getAverageRatingByProductId(product.getId());
        Long count = reviewRepo.getReviewCountByProductId(product.getId());

        product.setAverageRating(avg != null ? avg : 0.0);
        product.setReviewCount(count != null ? count : 0);

        productRepo.save(product);
        productRepo.flush();
    }


    // ======================================================
    // MAPPER
    // ======================================================
    private ReviewRs toRs(ReviewBO bo) {

        ReviewRs rs = new ReviewRs();

        rs.setId(bo.getId());
        rs.setProductId(bo.getProduct().getId());
        rs.setProductName(bo.getProduct().getName());
        rs.setProductSlug(bo.getProduct().getSlug());
        rs.setUserId(bo.getUser().getId());
        rs.setUserName(bo.getUser().getFullName());

        rs.setRating(bo.getRating());
        rs.setComment(bo.getComment());

        rs.setDate(
                bo.getCreatedAt()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        );

        rs.setLikes(bo.getLikes());
        rs.setDislikes(bo.getDislikes());
        rs.setVerifiedPurchase(bo.isVerified());

        return rs;
    }
}
