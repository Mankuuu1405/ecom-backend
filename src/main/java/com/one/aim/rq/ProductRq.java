package com.one.aim.rq;

import com.one.vm.core.BaseVM;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ProductRq extends BaseVM {

    private String docId;

    @NotBlank(message = "Product name is required")
    private String name;

    @NotBlank(message = "Product description is required")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private Double price;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 1, message = "Stock must be at least 1")
    private Integer stock;

    private String brand;

    //  REMOVED: Sellers cannot set these
    // private boolean bestSeller;
    // private boolean newArrival;

    //  Only onSale is allowed for sellers
    private Boolean onSale;
    private Double offerPrice;        // discounted selling price (₹)
    private Integer discountPercent;  // discount % (auto OR manual)

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

    // Optional specifications (can be part of specificationsJson)
    private String color;
    private String size;
}