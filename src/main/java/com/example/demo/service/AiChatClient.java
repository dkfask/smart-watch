package com.example.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容 Chat Completions 客户端（DeepSeek / GLM / DashScope 兼容模式等）。
 * 非流式与 SSE 流式共用同一套配置，密钥通过环境变量 AI_API_KEY 注入。
 */
@Service
public class AiChatClient {

    private static final Logger log = LoggerFactory.getLogger(AiChatClient.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final int timeoutSeconds;

    @Autowired
    public AiChatClient(Environment env, ObjectMapper objectMapper) {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),
                objectMapper,
                env.getProperty("app.ai.base-url", "https://api.deepseek.com"),
                env.getProperty("app.ai.api-key", ""),
                env.getProperty("app.ai.model", "deepseek-chat"),
                parseIntSafely(env.getProperty("app.ai.timeout-seconds", "120")));
    }

    /** 测试构造器 */
    AiChatClient(HttpClient httpClient, ObjectMapper objectMapper,
                 String baseUrl, String apiKey, String model, int timeoutSeconds) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        this.timeoutSeconds = timeoutSeconds;
    }

    public boolean isConfigured() {
        return !apiKey.isEmpty();
    }

    public String getModel() {
        return model;
    }

    /**
     * 一次对话的最终结果：累积文本 + 完整工具调用列表
     */
    public static class ChatResult {
        public String content;
        public String finishReason;
        public final List<ToolCall> toolCalls = new ArrayList<>();
    }

    public static class ToolCall {
        public String id;
        public String name;
        public String arguments;

        public ToolCall(String id, String name, String arguments) {
            this.id = id;
            this.name = name;
            this.arguments = arguments;
        }
    }

    /** 流式模式下每收到一段增量文本的回调 */
    public interface ContentDeltaListener {
        void onDelta(String text);
    }

    /**
     * 非流式对话
     * @param messages OpenAI 消息数组
     * @param tools 工具定义，可为 null
     */
    public ChatResult chat(List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
        ObjectNode body = buildRequestBody(messages, tools, false);
        JsonNode response = postForJson(body);
        ChatResult result = new ChatResult();
        result.finishReason = response.path("choices").path(0).path("finish_reason").asText(null);
        JsonNode message = response.path("choices").path(0).path("message");
        if (message.hasNonNull("content")) {
            result.content = message.get("content").asText();
        }
        appendToolCalls(result, message.path("tool_calls"), null);
        return result;
    }

    /**
     * 流式对话：SSE 逐段回调增量文本，方法返回时给出完整累积结果
     */
    public ChatResult chatStream(List<Map<String, Object>> messages,
                                 List<Map<String, Object>> tools,
                                 ContentDeltaListener listener) {
        ObjectNode body = buildRequestBody(messages, tools, true);
        HttpRequest request = newRequestBuilder()
                .uri(URI.create(baseUrl + "/chat/completions"))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();
        ChatResult result = new ChatResult();
        Map<Integer, ToolCall> toolCallBuffer = new LinkedHashMap<>();
        try {
            HttpResponse<java.io.InputStream> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                throw new AiChatException(upstreamError(response.statusCode(), new String(response.body().readAllBytes(), StandardCharsets.UTF_8)));
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    accumulateStreamLine(line, result, toolCallBuffer, listener);
                    if ("stop".equals(result.finishReason) || "tool_calls".equals(result.finishReason)
                            || "length".equals(result.finishReason)) {
                        break;
                    }
                }
            }
        } catch (IOException e) {
            throw new AiChatException("连接 AI 服务失败：" + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiChatException("AI 请求被中断", e);
        }
        result.toolCalls.addAll(toolCallBuffer.values());
        return result;
    }

    /**
     * 解析一行 SSE 数据并累积到 result；toolCallBuffer 按 index 聚合分段下发的 tool_calls。
     * 包私有以便单元测试。
     */
    void accumulateStreamLine(String line, ChatResult result,
                              Map<Integer, ToolCall> toolCallBuffer,
                              ContentDeltaListener listener) {
        if (line == null || line.isBlank() || line.startsWith(":")) {
            return;
        }
        if (!line.startsWith("data:")) {
            return;
        }
        String payload = line.substring(5).trim();
        if ("[DONE]".equals(payload)) {
            if (result.finishReason == null) {
                result.finishReason = "stop";
            }
            return;
        }
        JsonNode chunk;
        try {
            chunk = objectMapper.readTree(payload);
        } catch (IOException e) {
            log.debug("忽略无法解析的 AI 流式分片: {}", payload);
            return;
        }
        JsonNode choice = chunk.path("choices").path(0);
        String finish = choice.path("finish_reason").asText(null);
        if (finish != null) {
            result.finishReason = finish;
        }
        JsonNode delta = choice.path("delta");
        if (delta.hasNonNull("content") && !delta.get("content").asText().isEmpty()) {
            String text = delta.get("content").asText();
            result.content = (result.content == null ? "" : result.content) + text;
            if (listener != null) {
                listener.onDelta(text);
            }
        }
        appendToolCalls(result, delta.path("tool_calls"), toolCallBuffer);
    }

    private void appendToolCalls(ChatResult result, JsonNode toolCallsNode, Map<Integer, ToolCall> buffer) {
        if (!toolCallsNode.isArray()) {
            return;
        }
        for (JsonNode tc : toolCallsNode) {
            int index = tc.has("index") ? tc.get("index").asInt() : result.toolCalls.size() + bufferCount(buffer);
            String id = tc.path("id").asText(null);
            String name = tc.path("function").path("name").asText(null);
            String arguments = tc.path("function").path("arguments").asText("");
            ToolCall existing = buffer != null ? buffer.get(index) : null;
            if (existing == null) {
                existing = new ToolCall(id, name, arguments);
                if (buffer != null) {
                    buffer.put(index, existing);
                } else {
                    result.toolCalls.add(existing);
                }
            } else {
                if (id != null) {
                    existing.id = id;
                }
                if (name != null) {
                    existing.name = name;
                }
                existing.arguments = existing.arguments + arguments;
            }
        }
    }

    private static int bufferCount(Map<Integer, ToolCall> buffer) {
        return buffer == null ? 0 : buffer.size();
    }

    private JsonNode postForJson(ObjectNode body) {
        HttpRequest request = newRequestBuilder()
                .uri(URI.create(baseUrl + "/chat/completions"))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new AiChatException(upstreamError(response.statusCode(), response.body()));
            }
            return objectMapper.readTree(response.body());
        } catch (IOException e) {
            throw new AiChatException("连接 AI 服务失败：" + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiChatException("AI 请求被中断", e);
        }
    }

    private ObjectNode buildRequestBody(List<Map<String, Object>> messages,
                                        List<Map<String, Object>> tools, boolean stream) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("stream", stream);
        ArrayNode msgNode = body.putArray("messages");
        for (Map<String, Object> message : messages) {
            ObjectNode node = msgNode.addObject();
            node.put("role", String.valueOf(message.get("role")));
            Object content = message.get("content");
            node.put("content", content == null ? "" : String.valueOf(content));
            Object toolCalls = message.get("tool_calls");
            if (toolCalls != null) {
                node.set("tool_calls", objectMapper.valueToTree(toolCalls));
            }
            Object toolCallId = message.get("tool_call_id");
            if (toolCallId != null) {
                node.put("tool_call_id", String.valueOf(toolCallId));
            }
        }
        if (tools != null && !tools.isEmpty()) {
            ArrayNode toolsNode = body.putArray("tools");
            for (Map<String, Object> tool : tools) {
                toolsNode.add(objectMapper.valueToTree(tool));
            }
        }
        return body;
    }

    private HttpRequest.Builder newRequestBuilder() {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .header("Content-Type", "application/json");
        if (!apiKey.isEmpty()) {
            builder.header("Authorization", "Bearer " + apiKey);
        }
        return builder;
    }

    private String upstreamError(int status, String body) {
        String detail = body == null ? "" : body.replaceAll("\\s+", " ").trim();
        if (detail.length() > 200) {
            detail = detail.substring(0, 200) + "...";
        }
        if (status == 401) {
            return "AI 服务鉴权失败（401），请检查 AI_API_KEY";
        }
        if (status == 429) {
            return "AI 服务限流或余额不足（429），请稍后重试";
        }
        return "AI 服务返回异常（HTTP " + status + "）" + (detail.isEmpty() ? "" : "：" + detail);
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "https://api.deepseek.com";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url.trim();
    }

    private static int parseIntSafely(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 120;
        }
    }

    /** AI 调用异常，消息已转为面向用户的中文提示 */
    public static class AiChatException extends RuntimeException {
        public AiChatException(String message) {
            super(message);
        }

        public AiChatException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
