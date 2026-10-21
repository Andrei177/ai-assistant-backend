package com.example.aienglishchatbackend.llm.client.gigachat;

import com.example.aienglishchatbackend.llm.client.*;
import com.example.aienglishchatbackend.llm.config.GigaChatProperties;
import com.example.aienglishchatbackend.llm.config.LlmProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class GigaChatLlmClient implements LlmClient {
    private static final Logger log = LoggerFactory.getLogger(GigaChatLlmClient.class);
    private final GigaChatProperties properties;
    private final LlmProperties llmProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private String accessToken;

    public GigaChatLlmClient(GigaChatProperties properties, LlmProperties llmProperties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.llmProperties = llmProperties;
        this.objectMapper = objectMapper;
        this.httpClient = createHttpClient(properties);
    }

    private static HttpClient createHttpClient(GigaChatProperties properties) {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10));

        if (properties.getTrustStorePath() != null && !properties.getTrustStorePath().isBlank()) {
            try {
                KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                char[] password = properties.getTrustStorePassword() != null
                        ? properties.getTrustStorePassword().toCharArray()
                        : null;
                try (FileInputStream fis = new FileInputStream(properties.getTrustStorePath())) {
                    keyStore.load(fis, password);
                }

                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(keyStore);

                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, tmf.getTrustManagers(), null);
                builder.sslContext(sslContext);
                log.info("Successfully configured custom trust store for GigaChat client");
            } catch (Exception e) {
                log.error("Failed to configure custom trust store for GigaChat client", e);
            }
        } else {
            log.warn("Custom trust store for GigaChat client is not configured, SSL handshake might fail.");
        }

        return builder.build();
    }

    private synchronized void refreshAccessToken() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getAuthUrl()))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Authorization", "Basic " + properties.getAuthKey())
                    .header("RqUID", UUID.randomUUID().toString())
                    .POST(HttpRequest.BodyPublishers.ofString("scope=GIGACHAT_API_PERS"))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("GigaChat auth failed: " + response.statusCode() + " " + response.body());
            }

            JsonNode json = objectMapper.readTree(response.body());
            this.accessToken = json.get("access_token").asText();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to refresh GigaChat access token", e);
        }
    }

    private synchronized String getAccessToken() {
        if (accessToken == null) {
            refreshAccessToken();
        }
        return accessToken;
    }

    @Override
    public LlmResponse chat(List<LlmMessage> messages, LlmOptions options) {
        String body = buildRequestBody(messages, options, false);
        return sendRequest(body, false);
    }

    @Override
    public void chatStream(List<LlmMessage> messages, LlmOptions options, 
                           Consumer<String> onToken, 
                           Consumer<LlmResponse> onComplete, 
                           Consumer<Throwable> onError) {
        String body = buildRequestBody(messages, options, true);
        try {
            chatStreamInternal(body, onToken, onComplete, onError);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("401")) {
                synchronized (this) {
                    this.accessToken = null; // force refresh
                }
                chatStreamInternal(body, onToken, onComplete, onError);
            } else {
                onError.accept(e);
            }
        }
    }

    private void chatStreamInternal(String body, 
                                    Consumer<String> onToken, 
                                    Consumer<LlmResponse> onComplete, 
                                    Consumer<Throwable> onError) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getApiUrl() + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/event-stream")
                    .header("Authorization", "Bearer " + getAccessToken())
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(60))
                    .build();

            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() == 401) {
                throw new IllegalStateException("401");
            }
            if (response.statusCode() >= 400) {
                try (InputStream is = response.body()) {
                    throw new IllegalStateException("GigaChat error " + response.statusCode() + ": " + new String(is.readAllBytes(), StandardCharsets.UTF_8));
                }
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                StringBuilder fullContent = new StringBuilder();
                String model = null;
                
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6);
                        if ("[DONE]".equals(data)) {
                            break;
                        }
                        JsonNode json = objectMapper.readTree(data);
                        JsonNode choice = json.get("choices").get(0);
                        JsonNode delta = choice.get("delta");
                        if (delta != null && delta.has("content")) {
                            String content = delta.get("content").asText();
                            fullContent.append(content);
                            onToken.accept(content);
                        }
                        if (model == null && json.has("model")) {
                            model = json.get("model").asText();
                        }
                    }
                }
                onComplete.accept(LlmResponse.builder()
                        .content(fullContent.toString())
                        .model(model)
                        .finishReason("stop")
                        .build());
            }
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("401")) {
                throw new IllegalStateException("401", e);
            }
            onError.accept(e);
        }
    }

    private LlmResponse sendRequest(String body, boolean stream) {
        try {
            return sendRequestInternal(body, stream);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("401")) {
                synchronized (this) {
                    this.accessToken = null; // force refresh
                }
                return sendRequestInternal(body, stream);
            }
            throw new IllegalStateException("GigaChat chat failed: " + e.getMessage(), e);
        }
    }

    private LlmResponse sendRequestInternal(String body, boolean stream) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getApiUrl() + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + getAccessToken())
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(60))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 401) {
                throw new IllegalStateException("401");
            }
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("GigaChat error " + response.statusCode() + ": " + response.body());
            }
            return parseCompletionResponse(response.body());
        } catch (Exception e) {
            throw new IllegalStateException("GigaChat request failed: " + e.getMessage(), e);
        }
    }

    private LlmResponse parseCompletionResponse(String body) {
        try {
            JsonNode json = objectMapper.readTree(body);
            JsonNode choice = json.get("choices").get(0);
            String content = choice.get("message").get("content").asText();
            
            LlmResponse.Usage usage = null;
            if (json.has("usage")) {
                JsonNode u = json.get("usage");
                usage = LlmResponse.Usage.builder()
                        .promptTokens(u.get("prompt_tokens").asInt())
                        .completionTokens(u.get("completion_tokens").asInt())
                        .totalTokens(u.get("total_tokens").asInt())
                        .build();
            }
            
            return LlmResponse.builder()
                    .content(content)
                    .model(json.get("model").asText())
                    .finishReason(choice.get("finish_reason").asText())
                    .usage(usage)
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse GigaChat response", e);
        }
    }

    private String buildRequestBody(List<LlmMessage> messages, LlmOptions options, boolean stream) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", properties.getModel());
        ArrayNode messagesArray = root.putArray("messages");
        for (LlmMessage msg : messages) {
            ObjectNode msgNode = messagesArray.addObject();
            msgNode.put("role", msg.getRole().toApiValue());
            msgNode.put("content", msg.getContent());
        }
        root.put("stream", stream);
        if (options != null && options.getMaxTokens() != null) {
            root.put("max_tokens", options.getMaxTokens());
        }
        return root.toString();
    }
}
