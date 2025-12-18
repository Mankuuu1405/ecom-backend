package com.one.aim.service;

import com.one.aim.rq.BlogCreateRq;
import org.springframework.web.multipart.MultipartFile;

public interface BlogService {

    void createBlog(BlogCreateRq rq, MultipartFile image) throws Exception;
}
