package com.one.utils;

import jakarta.persistence.Column;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UrlUtils {

    @Value("${app.backend.base-url}")
    private String backendBaseUrl;

    public String publicFile(Long fileId) {
        return backendBaseUrl + "/api/files/public/" + fileId + "/view";
    }

    public String defaultImage() {
        return backendBaseUrl + "/assets/placeholder.png";
    }

    public String defaultBanner() {
        return backendBaseUrl + "/assets/default-banner.jpg";
    }

    public String privateFile(Long fileId) {
        return backendBaseUrl + "/api/files/private/" + fileId + "/view";
    }

}
