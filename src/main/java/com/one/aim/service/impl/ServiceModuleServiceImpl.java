package com.one.aim.service.impl;

import com.one.aim.bo.FileBO;
import com.one.aim.bo.ServiceBO;
import com.one.aim.mapper.ServiceMapper;
import com.one.aim.repo.ServiceRepo;
import com.one.aim.rq.ServiceRq;
import com.one.aim.rs.ServiceCardRs;
import com.one.aim.service.FileService;
import com.one.aim.service.ServiceModuleService;
import com.one.vm.utils.SlugUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceModuleServiceImpl implements ServiceModuleService {

    private final ServiceRepo serviceRepo;
    private final FileService fileService;

    @Override
    public List<ServiceCardRs> getTopServices(int limit) {

        Pageable pageable = PageRequest.of(0, limit);

        return serviceRepo.findActiveServices(pageable)
                .stream()
                .map(ServiceMapper::toCard)
                .toList();
    }

    @Override
    public ServiceCardRs getServiceDetails(String slug) {
        ServiceBO bo = serviceRepo.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Service not found"));
        return ServiceMapper.toCard(bo);
    }

    @Override
    public ServiceCardRs createService(ServiceRq rq) throws Exception {

        ServiceBO bo = new ServiceBO();
        bo.setTitle(rq.getTitle());
        bo.setDescription(rq.getDescription());
        bo.setStartingPrice(rq.getStartingPrice());
        bo.setSlug(SlugUtils.from(rq.getTitle()));

        if (rq.getImage() != null) {
            FileBO file = fileService.uploadAndReturnFile(rq.getImage());
            bo.setImageFileId(file.getId());
        }

        return ServiceMapper.toCard(serviceRepo.save(bo));
    }

    @Override
    public ServiceCardRs updateService(ServiceRq rq) throws Exception {

        ServiceBO bo = serviceRepo.findById(rq.getId())
                .orElseThrow(() -> new RuntimeException("Service not found"));

        bo.setTitle(rq.getTitle());
        bo.setDescription(rq.getDescription());
        bo.setStartingPrice(rq.getStartingPrice());
        bo.setSlug(SlugUtils.from(rq.getTitle()));

        if (rq.getImage() != null) {
            FileBO file = fileService.uploadAndReturnFile(rq.getImage());
            bo.setImageFileId(file.getId());
        }

        return ServiceMapper.toCard(serviceRepo.save(bo));
    }

    @Override
    public void deleteService(Long id) {
        serviceRepo.deleteById(id);
    }


}

