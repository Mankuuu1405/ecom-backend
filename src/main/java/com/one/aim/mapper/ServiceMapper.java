package com.one.aim.mapper;

import com.one.aim.bo.ServiceBO;
import com.one.aim.rs.ServiceCardRs;
import com.one.utils.UrlUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ServiceMapper {

    private final UrlUtils urlUtils;

    public ServiceCardRs toCard(ServiceBO bo) {
        ServiceCardRs rs = new ServiceCardRs();

        rs.setId(bo.getId());
        rs.setTitle(bo.getTitle());
        rs.setDescription(bo.getDescription());
        rs.setStartingPrice(bo.getStartingPrice());
        rs.setSlug(bo.getSlug());


        rs.setImage(
                bo.getImageFileId() != null
                        ? urlUtils.publicFile(bo.getImageFileId())
                        : urlUtils.defaultImage()
        );

        return rs;
    }

}

