package com.arcarshowcaseserver.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserAccountDTO {
    @Size(min = 3, max = 20)
    @Pattern(regexp = "^[a-zA-Z0-9._]+$", message = "Username can only contain letters, numbers, dots, and underscores.")
    private String username;

    @Size(max = 60)
    private String displayName;

    @Size(max = 500)
    private String bio;

    @Size(max = 20)
    @Pattern(regexp = "^$|^\\+?[0-9\\s-]{7,20}$", message = "Phone number is invalid.")
    private String phoneNumber;

    private String profilePic;
}
