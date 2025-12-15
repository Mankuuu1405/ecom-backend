package com.one.aim.service.impl;

import com.one.aim.bo.ProductBO;
import com.one.aim.mapper.ProductMapper;
import com.one.aim.repo.OrderItemBORepo;
import com.one.aim.repo.ProductRepo;
import com.one.aim.rs.ProductCardRs;
import com.one.aim.rs.RecommendationRs;
import com.one.aim.service.FileService;
import com.one.aim.service.RecommendationService;
import com.one.utils.AuthUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private final OrderItemBORepo orderItemRepo;
    private final ProductRepo productRepo;
    private final FileService fileService;
    private final ProductMapper productMapper;

    // ========== SIMPLE VERSIONS (for homepage, returns RecommendationRs) ==========

    @Override
    public List<RecommendationRs> getFrequentlyBoughtTogetherSimple(Long productId, int limit) {
        var rows = orderItemRepo.findFrequentlyBoughtTogether(productId, PageRequest.of(0, limit));
        return rowsToRs(rows);
    }

    @Override
    public List<RecommendationRs> getPeopleAlsoBoughtSimple(Long productId, int limit) {
        var rows = orderItemRepo.findPeopleAlsoBought(productId, PageRequest.of(0, limit));
        return rowsToRs(rows);
    }

    @Override
    public List<RecommendationRs> getRecommendedForUserSimple(int limit) {
        Long userId = AuthUtils.findLoggedInUser().getDocId();
        List<Long> purchasedIds = orderItemRepo.findProductIdsPurchasedByUser(userId);

        LocalDateTime start = LocalDateTime.now().minusMonths(3);
        List<Object[]> trending = orderItemRepo.findTopSelling(
                start,
                LocalDateTime.now(),
                PageRequest.of(0, 100)
        );

        Map<Long, Long> scoreMap = new LinkedHashMap<>();
        for (Object[] row : trending) {
            Long pId = ((Number) row[0]).longValue();
            Long score = ((Number) row[2]).longValue();

            if (purchasedIds.contains(pId)) continue;

            scoreMap.put(pId, score);
            if (scoreMap.size() >= limit) break;
        }

        return fetchProductsFromIds(scoreMap);
    }

    @Override
    public List<RecommendationRs> getTrendingSimple(int days, int limit) {
        LocalDateTime start = LocalDateTime.now().minusDays(days);
        List<Object[]> rows = orderItemRepo.findTopSelling(start, LocalDateTime.now(), PageRequest.of(0, limit));
        return rowsToRs(rows);
    }

    @Override
    public List<RecommendationRs> getTopByCategorySimple(String categoryOrSlug, int limit) {
        String category = categoryOrSlug.replace("-", " ");
        LocalDateTime start = LocalDateTime.now().minusMonths(3);

        List<Object[]> rows = orderItemRepo.findTopSellingByCategory(
                category,
                start,
                LocalDateTime.now(),
                PageRequest.of(0, limit)
        );

        return rowsToRs(rows);
    }

    // ========== FULL VERSIONS (for product detail page, returns ProductCardRs) ==========

    @Override
    public List<ProductCardRs> getFrequentlyBoughtTogether(Long productId, int limit) {
        var rows = orderItemRepo.findFrequentlyBoughtTogether(productId, PageRequest.of(0, limit));
        return rowsToProductCards(rows);
    }

    @Override
    public List<ProductCardRs> getPeopleAlsoBought(
            Long productId,
            String categoryName,
            int limit
    ) {

        List<Object[]> rows =
                orderItemRepo.findPeopleAlsoBought(
                        productId,
                        PageRequest.of(0, 20)
                );

        //  Filter same category
        List<ProductBO> products = rows.stream()
                .map(r -> ((Number) r[0]).longValue())
                .distinct()
                .map(productRepo::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(p ->
                        p.isActive()
                                && p.getCategoryName() != null
                                && p.getCategoryName().equalsIgnoreCase(categoryName)
                )
                .toList();

        //  Sort by purchase frequency (rank)
        Map<Long, Long> scoreMap = rows.stream()
                .collect(Collectors.toMap(
                        r -> ((Number) r[0]).longValue(),
                        r -> ((Number) r[2]).longValue(),
                        Long::max
                ));

        return products.stream()
                .sorted((a, b) ->
                        scoreMap.getOrDefault(b.getId(), 0L)
                                .compareTo(scoreMap.getOrDefault(a.getId(), 0L))
                )
                .limit(limit)
                .map(productMapper::toCardRs)
                .toList();
    }



    @Override
    public List<ProductCardRs> getRecommendedForUser(int limit) {
        Long userId = AuthUtils.findLoggedInUser().getDocId();
        List<Long> purchasedIds = orderItemRepo.findProductIdsPurchasedByUser(userId);

        LocalDateTime start = LocalDateTime.now().minusMonths(3);
        List<Object[]> trending = orderItemRepo.findTopSelling(
                start,
                LocalDateTime.now(),
                PageRequest.of(0, 100)
        );

        List<Long> recommendedIds = new ArrayList<>();
        for (Object[] row : trending) {
            Long pId = ((Number) row[0]).longValue();

            if (purchasedIds.contains(pId)) continue;

            recommendedIds.add(pId);
            if (recommendedIds.size() >= limit) break;
        }

        return fetchProductCardsFromIds(recommendedIds);
    }

    @Override
    public List<ProductCardRs> getTrending(int days, int limit) {
        LocalDateTime start = LocalDateTime.now().minusDays(days);
        List<Object[]> rows = orderItemRepo.findTopSelling(start, LocalDateTime.now(), PageRequest.of(0, limit));
        return rowsToProductCards(rows);
    }

    @Override
    public List<ProductCardRs> getTopByCategory(String categoryOrSlug, int limit) {
        String category = categoryOrSlug.replace("-", " ");
        LocalDateTime start = LocalDateTime.now().minusMonths(3);

        List<Object[]> rows = orderItemRepo.findTopSellingByCategory(
                category,
                start,
                LocalDateTime.now(),
                PageRequest.of(0, limit)
        );

        return rowsToProductCards(rows);
    }

    // ========== HELPER METHODS ==========

//    private List<RecommendationRs> rowsToRs(List<Object[]> rows) {
//        if (rows == null || rows.isEmpty()) return Collections.emptyList();
//
//        return rows.stream().map(r -> {
//            Long productId = ((Number) r[0]).longValue();
//            String name = r[1] == null ? "" : r[1].toString();
//            Long score = r[2] == null ? 0L : ((Number) r[2]).longValue();
//            Double price = productRepo.findById(productId)
//                    .map(p -> p.getPrice() == null ? 0.0 : p.getPrice())
//                    .orElse(0.0);
//            return new RecommendationRs(productId, name, price, score);
//        }).collect(Collectors.toList());
//    }

    private List<RecommendationRs> fetchProductsFromIds(Map<Long, Long> scoreMap) {
        List<Long> ids = new ArrayList<>(scoreMap.keySet());
        List<ProductBO> products = productRepo.findAllById(ids);
        Map<Long, ProductBO> map = products.stream()
                .collect(Collectors.toMap(ProductBO::getId, p -> p));

        List<RecommendationRs> res = new ArrayList<>();
        for (Long id : ids) {
            ProductBO p = map.get(id);
            if (p == null) continue;
            res.add(new RecommendationRs(
                    id,
                    p.getName(),
                    p.getPrice() == null ? 0.0 : p.getPrice(),
                    scoreMap.get(id)
            ));
        }
        return res;
    }

    private List<ProductCardRs> rowsToProductCards(List<Object[]> rows) {
        if (rows == null || rows.isEmpty()) return Collections.emptyList();

        List<Long> productIds = rows.stream()
                .map(r -> ((Number) r[0]).longValue())
                .collect(Collectors.toList());

        return fetchProductCardsFromIds(productIds);
    }

    private List<ProductCardRs> fetchProductCardsFromIds(List<Long> ids) {

        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        List<ProductBO> products = productRepo.findAllById(ids);

        Map<Long, ProductBO> productMap = products.stream()
                .collect(Collectors.toMap(ProductBO::getId, p -> p));

        return ids.stream()
                .map(productMap::get)
                .filter(Objects::nonNull)
                .filter(ProductBO::isActive)
                .map(productMapper::toCardRs)
                .toList();
    }

    private List<RecommendationRs> rowsToRs(List<Object[]> rows) {

        return rows.stream().map(r -> {

            Long productId = ((Number) r[0]).longValue();
            String name = r[1].toString();
            Long score = ((Number) r[2]).longValue(); // order count

            Double price = productRepo.findById(productId)
                    .map(p -> p.getPrice() == null ? 0.0 : p.getPrice())
                    .orElse(0.0);

            return new RecommendationRs(
                    productId,
                    name,
                    price,
                    score
            );
        }).toList();
    }


}