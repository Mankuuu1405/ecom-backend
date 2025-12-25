package com.one.aim.service;

import com.one.aim.rs.CombinedUserRs;
import java.util.List;

public interface CombinedUserService {
    List<CombinedUserRs> getAllUsersAndSellers();
}