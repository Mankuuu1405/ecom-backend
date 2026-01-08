package com.one.aim.service;

import com.one.aim.rq.ServiceCreateRq;
import com.one.aim.rq.ServiceRq;
import com.one.aim.rs.ServiceCardRs;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ServiceModuleService {

    List<ServiceCardRs> getTopServices(int limit);

    ServiceCardRs getServiceDetails(String slug);

    ServiceCardRs createService(ServiceRq rq) throws Exception;

    ServiceCardRs updateService(ServiceRq rq) throws Exception;

    void deleteService(Long id);

    void createService(ServiceCreateRq rq, MultipartFile image) throws Exception;
}

