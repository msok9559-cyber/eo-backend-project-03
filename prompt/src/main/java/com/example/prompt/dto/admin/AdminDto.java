package com.example.prompt.dto.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class AdminDto {
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {

        @NotBlank
        private String adminId;

        @NotBlank
        private String password;
    }

    @Getter
    @AllArgsConstructor
    public static class LoginResponse {
        private String accessToken;
        private String adminId;
        private String name;
    }

    @Getter
    @AllArgsConstructor
    public static class MeResponse {
        private String adminId;
        private String name;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChangePlanRequest {
        private String planName;
    }
}
