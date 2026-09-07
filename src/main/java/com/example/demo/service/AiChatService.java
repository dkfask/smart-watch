package com.example.demo.service;

import com.example.demo.model.dto.AiChatRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * AI 助手编排：组装系统提示词与会话历史，执行「模型 → 工具 → 模型」循环，
 * 直到模型给出最终回答或达到工具轮次上限。
 */
@Service
public class AiChatService {

    private static final int MAX_HISTORY_MESSAGES = 16;

    private final AiChatClient client;
    private final AiToolService toolService;
    private final boolean enabled;
    private final int maxToolRounds;

    @Autowired
    public AiChatService(AiChatClient client, AiToolService toolService, Environment env) {
        this(client, toolService,
                !"false".equalsIgnoreCase(env.getProperty("app.ai.enabled", "true")),
                parseIntSafely(env.getProperty("app.ai.max-tool-rounds", "4")));
    }

    AiChatService(AiChatClient client, AiToolService toolService, boolean enabled, int maxToolRounds) {
        this.client = client;
        this.toolService = toolService;
        this.enabled = enabled;
        this.maxToolRounds = Math.max(1, maxToolRounds);
    }

    public boolean isEnabled() {
        return enabled && client.isConfigured();
    }

    public boolean isConfigured() {
        return client.isConfigured();
    }

    public String getModel() {
        return client.getModel();
    }

    /** 流式回答回调 */
    public interface StreamCallback {
        void onDelta(String text);

        void onTool(String name, String label);

        void onDone(String fullContent);
    }

    /**
     * 非流式回答（降级通道与测试用）
     */
    public String chat(List<AiChatRequest.ChatMessage> history) {
        requireConfigured();
        List<Map<String, Object>> messages = buildMessages(history);
        List<Map<String, Object>> tools = toolService.toolDefinitions();
        for (int round = 0; round <= maxToolRounds; round++) {
            AiChatClient.ChatResult result = client.chat(messages, tools);
            if (result.toolCalls.isEmpty()) {
                return result.content != null && !result.content.isBlank()
                        ? result.content
                        : "抱歉，AI 没有返回有效内容，请稍后重试。";
            }
            messages.add(assistantToolCallMessage(result));
            for (AiChatClient.ToolCall tc : result.toolCalls) {
                messages.add(toolMessage(tc.id, toolService.executeTool(tc.name, tc.arguments)));
            }
        }
        return "抱歉，这个问题需要的查询步骤超出了一次回答的限制，请把问题拆小一些再试。";
    }

    /**
     * 流式回答：增量文本通过回调逐段吐出，结束后回调 onDone(完整内容)
     */
    public void chatStream(List<AiChatRequest.ChatMessage> history, StreamCallback callback) {
        requireConfigured();
        List<Map<String, Object>> messages = buildMessages(history);
        List<Map<String, Object>> tools = toolService.toolDefinitions();
        StringBuilder full = new StringBuilder();
        for (int round = 0; round <= maxToolRounds; round++) {
            AiChatClient.ChatResult result = client.chatStream(messages, tools,
                    delta -> {
                        full.append(delta);
                        callback.onDelta(delta);
                    });
            if (result.toolCalls.isEmpty()) {
                // 以客户端累积的完整内容为准；兜底使用回调累积的文本
                callback.onDone(result.content != null ? result.content : full.toString());
                return;
            }
            messages.add(assistantToolCallMessage(result));
            for (AiChatClient.ToolCall tc : result.toolCalls) {
                callback.onTool(tc.name, toolService.toolLabel(tc.name));
                messages.add(toolMessage(tc.id, toolService.executeTool(tc.name, tc.arguments)));
            }
        }
        full.append("\n\n（查询步骤超出限制，以上为部分结果，请把问题拆小一些再试。）");
        callback.onDone(full.toString());
    }

    private void requireConfigured() {
        if (!client.isConfigured()) {
            throw new AiChatClient.AiChatException("AI 助手尚未配置，请联系管理员设置环境变量 AI_API_KEY");
        }
    }

    private List<Map<String, Object>> buildMessages(List<AiChatRequest.ChatMessage> history) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt()));
        if (history == null) {
            history = List.of();
        }
        int from = Math.max(0, history.size() - MAX_HISTORY_MESSAGES);
        int valid = 0;
        for (int i = from; i < history.size(); i++) {
            AiChatRequest.ChatMessage m = history.get(i);
            boolean userOrAssistant = "user".equals(m.getRole()) || "assistant".equals(m.getRole());
            if (!userOrAssistant || m.getContent() == null || m.getContent().isBlank()) {
                continue;
            }
            messages.add(Map.of("role", m.getRole(), "content", m.getContent()));
            valid++;
        }
        if (valid == 0) {
            throw new AiChatClient.AiChatException("请输入您的问题");
        }
        return messages;
    }

    private String systemPrompt() {
        LocalDate today = LocalDate.now();
        String weekday = today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.CHINA);
        return """
                你是「智能手环监护平台」的 AI 助手，服务对象是护理机构的护理人员和值班人员。你可以通过工具函数查询平台的真实数据：设备（在线状态、电量、最后位置）、患者（病房床位、诊断、在院状态）、告警（SOS、跌倒、心率异常、低电量、围栏越界等）、健康数据（心率、血压、血氧、体温）。

                今天是 %s（%s）。

                回答规则：
                1. 涉及平台实时数据的问题，必须先调用工具查询，再基于返回结果回答；禁止编造数据或猜测查询结果。
                2. 工具返回为空或报错时，如实说明“暂无相关数据”，不要虚构。
                3. 用简体中文回答，语气专业、简洁；多项数据时用短列表呈现。
                4. 涉及紧急情况（SOS、跌倒、严重心率异常等）时，提醒值班人员立即人工核实并按应急预案处置；你是辅助工具，不能代替人工处置。
                5. 不要提供医疗诊断或用药建议；健康数据只描述数值与一般常识，必要时建议咨询医生。
                6. 数据类型说明：heart_rate=心率(次/分)，blood_pressure=血压(mmHg)，blood_oxygen=血氧饱和度(%%)，temperature=体温(℃)。
                """.formatted(today.format(DateTimeFormatter.ISO_LOCAL_DATE), weekday);
    }

    private Map<String, Object> assistantToolCallMessage(AiChatClient.ChatResult result) {
        Map<String, Object> message = new HashMap<>();
        message.put("role", "assistant");
        message.put("content", result.content);
        List<Map<String, Object>> calls = new ArrayList<>();
        for (int i = 0; i < result.toolCalls.size(); i++) {
            AiChatClient.ToolCall tc = result.toolCalls.get(i);
            String id = tc.id != null ? tc.id : ("call_" + i + "_" + System.nanoTime());
            Map<String, Object> fn = new HashMap<>();
            fn.put("name", tc.name);
            fn.put("arguments", tc.arguments);
            Map<String, Object> call = new HashMap<>();
            call.put("id", id);
            call.put("type", "function");
            call.put("function", fn);
            calls.add(call);
        }
        message.put("tool_calls", calls);
        return message;
    }

    private Map<String, Object> toolMessage(String toolCallId, String resultJson) {
        Map<String, Object> message = new HashMap<>();
        message.put("role", "tool");
        message.put("tool_call_id", toolCallId);
        message.put("content", resultJson);
        return message;
    }

    private static int parseIntSafely(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 4;
        }
    }
}
