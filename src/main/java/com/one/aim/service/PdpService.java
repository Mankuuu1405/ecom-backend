package com.one.aim.service;

import com.one.aim.rs.PdpRs;

public interface PdpService {
    PdpRs getPdpBySlug(String slug) throws Exception;
}

