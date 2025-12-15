package com.one.aim.rq;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRq {

    private Long id;                    // null for create, required for update

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private Double startingPrice;

    // Uploaded image (optional for update)
    private MultipartFile image;
}

