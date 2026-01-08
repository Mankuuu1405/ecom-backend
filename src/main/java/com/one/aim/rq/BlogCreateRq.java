package com.one.aim.rq;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BlogCreateRq {
    private String title;
    private String content;
    private boolean active;
}

