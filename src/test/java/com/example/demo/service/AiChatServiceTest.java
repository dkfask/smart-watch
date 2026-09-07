package com.example.demo.service;

import com.example.demo.model.dto.AiChatRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AiChatService 单元测试：编排循环、未配置守卫、流式回调
 */
class AiChatServiceTest {

    private AiChatClient client;
    private AiToolService toolService;
    private AiChatService service;

    @BeforeEach
    void setUp() {
        client = mock(AiChatClient.class);
        toolService = mock(AiToolService.class);
        service = new AiChatService(client, toolService, true, 4);
        when(client.isConfigured()).thenReturn(true);
        when(toolService.toolDefinitions()).thenReturn(List.of());
    }

    private static List<AiChatRequest.ChatMessage> history(String text) {
        List<AiChatRequest.ChatMessage> messages = new ArrayList<>();
        messages.add(new AiChatRequest.ChatMessage("user", text));
        return messages;
    }

    // === 配置守卫 ===

    @Test
    void chat_notConfigured_throwsFriendlyError() {
        when(client.isConfigured()).thenReturn(false);
        AiChatClient.AiChatException ex = assertThrows(AiChatClient.AiChatException.class,
                () -> service.chat(history("你好")));
        assertTrue(ex.getMessage().contains("AI_API_KEY"));
        verify(client, never()).chat(anyList(), anyList());
    }

    @Test
    void chatStream_notConfigured_throwsFriendlyError() {
        when(client.isConfigured()).thenReturn(false);
        assertThrows(AiChatClient.AiChatException.class,
                () -> service.chatStream(history("你好"), new AiChatService.StreamCallback() {
                    @Override
                    public void onDelta(String text) {
                    }

                    @Override
                    public void onTool(String name, String label) {
                    }

                    @Override
                    public void onDone(String fullContent) {
                    }
                }));
    }

    // === 非流式 ===

    @Test
    void chat_directAnswer_returnsContent() {
        AiChatClient.ChatResult result = new AiChatClient.ChatResult();
        result.content = "当前在线设备 3 台";
        when(client.chat(anyList(), anyList())).thenReturn(result);

        String answer = service.chat(history("有多少设备在线"));

        assertEquals("当前在线设备 3 台", answer);
        verify(toolService, never()).executeTool(any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void chat_toolLoop_executesToolAndReturnsFinalAnswer() {
        AiChatClient.ChatResult toolRound = new AiChatClient.ChatResult();
        toolRound.toolCalls.add(new AiChatClient.ToolCall("call-1", "get_alarm_stats", "{}"));
        AiChatClient.ChatResult finalRound = new AiChatClient.ChatResult();
        finalRound.content = "待处理告警 2 条";

        when(client.chat(anyList(), anyList()))
                .thenReturn(toolRound)
                .thenReturn(finalRound);
        when(toolService.executeTool("get_alarm_stats", "{}")).thenReturn("{\"pending\":2}");

        String answer = service.chat(history("有多少待处理告警"));

        assertEquals("待处理告警 2 条", answer);

        ArgumentCaptor<List<Map<String, Object>>> captor =
                ArgumentCaptor.forClass((Class<List<Map<String, Object>>>) (Class<?>) List.class);
        verify(client, times(2)).chat(captor.capture(), anyList());
        List<Map<String, Object>> secondCallMessages = captor.getAllValues().get(1);
        boolean hasToolMessage = secondCallMessages.stream()
                .anyMatch(m -> "tool".equals(m.get("role"))
                        && String.valueOf(m.get("content")).contains("\"pending\":2"));
        assertTrue(hasToolMessage, "第二轮消息应包含工具结果");
    }

    @Test
    void chat_maxRoundsExceeded_returnsGuidance() {
        AiChatClient.ChatResult toolRound = new AiChatClient.ChatResult();
        toolRound.toolCalls.add(new AiChatClient.ToolCall("call-1", "list_devices", "{}"));
        when(client.chat(anyList(), anyList())).thenReturn(toolRound);
        when(toolService.executeTool(any(), any())).thenReturn("{}");

        AiChatService limited = new AiChatService(client, toolService, true, 1);
        String answer = limited.chat(history("复杂问题"));

        assertTrue(answer.contains("拆小"));
    }

    @Test
    void chat_emptyHistory_throws() {
        assertThrows(AiChatClient.AiChatException.class, () -> service.chat(List.of()));
    }

    // === 流式 ===

    @Test
    void chatStream_accumulatesDeltasAndCompletes() {
        AiChatClient.ChatResult result = new AiChatClient.ChatResult();
        when(client.chatStream(anyList(), anyList(), any(AiChatClient.ContentDeltaListener.class)))
                .thenAnswer(invocation -> {
                    AiChatClient.ContentDeltaListener listener = invocation.getArgument(2);
                    listener.onDelta("今天");
                    listener.onDelta("告警 2 条");
                    return result;
                });

        List<String> deltas = new ArrayList<>();
        List<String> done = new ArrayList<>();
        service.chatStream(history("今天告警"), new AiChatService.StreamCallback() {
            @Override
            public void onDelta(String text) {
                deltas.add(text);
            }

            @Override
            public void onTool(String name, String label) {
            }

            @Override
            public void onDone(String fullContent) {
                done.add(fullContent);
            }
        });

        assertEquals(List.of("今天", "告警 2 条"), deltas);
        assertEquals("今天告警 2 条", done.get(0));
    }

    @Test
    void chatStream_toolRound_reportsToolThenFinalAnswer() {
        AiChatClient.ChatResult toolRound = new AiChatClient.ChatResult();
        toolRound.toolCalls.add(new AiChatClient.ToolCall("call-1", "list_patients", "{}"));
        AiChatClient.ChatResult finalRound = new AiChatClient.ChatResult();
        finalRound.content = "在院患者 5 人";

        when(client.chatStream(anyList(), anyList(), any(AiChatClient.ContentDeltaListener.class)))
                .thenReturn(toolRound)
                .thenReturn(finalRound);
        when(toolService.executeTool("list_patients", "{}")).thenReturn("{\"patients\":[]}");
        when(toolService.toolLabel("list_patients")).thenReturn("患者列表");

        List<String> tools = new ArrayList<>();
        List<String> done = new ArrayList<>();
        service.chatStream(history("有哪些患者"), new AiChatService.StreamCallback() {
            @Override
            public void onDelta(String text) {
            }

            @Override
            public void onTool(String name, String label) {
                tools.add(name + ":" + label);
            }

            @Override
            public void onDone(String fullContent) {
                done.add(fullContent);
            }
        });

        assertEquals(List.of("list_patients:患者列表"), tools);
        assertEquals("在院患者 5 人", done.get(0));
    }
}
