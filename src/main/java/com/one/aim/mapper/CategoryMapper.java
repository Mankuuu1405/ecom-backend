package com.one.aim.mapper;

import com.one.aim.bo.CategoryBO;
import com.one.aim.rq.CategoryRq;
import com.one.aim.rs.CategoryCardRs;
import com.one.aim.rs.CategoryRs;
import com.one.utils.UrlUtils;
import com.one.vm.utils.SlugUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CategoryMapper {

    private final UrlUtils urlUtils;

    public  CategoryCardRs toCardRs(CategoryBO bo, Long productCount) {
        CategoryCardRs rs = new CategoryCardRs();

        rs.setId(bo.getId());
        rs.setName(bo.getName());
        rs.setSlug(bo.getSlug());

        // Convert imageFileId to public URL
        if (bo.getImageFileId() != null) {
            rs.setImage(urlUtils.publicFile(bo.getImageFileId()));
        } else {
            rs.setImage(null); // or default placeholder
        }

        rs.setProductCount(productCount);

        return rs;
    }

    public CategoryRs toRs(CategoryBO bo) {
        CategoryRs rs = new CategoryRs();

        rs.setId(bo.getId());
        rs.setName(bo.getName());
        rs.setSlug(bo.getSlug());
        rs.setActive(bo.isActive());
        rs.setTaxPercent(bo.getTaxPercent());
        rs.setDeliveryCharge(bo.getDeliveryCharge());
        rs.setReturnPolicyDays(bo.getReturnPolicyDays());
        rs.setCreatedAt(bo.getCreatedAt());
        rs.setUpdatedAt(bo.getUpdatedAt());

        if (bo.getImageFileId() != null) {
            rs.setImageUrl(urlUtils.publicFile(bo.getImageFileId()));
        }

        return rs;
    }

    public CategoryBO toEntity(CategoryRq rq) {
        return CategoryBO.builder()
                .name(rq.getName())
                .slug(SlugUtils.from(rq.getName()))
                .active(rq.isActive())
                .build();
    }

    public void updateEntity(CategoryBO bo, CategoryRq rq) {
        if (rq.getName() != null && !rq.getName().isBlank()) {
            bo.setName(rq.getName());
            bo.setSlug(SlugUtils.from(rq.getName()));
        }
        bo.setActive(rq.isActive());
    }

    public CategoryCardRs toBrowseRs(CategoryBO bo, Long productCount) {
        CategoryCardRs rs = new CategoryCardRs();
        rs.setId(bo.getId());
        rs.setName(bo.getName());
        rs.setSlug(bo.getSlug());
        rs.setProductCount(productCount);

        if (bo.getImageFileId() != null) {
            rs.setImage(urlUtils.publicFile(bo.getImageFileId()));
        }

        return rs;
    }
}