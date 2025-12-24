package com.one.aim.rs;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CombinedUserRs implements Serializable {

    private Long docId;
    private String fullName;
    private String email;
    private String phoneNo;
    private String role;           // USER / SELLER
    private Boolean active;
    private Boolean emailVerified;
    private String imageUrl;
    private LocalDateTime createdAt;
}