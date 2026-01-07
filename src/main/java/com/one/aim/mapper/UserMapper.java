package com.one.aim.mapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.one.aim.bo.UserBO;
import com.one.aim.rs.UserRs;
import com.one.utils.UrlUtils;
import com.one.utils.Utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

//this data for front-end person
@Slf4j
@RequiredArgsConstructor
@Component
public class UserMapper {
    private final UrlUtils urlUtils;

    public  UserRs mapToUserRs(UserBO bo) {

        UserRs rs = new UserRs();
//
//        rs.setDocId(bo.getId());
//        rs.setFullName(bo.getFullName());
//        rs.setPhoneNo(bo.getPhoneNo());
//        rs.setEmail(bo.getEmail());
//        rs.setRoll(bo.getRole());
//
//        // NEW — convert fileId → URL
//        if (bo.getImageFileId() != null) {
//            rs.setImageUrl(urlUtils.privateFile(bo.getImageFileId()));
//        }
//
//
//        return rs;
    	
    	rs.setDocId(bo.getId());
        rs.setFullName(bo.getFullName());
        rs.setEmail(bo.getEmail());
        rs.setPhoneNo(bo.getPhoneNo());
        rs.setRoll(bo.getRole());

        rs.setActive(bo.getActive());
        rs.setEmailVerified(bo.getEmailVerified());

        rs.setCreatedAt(bo.getCreatedAt());

        // Image URL (example logic)
        if (bo.getImageFileId() != null) {
            rs.setImageUrl("/api/file/" + bo.getImageFileId());
        }

        return rs;
    }

    public List<UserRs> mapToUserRsList(List<UserBO> bos) {

        if (bos == null || bos.isEmpty()) {
            return Collections.emptyList();
        }

        return bos.stream()
                .map(this::mapToUserRs)
                .toList();
    }

}