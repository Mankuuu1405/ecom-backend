package com.one.aim.service.impl;

import com.one.aim.bo.FileBO;
import com.one.aim.bo.ServiceBO;
import com.one.aim.mapper.ServiceMapper;
import com.one.aim.repo.ServiceRepo;
import com.one.aim.rq.ServiceCreateRq;
import com.one.aim.rq.ServiceRq;
import com.one.aim.rs.ServiceCardRs;
import com.one.aim.rs.data.FileDataRs;
import com.one.aim.service.FileService;
import com.one.aim.service.ServiceModuleService;
import com.one.vm.core.BaseRs;
import com.one.vm.utils.SlugUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ServiceModuleServiceImpl implements ServiceModuleService {

    private final ServiceRepo serviceRepo;
    private final FileService fileService;
    private final ServiceMapper serviceMapper;

    @Override
    public List<ServiceCardRs> getTopServices(int limit) {

        Pageable pageable = PageRequest.of(0, limit);

        return serviceRepo.findActiveServices(pageable)
                .stream()
                .map(serviceMapper::toCard)
                .toList();
    }

    @Override
    public ServiceCardRs getServiceDetails(String slug) {
        ServiceBO bo = serviceRepo.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Service not found"));
        return serviceMapper.toCard(bo);
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

        return serviceMapper.toCard(serviceRepo.save(bo));
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

        return serviceMapper.toCard(serviceRepo.save(bo));
    }

    @Override
    public void deleteService(Long id) {
        serviceRepo.deleteById(id);
    }

    @Override
    public void createService(ServiceCreateRq rq, MultipartFile image) throws Exception {

        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Service image is required");
        }

        BaseRs baseRs = fileService.uploadFile(image);
        FileDataRs fileData = (FileDataRs) baseRs.getData();

        String baseSlug = SlugUtils.from(rq.getTitle());
        String slug = baseSlug;
        int counter = 1;

        while (serviceRepo.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter++;
        }

        ServiceBO service = new ServiceBO();
        service.setTitle(rq.getTitle());
        service.setDescription(rq.getDescription());
        service.setStartingPrice(rq.getStartingPrice());
        service.setActive(rq.isActive());
        service.setImageFileId(fileData.getFileId());
        service.setSlug(slug);

        serviceRepo.save(service);
    }


}

