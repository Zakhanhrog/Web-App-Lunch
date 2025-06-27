package com.example.lunchapp.model.dto;

import lombok.Getter;
import lombok.Setter;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Getter
@Setter
public class ChangePasswordDto {

    @NotBlank(message = "New password cannot be blank")
    @Size(min = 6, message = "New password must be at least 6 characters")
    private String newPassword;

    @NotBlank(message = "Confirm new password cannot be blank")
    private String confirmNewPassword;
}