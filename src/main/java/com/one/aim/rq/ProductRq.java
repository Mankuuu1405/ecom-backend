package com.one.aim.rq;

import com.one.vm.core.BaseVM;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ProductRq extends BaseVM {

    private String docId;

    @NotBlank
    private String name;

    @NotBlank
    private String description;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private Double price;

    @NotNull
    @Min(0)
    private Integer stock;

    private String brand;

    private boolean bestSeller;
    private boolean newArrival;
    private boolean onSale;

    private Long categoryId;
    private String customCategoryName;

    private String specificationsJson;

    private Boolean active;

    // =========================
    // IMAGES
    // =========================
    private List<MultipartFile> images;

    // 0-based index of thumbnail image in images list
    private Integer thumbnailIndex;

    // Optional (only if seller selects these)
    private String color;
    private String size;
}

