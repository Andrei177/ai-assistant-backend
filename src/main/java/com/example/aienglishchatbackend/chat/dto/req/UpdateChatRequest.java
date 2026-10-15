package com.example.aienglishchatbackend.chat.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateChatRequest {
    @NotBlank
    private String chatName;
}
