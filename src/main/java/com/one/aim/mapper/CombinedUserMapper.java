package com.one.aim.mapper;

import com.one.aim.bo.SellerBO;
import com.one.aim.bo.UserBO;
import com.one.aim.rs.CombinedUserRs;
import org.springframework.stereotype.Component;

@Component
public class CombinedUserMapper {

    public CombinedUserRs mapUser(UserBO user) {
        CombinedUserRs rs = new CombinedUserRs();
        rs.setDocId(user.getId());
        rs.setFullName(user.getFullName());
        rs.setEmail(user.getEmail());
        rs.setPhoneNo(user.getPhoneNo());
        rs.setRole(user.getRole());
        rs.setActive(user.getActive());
        rs.setEmailVerified(user.getEmailVerified());
        rs.setImageUrl(user.getImageFileId() != null ? "/api/files/" + user.getImageFileId() : null);
        rs.setCreatedAt(user.getCreatedAt());
        return rs;
    }

    public CombinedUserRs mapSeller(SellerBO seller) {
        CombinedUserRs rs = new CombinedUserRs();
        rs.setDocId(seller.getId());
        rs.setFullName(seller.getFullName());
        rs.setEmail(seller.getEmail());
        rs.setPhoneNo(seller.getPhoneNo());
        rs.setRole("SELLER");
        rs.setActive(seller.isActive());
        rs.setEmailVerified(seller.isEmailVerified());
        rs.setImageUrl(seller.getImageFileId() != null ? "/api/files/" + seller.getImageFileId() : null);
        rs.setCreatedAt(seller.getCreatedAt());
        return rs;
    }
}
