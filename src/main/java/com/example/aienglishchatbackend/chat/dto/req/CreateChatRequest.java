package com.example.aienglishchatbackend.chat.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Setter
@Getter
public class CreateChatRequest {
    @NotBlank
    private String chatName;
}
