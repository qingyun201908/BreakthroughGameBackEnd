package BreakthroughGame.backEnd.UserModule.UserRestful.request;

import jakarta.validation.constraints.NotBlank;


public class LoginRequest {
    @NotBlank
    private String email; // 简化：用用户名登录


    @NotBlank
    private String password;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}