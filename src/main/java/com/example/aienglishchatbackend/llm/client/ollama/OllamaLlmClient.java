package com.example.aienglishchatbackend.llm.client.ollama;


import com.example.aienglishchatbackend.llm.client.LlmClient;
import com.example.aienglishchatbackend.llm.client.LlmMessage;
import com.example.aienglishchatbackend.llm.client.LlmOptions;
import com.example.aienglishchatbackend.llm.client.LlmResponse;
import com.example.aienglishchatbackend.llm.config.LlmProperties;
import com.example.aienglishchatbackend.llm.config.OllamaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

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
import java.util.function.Consumer;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public class OllamaLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaLlmClient.class);

    private final OllamaProperties ollamaProperties;
    private final LlmProperties llmProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OllamaLlmClient(
            OllamaProperties ollamaProperties,
            LlmProperties llmProperties,
            ObjectMapper objectMapper
    ) {
        this.ollamaProperties = ollamaProperties;
        this.llmProperties = llmProperties;
        this.objectMapper = objectMapper;
        this.httpClient = createHttpClient(ollamaProperties);
    }

    private static HttpClient createHttpClient(OllamaProperties properties) {
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
            } catch (Exception e) {
                log.error("Failed to configure custom trust store for Ollama client", e);
            }
        }

        return builder.build();
    }

    @Override
    public LlmResponse chat(List<LlmMessage> messages, LlmOptions options) {
        try {
            String body = buildRequestBody(messages, options, false);
            String url = ollamaProperties.getBaseUrl() + "/v1/chat/completions";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(120))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("Ollama error " + response.statusCode() + ": " + response.body());
            }
            return parseCompletionResponse(response.body());
        } catch (Exception e) {
            throw new IllegalStateException("Ollama chat failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void chatStream(
            List<LlmMessage> messages,
            LlmOptions options,
            Consumer<String> onToken,
            Consumer<LlmResponse> onComplete,
            Consumer<Throwable> onError
    ) {
        try {
            String body = buildRequestBody(messages, options, true);
            String url = ollamaProperties.getBaseUrl() + "/v1/chat/completions";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/event-stream")
                    .timeout(Duration.ofSeconds(180))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() >= 400) {
                String err = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                onError.accept(new IllegalStateException("Ollama stream error " + response.statusCode() + ": " + err));
                return;
            }

            StringBuilder fullContent = new StringBuilder();
            String model = null;
            String finishReason = null;

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body(), StandardCharsets.UTF_8)
            )) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) {
                        continue;
                    }
                    if (line.startsWith("data:")) {
                        line = line.substring(5).trim();
                    }
                    if ("[DONE]".equals(line)) {
                        break;
                    }
                    JsonNode chunk = objectMapper.readTree(line);
                    if (chunk.has("model")) {
                        model = chunk.get("model").asText();
                    }
                    JsonNode choices = chunk.get("choices");
                    if (choices != null && choices.isArray() && !choices.isEmpty()) {
                        JsonNode choice = choices.get(0);
                        if (choice.has("finish_reason") && !choice.get("finish_reason").isNull()) {
                            finishReason = choice.get("finish_reason").asText();
                        }
                        JsonNode delta = choice.get("delta");
                        if (delta != null && delta.has("content") && !delta.get("content").isNull()) {
                            String token = delta.get("content").asText();
                            fullContent.append(token);
                            onToken.accept(token);
                        }
                    }
                }
            }

            onComplete.accept(LlmResponse.builder()
                    .content(fullContent.toString())
                    .model(model)
                    .finishReason(finishReason)
                    .build());
        } catch (Exception e) {
            log.error("Ollama stream failed", e);
            onError.accept(e);
        }
    }

    private String buildRequestBody(List<LlmMessage> messages, LlmOptions options, boolean stream) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();

        String model = options.getModel() != null ? options.getModel() : ollamaProperties.getModel();
        root.put("model", model);
        root.put("stream", stream);

        Double temperature = options.getTemperature() != null
                ? options.getTemperature()
                : llmProperties.getDefaults().getTemperature();
        if (temperature != null) {
            root.put("temperature", temperature);
        }

        Integer maxTokens = options.getMaxTokens();
        if (maxTokens == null) {
            maxTokens = ollamaProperties.getMaxTokens();
        } else if (ollamaProperties.getMaxTokens() != null) {
            maxTokens = Math.min(maxTokens, ollamaProperties.getMaxTokens());
        }
        if (maxTokens == null) {
            maxTokens = llmProperties.getDefaults().getMaxTokens();
        }
        if (maxTokens != null && maxTokens > 0) {
            root.put("max_tokens", maxTokens);
        }

        if (options.getTopP() != null) {
            root.put("top_p", options.getTopP());
        }
        if (options.getSeed() != null) {
            root.put("seed", options.getSeed());
        }

        ArrayNode msgs = root.putArray("messages");
        for (LlmMessage m : messages) {
            ObjectNode msg = msgs.addObject();
            msg.put("role", m.getRole().toApiValue());
            msg.put("content", m.getContent());
        }
        return objectMapper.writeValueAsString(root);
    }

    private LlmResponse parseCompletionResponse(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        String model = root.has("model") ? root.get("model").asText() : null;
        String content = "";
        String finishReason = null;

        JsonNode choices = root.get("choices");
        if (choices != null && choices.isArray() && !choices.isEmpty()) {
            JsonNode choice = choices.get(0);
            if (choice.has("finish_reason") && !choice.get("finish_reason").isNull()) {
                finishReason = choice.get("finish_reason").asText();
            }
            JsonNode message = choice.get("message");
            if (message != null && message.has("content")) {
                content = message.get("content").asText();
            }
        }

        LlmResponse.Usage usage = null;
        JsonNode usageNode = root.get("usage");
        if (usageNode != null && !usageNode.isNull()) {
            usage = LlmResponse.Usage.builder()
                    .promptTokens(usageNode.path("prompt_tokens").isMissingNode() ? null : usageNode.get("prompt_tokens").asInt())
                    .completionTokens(usageNode.path("completion_tokens").isMissingNode() ? null : usageNode.get("completion_tokens").asInt())
                    .totalTokens(usageNode.path("total_tokens").isMissingNode() ? null : usageNode.get("total_tokens").asInt())
                    .build();
        }

        return LlmResponse.builder()
                .content(content)
                .model(model)
                .finishReason(finishReason)
                .usage(usage)
                .build();
    }
}