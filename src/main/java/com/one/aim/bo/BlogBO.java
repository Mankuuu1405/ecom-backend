package com.one.aim.bo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class BlogBO {
    @Id
    @GeneratedValue
    private Long id;
    private String title;
    private String content;
    @Column(nullable = false, unique = true, length = 150)
    private String slug;

    private Long imageFileId;
    private boolean active;
    private LocalDateTime createdAt;
}

