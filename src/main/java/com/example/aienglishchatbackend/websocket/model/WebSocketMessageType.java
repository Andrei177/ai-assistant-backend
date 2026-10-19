package com.example.aienglishchatbackend.websocket.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WebSocketMessageType {
    AUTH("AUTH"),
    AUTH_OK("AUTH_OK"),
    AUTH_FAILED("AUTH_FAILED"),
    SEND_MESSAGE("SEND_MESSAGE"),
    MESSAGE_ACCEPTED("MESSAGE_ACCEPTED"),
    MESSAGE_STREAM("MESSAGE_STREAM"),
    MESSAGE_STREAM_COMPLETED("MESSAGE_STREAM_COMPLETED"),
    ERROR("ERROR");

    private final String value;

    public static WebSocketMessageType fromString(String text) {
        for (WebSocketMessageType type : WebSocketMessageType.values()) {
            if (type.value.equalsIgnoreCase(text)) {
                return type;
            }
        }
        return null;
    }
}
