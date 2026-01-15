package com.one.aim.rs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddressRs {

    private String fullName;
    private String street;
    private String city;
    private String state;
    private String zip;
    private String country;
    private String phone;
}

