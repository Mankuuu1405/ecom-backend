package com.one.aim.service.impl;

import com.one.aim.repo.ProductRepo;
import com.one.aim.service.ProductAutocompleteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductAutocompleteServiceImpl
        implements ProductAutocompleteService {

    private final ProductRepo productRepository;

    @Override
    public List<String> autocomplete(String q) {

        if (q == null || q.length() < 2) {
            return List.of();
        }

        return productRepository.autocompleteNames(q.toLowerCase());
    }
}

