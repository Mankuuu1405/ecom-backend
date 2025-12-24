package com.one.aim.service.impl;

import com.one.aim.mapper.CombinedUserMapper;
import com.one.aim.repo.SellerRepo;
import com.one.aim.repo.UserRepo;
import com.one.aim.rs.CombinedUserRs;
import com.one.aim.service.CombinedUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CombinedUserServiceImpl implements CombinedUserService {

    private final UserRepo userRepo;
    private final SellerRepo sellerRepo;
    private final CombinedUserMapper mapper;

    @Override
    public List<CombinedUserRs> getAllUsersAndSellers() {
        List<CombinedUserRs> combined = new ArrayList<>();

        userRepo.findAll().forEach(user -> combined.add(mapper.mapUser(user)));
        sellerRepo.findAll().forEach(seller -> combined.add(mapper.mapSeller(seller)));

        return combined;
    }
}
