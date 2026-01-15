package com.one.aim.rq;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRq {

    private static final String STRONG_PASSWORD_REGEX =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";


    private String fullName;
    private String phoneNo;

    // Optional for password change
    private String oldPassword;
    private String newPassword;
    private String confirmPassword;

    private String email;
    private MultipartFile image;

    private Boolean removeImage = false;

    // ===========================================================
    // Helper: Check if user is trying to update password
    // ===========================================================
    public boolean hasPasswordUpdate() {
        return (oldPassword != null && !oldPassword.isBlank()) ||
                (newPassword != null && !newPassword.isBlank()) ||
                (confirmPassword != null && !confirmPassword.isBlank());
    }

    // ===========================================================
    // Helper: Validate password update fields
    // ===========================================================
    public boolean isPasswordDataValid() {

        // All 3 must be present
        if (oldPassword == null || oldPassword.isBlank() ||
                newPassword == null || newPassword.isBlank() ||
                confirmPassword == null || confirmPassword.isBlank()) {
            return false;
        }

        // Strong password validation
        if (!newPassword.matches(STRONG_PASSWORD_REGEX)) {
            return false;
        }

        // New & confirm must match
        if (!newPassword.equals(confirmPassword)) {
            return false;
        }

        return true;
    }


    // ===========================================================
    // Helper: Provide specific password validation error message
    // ===========================================================
    public String passwordErrorMessage() {

        if (oldPassword == null || oldPassword.isBlank() ||
                newPassword == null || newPassword.isBlank() ||
                confirmPassword == null || confirmPassword.isBlank()) {
            return "All password fields (old, new, confirm) are required.";
        }

        if (!newPassword.matches(STRONG_PASSWORD_REGEX)) {
            return "Password must be at least 8 characters and include uppercase, lowercase, number, and special character.";
        }

        if (!newPassword.equals(confirmPassword)) {
            return "New password and confirm password do not match.";
        }

        return "Invalid password details.";
    }

}
