package com.example.aienglishchatbackend.llm.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "llm.gigachat")
public class GigaChatProperties {

    private String authUrl = "https://ngw.devices.sberbank.ru:9443/api/v2/oauth";
    private String apiUrl = "https://api.giga.chat/v1";
    private String clientId = "";
    private String authKey = "";
    private String model = "GigaChat";
    private Integer maxTokens;
    private String trustStorePath;
    private String trustStorePassword;
}
