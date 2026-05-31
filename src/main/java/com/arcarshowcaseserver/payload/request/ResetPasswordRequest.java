package com.arcarshowcaseserver.payload.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String code;

    @NotBlank
    @Size(min = 8, message = "Password must be at least 8 characters long.")
    private String newPassword;
}
