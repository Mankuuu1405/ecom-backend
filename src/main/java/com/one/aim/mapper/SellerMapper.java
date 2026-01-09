package com.one.aim.mapper;

import java.util.ArrayList;
import java.util.List;

import com.one.aim.bo.SellerBO;
import com.one.aim.bo.VendorBO;
import com.one.aim.rs.SellerRs;
import com.one.aim.rs.VendorRs;
import com.one.utils.UrlUtils;
import com.one.utils.Utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class SellerMapper {

    private final UrlUtils urlUtils;

    public SellerRs mapToSellerRs(SellerBO bo) {

        if (bo == null) return null;

        SellerRs rs = new SellerRs();

        rs.setDocId(String.valueOf(bo.getId()));
        rs.setSellerId(bo.getSellerId());

        rs.setUserName(bo.getFullName());
        rs.setEmail(bo.getEmail());
        rs.setPhoneNo(bo.getPhoneNo());
        rs.setGst(bo.getGst());
        rs.setAdhaar(bo.getAdhaar());
        rs.setPanCard(bo.getPanCard());
        rs.setRole(bo.getRole());
        rs.setVerified(bo.isVerified());
        rs.setRejected(bo.isRejected());
        rs.setLocked(bo.isLocked());
        rs.setEmailVerified(bo.isEmailVerified());
        rs.setCreatedAt(bo.getCreatedAt());

        if (bo.getImageFileId() != null) {
            rs.setImageUrl(urlUtils.privateFile(bo.getImageFileId()));
        }

        rs.setResumePdfUrl("/aimdev/api/admin/seller/" + bo.getSellerId() + "/resume.pdf");



        return rs;
    }


    public List<SellerRs> mapToSellerRsList(List<SellerBO> bos) {
        if (bos == null || bos.isEmpty()) {
            log.warn("SellerBO list is empty");
            return List.of();
        }

        return bos.stream()
                .map(this::mapToSellerRs)
                .toList();
    }
}
