package com.example.demo.controller.api;

import com.example.demo.model.dto.AiChatRequest;
import com.example.demo.service.AiChatClient;
import com.example.demo.service.AiChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AiChatController 集成测试（MockMvc）
 */
@WebMvcTest(value = AiChatController.class, properties = {
        // 放宽每用户限流，避免用例间共享限流器导致误拦
        "app.ai.user-requests-per-second=1000"
}, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration.class,
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(MockitoExtension.class)
class AiChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AiChatService aiChatService;

    @MockBean
    private com.google.common.util.concurrent.RateLimiter rateLimiter;

    private String bodyWithMessage(String text) throws Exception {
        AiChatRequest request = new AiChatRequest();
        request.setMessages(List.of(new AiChatRequest.ChatMessage("user", text)));
        return objectMapper.writeValueAsString(request);
    }

    @Test
    void status_returnsEnvelopeWithConfigFlags() throws Exception {
        when(aiChatService.isEnabled()).thenReturn(true);
        when(aiChatService.isConfigured()).thenReturn(true);
        when(aiChatService.getModel()).thenReturn("deepseek-chat");

        mockMvc.perform(MockMvcRequestBuilders.get("/api/ai/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.configured").value(true))
                .andExpect(jsonPath("$.data.model").value("deepseek-chat"));
    }

    @Test
    void chat_returnsContentInEnvelope() throws Exception {
        when(aiChatService.chat(anyList())).thenReturn("今天有 3 条待处理告警");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyWithMessage("今天有多少告警")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").value("今天有 3 条待处理告警"));
    }

    @Test
    void chat_notConfigured_returns503WithMessage() throws Exception {
        when(aiChatService.chat(anyList()))
                .thenThrow(new AiChatClient.AiChatException("AI 助手尚未配置，请联系管理员设置环境变量 AI_API_KEY"));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyWithMessage("你好")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(503))
                .andExpect(jsonPath("$.message").value(
                        "AI 助手尚未配置，请联系管理员设置环境变量 AI_API_KEY"));
    }

    @Test
    void chatStream_emitsDeltaAndDoneEvents() throws Exception {
        doAnswer(invocation -> {
            AiChatService.StreamCallback callback = invocation.getArgument(1);
            callback.onDelta("今日");
            callback.onDelta("告警 2 条");
            callback.onDone("今日告警 2 条");
            return null;
        }).when(aiChatService).chatStream(anyList(), any(AiChatService.StreamCallback.class));

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/ai/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyWithMessage("今天告警")))
                .andExpect(request().asyncStarted())
                .andReturn();

        String stream = mockMvc.perform(MockMvcRequestBuilders.asyncDispatch(result))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);

        assertTrue(stream.contains("event:delta"), "应包含 delta 事件: " + stream);
        assertTrue(stream.contains("今日"), "应包含增量文本: " + stream);
        assertTrue(stream.contains("event:done"), "应包含 done 事件: " + stream);
    }
}
