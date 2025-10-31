package com.example.demo.socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.example.demo.model.Device;
import com.example.demo.repository.DeviceRepository;

/**
 * 最简单的手环协议服务器 - Spring 集成版本
 * 功能：监听手环连接，解析协议，保存数据，回复确认
 */
@Component
public class MpbandServer implements SmartLifecycle {
    private static final Logger log = LoggerFactory.getLogger(MpbandServer.class);

    @Value("${app.mpband.port:9000}")
    private int port;

    // 注入设备仓库，用于保存第一次登录的 IMEI
    private final DeviceRepository deviceRepository;

    // 协议常量
    private static final String HEADER = "IW";
    private static final String END_MARKER = "#";

    private volatile boolean running = false;
    private ServerSocket serverSocket;
    private ExecutorService clientPool;
    private Thread acceptThread;

    public MpbandServer(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    @Override
    public synchronized void start() {
        if (running) return;
        try {
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress("0.0.0.0", port));

            clientPool = Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r, "mpband-client-" + UUID.randomUUID());
                t.setDaemon(true);
                return t;
            });

            acceptThread = new Thread(this::acceptLoop, "mpband-acceptor");
            acceptThread.setDaemon(true);
            running = true;
            acceptThread.start();
            log.info("🚀 MpbandServer started, listening on {}", getBoundPort());
        } catch (IOException e) {
            running = false;
            closeQuietly(serverSocket);
            log.error("❌ Failed to start MpbandServer on port {}", port, e);
            throw new IllegalStateException("Failed to start MpbandServer", e);
        }
    }

    private void acceptLoop() {
        log.info("✅ 服务器启动成功！等待手环连接...");
        while (running && !serverSocket.isClosed()) {
            try {
                Socket client = serverSocket.accept();
                log.info("📞 新手环连接: {}:{}", client.getInetAddress().getHostAddress(), client.getPort());
                clientPool.submit(() -> handleBracelet(client));
            } catch (SocketException se) {
                if (running) {
                    log.warn("Server socket exception: {}", se.getMessage());
                }
            } catch (IOException e) {
                if (running) {
                    log.error("Accept failed", e);
                }
            }
        }
    }

    /** 处理单个手环连接 */
    private void handleBracelet(Socket clientSocket) {
        String clientInfo = clientSocket.getInetAddress().getHostAddress();
        BufferedReader reader = null;
        BufferedWriter writer = null;
        try {
            reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
            writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8));

            log.info("🔄 开始处理手环: {}", clientInfo);

            String message;
            while ((message = reader.readLine()) != null) {
                log.info("📨 收到原始数据: {}", message);

                // 1. 解析数据包
                BraceletPacket packet = parsePacket(message);
                if (packet == null) {
                    log.warn("❌ 数据包格式错误");
                    continue;
                }

                // 2. 处理数据
                processPacket(packet, clientInfo);

                // 3. 回复手环
                String response = createResponse(packet);
                writeLine(writer, response);
                log.info("📤 发送回复: {}", response);
            }
        } catch (IOException e) {
            log.info("🔌 手环断开连接: {}", clientInfo);
        } finally {
            try { if (reader != null) reader.close(); } catch (IOException ignored) {}
            try { if (writer != null) writer.close(); } catch (IOException ignored) {}
            try { if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close(); } catch (IOException ignored) {}
        }
    }

    /** 解析手环数据包 - 核心解析逻辑 */
    private BraceletPacket parsePacket(String rawData) {
        // 基本格式检查
        if (rawData == null || !rawData.startsWith(HEADER) || !rawData.endsWith(END_MARKER)) {
            log.warn("❌ 数据包头尾格式错误");
            return null;
        }

        if (rawData.length() < 6 + 1) { // 最少应包含 HEADER(2) + 协议号(4) + END(1)
            log.warn("❌ 数据包长度太短");
            return null;
        }

        try {
            BraceletPacket packet = new BraceletPacket();

            // 提取协议号 (位置2-5，共4字符)
            packet.protocolNo = rawData.substring(2, 6);
            log.debug("🔢 协议号: {}", packet.protocolNo);

            // AP00 登录包格式特殊：协议号之后直接是 IMEI / 其它登录信息
            if ("AP00".equals(packet.protocolNo)) {
                packet.serialNo = "";
                // payload 从协议号后开始，到结束符之前
                packet.rawPayload = rawData.substring(6, rawData.length() - 1);
                log.debug("🔎 AP00 payload: {}", packet.rawPayload);
            } else {
                // 提取流水号 (位置6-11，共6字符)
                if (rawData.length() >= 12) {
                    packet.serialNo = rawData.substring(6, 12);
                    log.debug("🎫 流水号: {}", packet.serialNo);
                } else {
                    packet.serialNo = "";
                }

                // 提取参数 (位置12到倒数第2个字符)
                if (rawData.length() > 13) {
                    String paramStr = rawData.substring(12, rawData.length() - 1);
                    packet.params = parseParams(paramStr);
                    log.debug("📊 参数: {}", packet.params);
                } else {
                    packet.params = new HashMap<>();
                }
            }

            packet.receiveTime = new Date();
            return packet;

        } catch (Exception e) {
            log.warn("❌ 解析数据包异常: {}", e.getMessage());
            return null;
        }
    }

    /** 解析参数部分 (如: "heartRate=75,steps=8000") */
    private Map<String, String> parseParams(String paramStr) {
        Map<String, String> params = new HashMap<>();
        if (paramStr == null || paramStr.isEmpty()) {
            return params;
        }
        String[] pairs = paramStr.split(",");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                String key = keyValue[0];
                String value = keyValue[1];
                if (isUnicodeString(value)) {
                    params.put(key, unicodeToString(value));
                } else {
                    params.put(key, value);
                }
            }
        }
        return params;
    }

    /** 处理解析后的数据包 */
    private void processPacket(BraceletPacket packet, String clientInfo) {
        log.info("🔄 处理数据包...");
        switch (packet.protocolNo) {
            case "AP00": // 登录包
                log.info("🔐 处理登录包");
                handleLogin(packet, clientInfo);
                break;
            case "AP01": // 心跳包
                log.info("💓 处理心跳包");
                break;
            case "AP02": // 健康数据
                log.info("🏃 处理健康数据");
                saveHealthData(packet.params);
                break;
            case "AP03": // 位置数据
                log.info("📍 处理位置数据");
                saveLocationData(packet.params);
                break;
            default:
                log.info("❓ 未知协议类型: {}", packet.protocolNo);
        }
    }

    /** 处理 AP00 登录包并在数据库中保存设备信息（首次登录） */
    private void handleLogin(BraceletPacket packet, String clientInfo) {
        if (packet.rawPayload == null || packet.rawPayload.isEmpty()) {
            log.warn("AP00 包体为空，忽略登录");
            return;
        }

        // AP00 payload 可能是几种形式：
        // 1) IMEI
        // 2) IMEI,MCC|MNC|APN
        // 3) IMEI,ICCID,IMSI
        String[] parts = packet.rawPayload.split(",");
        String imei = parts[0].trim();

        if (imei.length() != 15 || !imei.chars().allMatch(Character::isDigit)) {
            log.warn("无效的 IMEI: {}", imei);
            return;
        }

        // 查询并插入（如果不存在）
        Optional<Device> opt = deviceRepository.findByImei(imei);
        if (opt.isPresent()) {
            log.info("设备已存在 IMEI={}", imei);
            return;
        }

        Device device = new Device();
        device.setImei(imei);
        device.setCreatedAt(new Date());

        // 如果包含 MCC|MNC|APN
        if (parts.length >= 2 && parts[1].contains("|")) {
            String[] net = parts[1].split("\\|", 3);
            if (net.length >= 1) device.setMcc(net[0]);
            if (net.length >= 2) device.setMnc(net[1]);
            if (net.length >= 3) device.setApn(net[2]);
        }

        // 如果包含 ICCID,IMSI（parts 长度 >=3 时）
        if (parts.length >= 3 && !parts[1].contains("|")) {
            device.setIccid(parts[1].trim());
            device.setImsi(parts[2].trim());
        }

        try {
            deviceRepository.save(device);
            log.info("💾 新设备已保存 IMEI={} 来自 {}", imei, clientInfo);
        } catch (Exception e) {
            log.error("保存设备失败 IMEI={}", imei, e);
        }
    }

    /** 保存健康数据到文件（简单版本） */
    private void saveHealthData(Map<String, String> params) {
        try (FileWriter fw = new FileWriter("health_data.txt", true);
             PrintWriter out = new PrintWriter(fw)) {
            String record = String.format("[%s] 心率: %s, 步数: %s",
                    new Date(),
                    params.getOrDefault("heartRate", "未知"),
                    params.getOrDefault("steps", "未知"));
            out.println(record);
            log.info("💾 保存健康数据: {}", record);
        } catch (IOException e) {
            log.warn("❌ 保存健康数据失败: {}", e.getMessage());
        }
    }

    /** 保存位置数据到文件（简单版本） */
    private void saveLocationData(Map<String, String> params) {
        try (FileWriter fw = new FileWriter("location_data.txt", true);
             PrintWriter out = new PrintWriter(fw)) {
            String record = String.format("[%s] 位置: %s, 坐标: %s,%s",
                    new Date(),
                    params.getOrDefault("address", "未知地址"),
                    params.getOrDefault("lat", "未知"),
                    params.getOrDefault("lng", "未知"));
            out.println(record);
            log.info("💾 保存位置数据: {}", record);
        } catch (IOException e) {
            log.warn("❌ 保存位置数据失败: {}", e.getMessage());
        }
    }

    /** 创建回复给手环的数据包 */
    private String createResponse(BraceletPacket packet) {
        // 上行协议转下行协议: AP00 -> BP00
        String responseProtocol = "B" + packet.protocolNo.substring(1);

        // 如果是登录包，按协议要求返回：IWBP00,UTC时间(yyyyMMddHHmmss),时区#
        if ("AP00".equals(packet.protocolNo)) {
            // UTC 时间
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC);
            String utc = fmt.format(Instant.now());
            // 本机时区相对于 UTC 的小时偏移（负数表示西经）
            ZoneOffset offset = ZoneId.systemDefault().getRules().getOffset(Instant.now());
            int tzHours = offset.getTotalSeconds() / 3600;
            return HEADER + responseProtocol + "," + utc + "," + tzHours + END_MARKER;
        }

        Map<String, String> responseParams = new LinkedHashMap<>();
        responseParams.put("status", "SUCCESS");
        responseParams.put("time", String.valueOf(System.currentTimeMillis()));
        return HEADER +
                responseProtocol +
                packet.serialNo +
                encodeParams(responseParams) +
                END_MARKER;
    }

    /** 编码参数（简单版本，不处理中文） */
    private String encodeParams(Map<String, String> params) {
        if (params.isEmpty()) return "";
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (result.length() > 0) result.append(",");
            result.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return result.toString();
    }

    /** 判断字符串是否为Unicode编码 */
    private boolean isUnicodeString(String str) {
        return str != null && str.matches("[0-9a-fA-F]{4,}");
    }

    /** Unicode转字符串（简单版本） */
    private String unicodeToString(String unicodeStr) {
        try {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < unicodeStr.length(); i += 4) {
                if (i + 4 <= unicodeStr.length()) {
                    String hex = unicodeStr.substring(i, i + 4);
                    char ch = (char) Integer.parseInt(hex, 16);
                    result.append(ch);
                }
            }
            return result.toString();
        } catch (Exception e) {
            return unicodeStr; // 解析失败返回原字符串
        }
    }

    @Override
    public synchronized void stop() {
        if (!running) return;
        running = false;
        closeQuietly(serverSocket);
        if (acceptThread != null) {
            try { acceptThread.join(1000); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        }
        if (clientPool != null) {
            clientPool.shutdownNow();
            try { clientPool.awaitTermination(2, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        log.info("TCP MpbandServer stopped");
    }

    @Override
    public boolean isRunning() { return running; }

    @Override
    public boolean isAutoStartup() { return true; }

    public int getBoundPort() {
        return (serverSocket != null && serverSocket.isBound()) ? serverSocket.getLocalPort() : -1;
    }

    private void writeLine(BufferedWriter out, String s) throws IOException {
        out.write(s);
        out.write("\r\n");
        out.flush();
    }

    private void closeQuietly(ServerSocket s) {
        if (s != null && !s.isClosed()) {
            try { s.close(); } catch (IOException ignored) {}
        }
    }

    /** 数据包结构 */
    static class BraceletPacket {
        String protocolNo;      // 协议号：AP01, AP02等
        String serialNo;        // 流水号：6位数字（部分协议可能为空）
        Map<String, String> params; // 参数键值对
        String rawPayload;      // 原始包体（用于 AP00 登录包）
        Date receiveTime;       // 接收时间
        BraceletPacket() { this.params = new HashMap<>(); }
    }
}

