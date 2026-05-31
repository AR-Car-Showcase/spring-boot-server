package com.arcarshowcaseserver.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDTO {
    private Long id;
    private String username;
    private String email;
    private String authProvider;
    private Boolean profileCompleted;
    private String displayName;
    private String phoneNumber;
    private String bio;
    private String profilePic;
    private Set<String> favBrands;
    private Set<String> preferredBodyTypes;
    private Set<String> preferredFuelTypes;
    private Set<String> preferredTransmissions;
    private String drivingCondition;
    private Double maxBudget;
    private long savedCount;
    private long customizedCount;
}
