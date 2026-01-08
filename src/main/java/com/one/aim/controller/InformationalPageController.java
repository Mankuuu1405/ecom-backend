package com.one.aim.controller;

import com.one.aim.constants.PageType;
import com.one.aim.rq.InformationalPageRq;
import com.one.aim.rs.InformationalPageRs;
import com.one.aim.rs.PagedRs;
import com.one.aim.service.InformationalPageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/pages")
@RequiredArgsConstructor
public class InformationalPageController {

    private final InformationalPageService pageService;

    @PostMapping
    public ResponseEntity<InformationalPageRs> createOrUpdatePage(
            @Valid @RequestBody InformationalPageRq requestDTO) {
        InformationalPageRs response = pageService.createOrUpdatePage(requestDTO);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InformationalPageRs> getPageById(@PathVariable Long id) {
        InformationalPageRs response = pageService.getPageById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/type/{pageType}")
    public ResponseEntity<InformationalPageRs> getPageByType(@PathVariable PageType pageType) {
        InformationalPageRs response = pageService.getPageByType(pageType);
        return ResponseEntity.ok(response);
    }



    @GetMapping
    public ResponseEntity<PagedRs<InformationalPageRs>> getAllPages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("ASC") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PagedRs<InformationalPageRs> response = pageService.getAllPages(pageable);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePage(@PathVariable Long id) {
        pageService.deletePage(id);
        return ResponseEntity.noContent().build();
    }
}
