package com.example.aienglishchatbackend.auth.dto.req;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AuthRequest {
    @Email(message = "Email должен иметь соответствующий формат")
    private String userEmail;

    @NotBlank(message = "Пароль не может быть пустым")
    @Size(min = 7, message = "Пароль минимум 7 символов")
    private String password;
}
