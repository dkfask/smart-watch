package com.example.demo.controller.api;

import com.example.demo.model.dto.AiChatRequest;
import com.example.demo.service.AiChatClient;
import com.example.demo.service.AiChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AI 助手接口：状态查询、非流式回答、SSE 流式回答。
 * 会话历史由前端携带，服务端不持久化。
 */
@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    private static final Logger log = LoggerFactory.getLogger(AiChatController.class);

    /** 每用户对话频率（次/秒），默认 2 秒 1 次，保护模型配额 */
    private final double perUserRequestsPerSecond;

    private final AiChatService aiChatService;
    private final ObjectMapper objectMapper;
    private final ExecutorService aiExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final Map<String, RateLimiter> userLimiters = new ConcurrentHashMap<>();

    public AiChatController(AiChatService aiChatService, ObjectMapper objectMapper,
                            org.springframework.core.env.Environment env) {
        this.aiChatService = aiChatService;
        this.objectMapper = objectMapper;
        double rate = 0.5;
        try {
            rate = Double.parseDouble(env.getProperty("app.ai.user-requests-per-second", "0.5"));
        } catch (NumberFormatException ignored) {
        }
        this.perUserRequestsPerSecond = rate <= 0 ? 0.5 : rate;
    }

    /**
     * AI 助手配置状态，前端据此显示或隐藏入口
     */
    @GetMapping("/status")
    public ResponseEntity<?> status() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("enabled", aiChatService.isEnabled());
        data.put("configured", aiChatService.isConfigured());
        data.put("model", aiChatService.getModel());
        return ResponseEntity.ok(data);
    }

    /**
     * 非流式回答（降级通道）
     */
    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody AiChatRequest request) {
        acquireUserQuota();
        String content = aiChatService.chat(request.getMessages());
        return ResponseEntity.ok(Map.of("content", content));
    }

    /**
     * SSE 流式回答，事件：delta（增量文本）、tool（查询进度）、done（完成）、error（错误）
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody AiChatRequest request) {
        acquireUserQuota();
        SseEmitter emitter = new SseEmitter(180_000L);
        aiExecutor.execute(() -> {
            try {
                aiChatService.chatStream(request.getMessages(), new AiChatService.StreamCallback() {
                    @Override
                    public void onDelta(String text) {
                        sendEvent(emitter, "delta", Map.of("text", text));
                    }

                    @Override
                    public void onTool(String name, String label) {
                        sendEvent(emitter, "tool", Map.of("name", name, "label", label));
                    }

                    @Override
                    public void onDone(String fullContent) {
                        sendEvent(emitter, "done", Map.of("content", fullContent));
                        emitter.complete();
                    }
                });
            } catch (AiChatClient.AiChatException e) {
                sendEvent(emitter, "error", Map.of("message", e.getMessage()));
                emitter.complete();
            } catch (Exception e) {
                log.error("AI 流式回答失败", e);
                sendEvent(emitter, "error", Map.of("message", "AI 服务暂时不可用，请稍后重试"));
                emitter.complete();
            }
        });
        emitter.onTimeout(emitter::complete);
        emitter.onError(t -> log.debug("AI SSE 连接中断: {}", t.getMessage()));
        return emitter;
    }

    private void acquireUserQuota() {
        String user = currentUserKey();
        RateLimiter limiter = userLimiters.computeIfAbsent(user,
                k -> RateLimiter.create(perUserRequestsPerSecond));
        if (!limiter.tryAcquire()) {
            throw new AiChatClient.AiChatException("发送太频繁，请稍等几秒再试");
        }
    }

    private String currentUserKey() {
        var auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        return auth != null && auth.getName() != null ? auth.getName() : "anonymous";
    }

    private void sendEvent(SseEmitter emitter, String event, Object payload) {
        try {
            // payload 统一 JSON 序列化，避免文本换行破坏 SSE 分帧
            emitter.send(SseEmitter.event().name(event).data(objectMapper.writeValueAsString(payload)));
        } catch (IOException | IllegalStateException e) {
            // 客户端已断开等情况：放弃本次写入，后续事件同样被忽略，由 complete 收尾
            log.debug("AI SSE 事件发送失败（event={}）: {}", event, e.getMessage());
        }
    }
}
