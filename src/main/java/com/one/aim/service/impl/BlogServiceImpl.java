package com.one.aim.service.impl;

import com.one.aim.bo.BlogBO;
import com.one.aim.repo.BlogRepo;
import com.one.aim.rq.BlogCreateRq;
import com.one.aim.rs.data.FileDataRs;
import com.one.aim.service.BlogService;
import com.one.aim.service.FileService;
import com.one.vm.core.BaseRs;
import com.one.vm.utils.SlugUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class BlogServiceImpl implements BlogService {

    private final BlogRepo blogRepo;
    private final FileService fileService;

    @Override
    public void createBlog(BlogCreateRq rq, MultipartFile image) throws Exception {

        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Blog image is required");
        }

        FileDataRs fileData = (FileDataRs) fileService.uploadFile(image).getData();
        Long imageFileId = fileData.getFileId();

        BlogBO blog = new BlogBO();
        blog.setTitle(rq.getTitle());
        blog.setContent(rq.getContent());
        blog.setActive(rq.isActive());
        blog.setSlug(SlugUtils.from(rq.getTitle()));
        blog.setImageFileId(imageFileId);

        blogRepo.save(blog);
    }
}

