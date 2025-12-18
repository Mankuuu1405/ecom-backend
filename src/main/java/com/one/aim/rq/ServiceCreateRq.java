package com.one.aim.rq;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServiceCreateRq {
    @NotBlank
    private String title;

    private String description;

    @NotNull
    private Double startingPrice;

    private boolean active = true;
}

