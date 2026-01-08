package com.one.aim.rq;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewUpdateRq {

    @NotNull
    private Long id;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;

    @NotBlank
    private String comment;
}


