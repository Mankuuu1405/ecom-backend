package com.one.aim.service.impl;

import com.one.aim.bo.InformationalPageBO;
import com.one.aim.constants.PageType;
import com.one.aim.mapper.InformationalPageMapper;
import com.one.aim.repo.InformationalPageRepository;
import com.one.aim.rq.InformationalPageRq;
import com.one.aim.rs.InformationalPageRs;
import com.one.aim.rs.PagedRs;
import com.one.aim.service.InformationalPageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InformationalPageServiceImpl implements InformationalPageService {

    private final InformationalPageRepository pageRepository;
    private final InformationalPageMapper pageMapper;

    @Override
    @Transactional
    public InformationalPageRs createOrUpdatePage(InformationalPageRq requestDTO) {
        Optional<InformationalPageBO> existingPage = pageRepository.findByPageType(requestDTO.getPageType());

        InformationalPageBO page;
        if (existingPage.isPresent()) {
            page = existingPage.get();
            pageMapper.updateEntityFromDTO(requestDTO, page);
            log.info("Updating existing page: {}", requestDTO.getPageType());
        } else {
            page = pageMapper.toEntity(requestDTO);
            log.info("Creating new page: {}", requestDTO.getPageType());
        }

        InformationalPageBO savedPage = pageRepository.save(page);
        return pageMapper.toResponseDTO(savedPage);
    }

    @Override
    @Transactional(readOnly = true)
    public InformationalPageRs getPageById(Long id) {
        InformationalPageBO page = pageRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("Page not found with ID: " + id));
        return pageMapper.toResponseDTO(page);
    }

    @Override
    @Transactional(readOnly = true)
    public InformationalPageRs getPageByType(PageType pageType) {
        InformationalPageBO page = pageRepository.findByPageType(pageType)
                .orElseThrow(() -> new UsernameNotFoundException("Page not found for type: " + pageType));
        return pageMapper.toResponseDTO(page);
    }


    @Override
    @Transactional(readOnly = true)
    public PagedRs<InformationalPageRs> getAllPages(Pageable pageable) {
        Page<InformationalPageBO> pagePage = pageRepository.findAll(pageable);
        return buildPagedResponse(pagePage);
    }

    @Override
    @Transactional
    public void deletePage(Long id) {
        if (!pageRepository.existsById(id)) {
            throw new UsernameNotFoundException("Page not found with ID: " + id);
        }
        pageRepository.deleteById(id);
        log.info("Page deleted with ID: {}", id);
    }

    private PagedRs<InformationalPageRs> buildPagedResponse(Page<InformationalPageBO> page) {
        List<InformationalPageRs> content = page.getContent().stream()
                .map(pageMapper::toResponseDTO)
                .collect(Collectors.toList());

        return PagedRs.<InformationalPageRs>builder()
                .content(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .build();
    }
}
