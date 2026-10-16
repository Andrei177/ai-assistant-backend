package com.example.aienglishchatbackend.llm.client;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmMessage {
    private Role role;      // SYSTEM | USER | ASSISTANT
    private String content;

    public enum Role {
        SYSTEM,
        USER,
        ASSISTANT;

        public String toApiValue() {
            return name().toLowerCase();
        }

        public static Role fromApiValue(String value) {
            if (value == null) {
                return null;
            }
            return Role.valueOf(value.toUpperCase());
        }
    }
}