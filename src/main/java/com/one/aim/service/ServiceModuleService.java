package com.one.aim.service;

import com.one.aim.rq.ServiceRq;
import com.one.aim.rs.ServiceCardRs;

import java.util.List;

public interface ServiceModuleService {

    List<ServiceCardRs> getTopServices(int limit);

    ServiceCardRs getServiceDetails(String slug);

    ServiceCardRs createService(ServiceRq rq) throws Exception;

    ServiceCardRs updateService(ServiceRq rq) throws Exception;

    void deleteService(Long id);
}

