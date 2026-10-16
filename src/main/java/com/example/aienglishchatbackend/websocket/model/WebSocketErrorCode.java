package com.example.aienglishchatbackend.websocket.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WebSocketErrorCode {
    MALFORMED_JSON("MALFORMED_JSON"),
    INVALID_MESSAGE("INVALID_MESSAGE"),
    UNKNOWN_TYPE("UNKNOWN_TYPE"),
    UNAUTHORIZED("UNAUTHORIZED"),
    FORBIDDEN("FORBIDDEN"),
    INVALID_TOKEN("INVALID_TOKEN");

    private final String code;
}
