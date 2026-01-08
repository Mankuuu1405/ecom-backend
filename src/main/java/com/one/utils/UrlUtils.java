package com.one.utils;

import jakarta.persistence.Column;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UrlUtils {

    public String publicFile(Long fileId) {
        return "/aimdev/api/files/public/" + fileId + "/view";
    }

    public String privateFile(Long fileId) {
        return "/aimdev/api/files/private/" + fileId + "/view";
    }

    public String defaultImage() {
        return "/assets/placeholder.png";
    }

    public String defaultBanner() {
        return "/assets/default-banner.jpg";
    }
}

