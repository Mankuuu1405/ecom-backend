package com.one.aim.mapper;

import com.one.aim.bo.ServiceBO;
import com.one.aim.rs.ServiceCardRs;

public class ServiceMapper {

    public static ServiceCardRs toCard(ServiceBO bo) {
        ServiceCardRs rs = new ServiceCardRs();

        rs.setId(bo.getId());
        rs.setTitle(bo.getTitle());  // Map title → name
        rs.setDescription(bo.getDescription());
        rs.setStartingPrice(bo.getStartingPrice());  // Map startingPrice → price

        // Generate image URL
        if (bo.getImageFileId() != null) {
            rs.setImage("/api/files/public/" + bo.getImageFileId() + "/view");
        } else {
            rs.setImage("/assets/default-service.jpg");
        }

        // Set default/placeholder values for missing fields
//        rs.setRating(0.0);  // Default rating or calculate from reviews
//        rs.setProvider("Platform");  // Default provider or get from related entity

        return rs;
    }
}

