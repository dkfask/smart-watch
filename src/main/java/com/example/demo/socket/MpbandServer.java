package com.example.demo.socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import com.example.demo.model.Device;
import com.example.demo.model.LocationRecord;
import com.example.demo.model.HeartbeatRecord;
import com.example.demo.model.HealthRecord;
import com.example.demo.model.DeviceStatus;
import com.example.demo.repository.DeviceRepository;
import com.example.demo.repository.DeviceStatusRepository;
import com.example.demo.repository.HealthRecordRepository;
import com.example.demo.repository.HeartbeatRecordRepository;
import com.example.demo.repository.LocationRecordRepository;
import com.example.demo.service.AmapLocationService;
import com.example.demo.service.HealthMonitorService;
import com.example.demo.service.TrackingService;
import com.example.demo.socket.downlink.DownlinkManager;
import com.example.demo.socket.processor.LogProcessor;
import com.example.demo.socket.processor.PacketProcessor;
import com.example.demo.socket.processor.ResponseGenerator;
import com.example.demo.socket.protocol.BraceletPacket;
import com.example.demo.socket.protocol.ProtocolParser;
import com.example.demo.socket.protocol.ProtocolException;

/**
 * 最简单的手环协议服务器 - Spring 集成版本
 * 功能：监听手环连接，解析协议，保存数据，回复确认
 *
 * 新增功能：
 *  - 将服务器接收的每条上行报文以文件方式保存到磁盘（默认保存到应用运行目录下的 mpband_data/raw）
 *  - 按设备 IMEI 将通信记录追加到设备专属日志（mpband_data/devices/{imei}.log）
 *  - 并发写入采用基于每设备的锁保证线程安全
 */
@Component
public class MpbandServer implements SmartLifecycle {
    private static final Logger log = LoggerFactory.getLogger(MpbandServer.class);

    @Value("${app.mpband.port:9000}")
    private int port;

    /** 是否使用基于 '#' 分隔符的帧读取模式 */
    @Value("${app.mpband.useDelimiterReader:true}")
    private boolean useDelimiterReader;

    @Value("${app.mpband.frameDelimiter:#}")
    private String frameDelimiter;

    @Value("${app.mpband.appendNewlineAfterResponse:false}")
    private boolean appendNewlineAfterResponse;

    @Value("${app.mpband.soTimeoutMillis:180000}")
    private int soTimeoutMillis;

    @Value("${app.mpband.maxFrameLength:64}")
    private int maxFrameLength;

    // 新增：允许的最大帧长度（超过此长度才真正进入丢弃模式），默认 16KB
    @Value("${app.mpband.maxAllowedFrameLength:16384}")
    private int maxAllowedFrameLength;

    // 防御性配置：当丢弃字节过多时是否断开连接
    @Value("${app.mpband.disconnectOnExcessiveDrop:true}")
    private boolean disconnectOnExcessiveDrop;
    // 丢弃字节阈值，超过则断开连接（单位字节）
    @Value("${app.mpband.dropThresholdBytes:16384}")
    private long dropThresholdBytes;
    // 连续进入丢弃模式次数阈值，超过则断开连接（防止频繁短时间内多次进入）
    @Value("${app.mpband.maxConsecutiveDiscardEvents:8}")
    private int maxConsecutiveDiscardEvents;

    // 新增：保存目录（相对于运行目录 user.dir）
    @Value("${app.mpband.saveDir:mpband_data}")
    private String saveDirName;

    // 注入依赖
    private final DeviceRepository deviceRepository;
    private final DownlinkManager downlinkManager;
    private final LocationRecordRepository locationRecordRepository;
    private final HeartbeatRecordRepository heartbeatRecordRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final DeviceStatusRepository deviceStatusRepository;
    private final AmapLocationService amapLocationService;
    private final TrackingService trackingService;

    // 处理器类
    private final PacketProcessor packetProcessor;
    private final ResponseGenerator responseGenerator;
    private final LogProcessor logProcessor;

    // 协议常量
    private static final String HEADER = "IW";
    private static final String END_MARKER = "#";

    private volatile boolean running = false;
    private ServerSocket serverSocket;
    private ExecutorService clientPool;
    private Thread acceptThread;

    // 可选：在 AP00 回复中追加的可变 key（若为空则不追加）
    @Value("${app.mpband.responseKey:}")
    private String responseKey;

    // 用于从 payload 中识别 IMEI（15 位数字）
    private static final Pattern IMEI_PATTERN = Pattern.compile("[0-9]{15}");
    private final HealthMonitorService healthMonitorService;
    
    public MpbandServer(DeviceRepository deviceRepository,
                        DownlinkManager downlinkManager,
                        LocationRecordRepository locationRecordRepository,
                        HeartbeatRecordRepository heartbeatRecordRepository,
                        HealthRecordRepository healthRecordRepository,
                        DeviceStatusRepository deviceStatusRepository,
                        AmapLocationService amapLocationService,
                        HealthMonitorService healthMonitorService,
                        TrackingService trackingService) {
        this.deviceRepository = deviceRepository;
        this.downlinkManager = downlinkManager;
        this.locationRecordRepository = locationRecordRepository;
        this.heartbeatRecordRepository = heartbeatRecordRepository;
        this.healthRecordRepository = healthRecordRepository;
        this.deviceStatusRepository = deviceStatusRepository;
        this.amapLocationService = amapLocationService;
        this.healthMonitorService = healthMonitorService;
        this.trackingService = trackingService;
        
        // 初始化处理器类
        this.packetProcessor = new PacketProcessor(deviceRepository, downlinkManager, locationRecordRepository,
                heartbeatRecordRepository, healthRecordRepository, deviceStatusRepository, amapLocationService, healthMonitorService, trackingService);
        // 先创建logProcessor，然后再创建responseGenerator
        this.logProcessor = new LogProcessor(saveDirName);
        this.responseGenerator = new ResponseGenerator(responseKey, appendNewlineAfterResponse, logProcessor);
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

            // 日志处理器已在构造函数中初始化，无需在这里处理目录

            // 添加日志以便调试
            log.info("🚀 MpbandServer started, listening on {}", getBoundPort());
        } catch (IOException e) {
            running = false;
            closeQuietly(serverSocket);
            log.error("❌ Failed to start MpbandServer on port {}", port, e);
            throw new IllegalStateException("Failed to start MpbandServer", e);
        }
    }

    private void acceptLoop() {
        // 不再打印INFO级别日志，保持控制台简洁
        // log.info("✅ 服务器启动成功！等待手环连接...");
        while (running && !serverSocket.isClosed()) {
            try {
                Socket client = serverSocket.accept();
                // 不再打印INFO级别日志，保持控制台简洁
                // log.info("📞 新手环连接: {}:{}", client.getInetAddress().getHostAddress(), client.getPort());
                clientPool.submit(() -> handleBracelet(client));
            } catch (SocketException se) {
                if (running) {
                    // 不再打印WARN级别日志，保持控制台简洁
                    // log.warn("Server socket exception: {}", se.getMessage());
                }
            } catch (IOException e) {
                if (running) {
                    // 保留致命错误日志
                    log.error("Accept failed", e);
                }
            }
        }
    }

    /** 处理单个手环连接 */
    private void handleBracelet(Socket clientSocket) {
        String clientInfo = clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort();
        BufferedReader reader = null;
        BufferedOutputStream writer = null;
        InputStream rawIn;
        try {
            // 设置底层 Socket 选项，增加连接稳定性
            try {
                clientSocket.setKeepAlive(true);
                clientSocket.setSoTimeout(soTimeoutMillis);
                clientSocket.setTcpNoDelay(true);
            } catch (Exception e) {
                // 不再打印WARN级别日志，保持控制台简洁
                // log.warn("无法设置 Socket 选项: {}", e.getMessage());
            }

            rawIn = clientSocket.getInputStream();
            writer = new BufferedOutputStream(clientSocket.getOutputStream());

            // 不再打印INFO级别日志，保持控制台简洁
            // log.info("🔄 开始处理手环: {} (useDelimiterReader={})", clientInfo, useDelimiterReader);

            if (useDelimiterReader) {
                // 采用分隔符读取模式：按字符读取直到遇到分隔符（默认 '#')，不要求换行
                readFramesLoop(rawIn, writer, clientSocket, clientInfo);
            } else {
                // 回退：兼容旧逻辑（设备发送 '\r\n'）：按行读取
                try {
                    // 尝试使用GB2312编码解析，设备通常使用此编码
                    reader = new BufferedReader(new InputStreamReader(rawIn, "GB2312"));
                } catch (Exception e) {
                    // 若GB2312解析失败，回退到UTF-8
                    reader = new BufferedReader(new InputStreamReader(rawIn, StandardCharsets.UTF_8));
                }
                String message;
                while ((message = reader.readLine()) != null) {
                    handleOneMessage(message, writer, clientSocket, clientInfo);
                }
            }
        } catch (IOException e) {
            
        } finally {
            // 断开时注销下行管理器中的映射
            try { 
                // 获取设备IMEI
                String disconnectedImei = downlinkManager.getImeiBySocket(clientSocket);
                // 注销下行管理器中的映射
                downlinkManager.unregisterBySocket(clientSocket);
                // 如果IMEI不为空，将设备设置为离线状态
                if (disconnectedImei != null && !disconnectedImei.isEmpty()) {
                    deviceRepository.findByImei(disconnectedImei).ifPresent(device -> {
                        DeviceStatus status = new DeviceStatus();
                        status.setDeviceId(device.getId());
                        status.setImei(disconnectedImei);
                        status.setIsOnline(false);
                        status.setUpdatedAt(new Date());
                        deviceStatusRepository.upsert(status);
                        log.debug("✅ 更新设备在线状态: IMEI={}, 在线状态={}", disconnectedImei, false);
                    });
                }
            } catch (Exception ignored) {}
            try { if (reader != null) reader.close(); } catch (IOException ignored) {}
            try { if (writer != null) writer.close(); } catch (IOException ignored) {}
            try { if (!clientSocket.isClosed()) clientSocket.close(); } catch (IOException ignored) {}
        }
    }

    /**
     * 新增：分隔符读取主循环。读取字节直到遇到分隔符（默认 '#')，不要求换行
     * 说明：
     * 1. 很多手环/定位终端协议使用 '#' 作为结束符，但不保证追加换行；旧实现依赖 readLine() 可能导致阻塞，从而设备端等待 ACK 超时后主动重连 -> 形成“反复连接”现象。
     * 2. 这里采用逐字节累积 + 分隔符判断，避免阻塞在 readLine，同时也支持设备如果仍然发送换行不会出错（换行会被当作普通字符保留或过滤——此处简单过滤）。
     */
    private void readFramesLoop(InputStream in, BufferedOutputStream writer, Socket clientSocket, String clientInfo) throws IOException {
        String delim = (frameDelimiter == null || frameDelimiter.isEmpty()) ? "#" : frameDelimiter;
        char endChar = delim.charAt(0); // 当前仅支持单字符分隔符

        // 预置 StringBuilder，容量以 maxFrameLength 为参考，但不超过 maxAllowedFrameLength，避免频繁扩容
        int initialCap = Math.min(Math.max(32, maxFrameLength), Math.max(256, Math.min(maxAllowedFrameLength, 4096)));
        StringBuilder frame = new StringBuilder(initialCap);
        byte[] buf = new byte[512];
        int len;
        boolean discarding = false; // 丢弃模式：当单条帧超过限制时进入，跳过直到遇到分隔符
        long droppedBytes = 0L; // 丢弃字节统计（仅用于日志）
        int consecutiveDiscardEvents = 0; // 连续进入丢弃模式次数
        boolean growthWarned = false; // 是否已记录过一次 "扩展缓冲" 的告警/信息，以免日志刷屏

        while (true) {
            try {
                len = in.read(buf);
            } catch (SocketTimeoutException ste) {
                // 读取超时：不主动断开连接，继续等待数据到来
                log.debug("读取超时，继续等待数据 (client={})", clientInfo);
                continue;
            }

            if (len == -1) {
                // 流结束：对端关闭连接
                log.info("输入流已终止（对端关闭连接） client={}", clientInfo);
                break;
            }

            // 将字节数组转换为字符串，使用GB2312编码（设备常用编码）
            String chunk;
            try {
                // 尝试使用GB2312编码解析，设备通常使用此编码
                chunk = new String(buf, 0, len, "GB2312");
            } catch (Exception e) {
                // 若GB2312解析失败，回退到UTF-8
                chunk = new String(buf, 0, len, StandardCharsets.UTF_8);
            }

            for (int i = 0; i < chunk.length(); i++) {
                char c = chunk.charAt(i);
                // 过滤回车/换行
                if (c == '\r' || c == '\n') {
                    if (discarding) {
                        droppedBytes++;
                    }
                    continue;
                }

                if (discarding) {
                    // 丢弃所有字符直到遇到分隔符
                    if (c == endChar) {
                        // log.warn("丢弃模式结束，遇到分隔符 '{}'：已丢弃 {} 字节 (client={})", endChar, droppedBytes, clientInfo);
                        // 防御策略：仅记录并根据策略重置计数，不主动断开连接
                        consecutiveDiscardEvents++;
                        if (disconnectOnExcessiveDrop && (droppedBytes >= dropThresholdBytes || consecutiveDiscardEvents > maxConsecutiveDiscardEvents)) {
                            // log.error("检测到异常流量或连续丢弃({})，但当前配置为保留连接，已记录事件 (client={})");
                            // 不调用 clientSocket.close()，也不 return；仅重置统计以继续服务
                        }
                        droppedBytes = 0L;
                        discarding = false;
                        growthWarned = false; // 重置扩展告警状态
                    } else {
                        droppedBytes++;
                    }
                    continue;
                }

                frame.append(c);

                // 当长度超过配置的 maxFrameLength 时，不再立刻丢弃；如果仍在 maxAllowedFrameLength 范围内，则允许扩展并在日志记录一次
                if (frame.length() > maxAllowedFrameLength) {
                    // 超过真正允许的上限 -> 进入丢弃模式
                    droppedBytes = frame.length();
                    log.warn("帧长度超过允许上限({})，进入丢弃模式，直到遇到分隔符 '{}'。已丢弃 {} 字符 (client={})", maxAllowedFrameLength, endChar, droppedBytes, clientInfo);
                    // 进入丢弃模式前记录事件，但不主动断开连接
                consecutiveDiscardEvents++;
                if (disconnectOnExcessiveDrop && (droppedBytes >= dropThresholdBytes || consecutiveDiscardEvents > maxConsecutiveDiscardEvents)) {
                    log.error("帧过长且连续丢弃次数超过阈值({})，但当前配置为保留连接，已记录事件 (client={})", consecutiveDiscardEvents, clientInfo);
                    // 不调用 clientSocket.close()，也不 return；仅重置统计以继续服务
                }
                frame.setLength(0);
                discarding = true;
                continue;
            } else if (frame.length() > maxFrameLength) {
                // 超过了配置的阈值，但仍在允许上限内：记录一次信息并继续累积
                if (!growthWarned) {
                    
                    growthWarned = true;
                }
                // 继续累积
            }

                if (c == endChar) {
                    // 完整帧（已包含结束符）
                    String rawMessage = frame.toString();
                    frame.setLength(0); // 准备下一个
                    // 处理成功帧后重置连续丢弃计数及扩展告警标志
                    consecutiveDiscardEvents = 0;
                    growthWarned = false;
                    try {
                        handleOneMessage(rawMessage, writer, clientSocket, clientInfo);
                    } catch (Exception e) {
                        // 处理单帧发生异常：记录并继续（不要主动断开）
                        log.warn("处理报文时出现异常，但保持连接: {} (client={})", e.getMessage(), clientInfo);
                    }
                }
            }
        }

        // 读到流末尾时，如果处于丢弃模式，记录一次告警日志
        if (discarding && droppedBytes > 0) {
            log.warn("连接关闭时仍在丢弃模式：已丢弃 {} 字节 (client={})", droppedBytes, clientInfo);
        }
    }

    /**
     * 统一处理单条上行报文（无论来源是行模式还是分隔符模式）。
     * @param rawMessage 原始报文，应该以 '#' 结束（若设备未按协议发送，则可能解析失败）。
     */
    private void handleOneMessage(String rawMessage, BufferedOutputStream writer, Socket clientSocket, String clientInfo) throws IOException {
        if (rawMessage == null) return;
        String message = rawMessage.trim();
        log.debug("📨 收到原始数据: {}", message);

        // 保存原始报文到磁盘并追加到设备日志（尽量提取 IMEI）
        String imei = null;
        try {
            // 对于AP03协议，完全跳过IMEI处理，因为AP03报文本身不包含IMEI信息
            if (!message.startsWith("IWAP03")) {
                imei = getImeiFromRaw(message);
                // 如果原始报文中未包含 IMEI，尝试通过已注册的 socket->imei 映射获取设备号（在 AP00 登录后会由 DownlinkManager.register 注册）
                if ((imei == null || imei.isEmpty()) && clientSocket != null) {
                    try {
                        String bySocket = downlinkManager.getImeiBySocket(clientSocket);
                        if (bySocket != null && !bySocket.isEmpty()) {
                            imei = bySocket;
                            log.debug("🔗 通过 socket 映射解析出 imei={} for client={}", bySocket, clientInfo);
                        }
                    } catch (Exception ex) {
                        // 忽略 getImeiBySocket 可能抛出的异常
                        log.debug("⚠️ 无法通过 socket 获取 imei: {}", ex.getMessage());
                    }
                }
            } else {
                log.debug("📌 AP03协议，完全跳过IMEI处理");
            }
            logProcessor.saveRawAndDeviceLog(message, imei, clientInfo);
        } catch (Exception ex) {
            log.warn("💾 保存上行报文到磁盘失败: {}", ex.getMessage());
        }

        // 使用协议解析器（保留），同时把原始报文也传入以便按协议做更精确的解析
        BraceletPacket packet;
        try {
            packet = ProtocolParser.parse(message);
        } catch (ProtocolException e) {
            log.warn("❌ 数据包格式错误(无法解析) raw={}, error={}", message, e.getMessage());
            // 发送默认回复，确保设备不会一直重发
            String defaultResponse = HEADER + "BP00#";
            responseGenerator.writeFrame(writer, defaultResponse, clientSocket, clientInfo, imei);
            return;
        }

        // 处理（传入原始消息与已提取 imei）
        try {
            packetProcessor.processPacket(packet, message, clientInfo, clientSocket, imei);
        } catch (Exception e) {
            log.error("💥 处理数据包失败: raw={}, error={}", message, e.getMessage());
            // 发送默认回复，确保设备不会一直重发
            String defaultResponse = HEADER + "BP00#";
            responseGenerator.writeFrame(writer, defaultResponse, clientSocket, clientInfo, imei);
            return;
        }

        // 构造并发送回复
        try {
            String response = responseGenerator.createResponse(packet);
            responseGenerator.writeFrame(writer, response, clientSocket, clientInfo, imei);
        } catch (Exception e) {
            log.error("📤 发送回复失败: error={}", e.getMessage());
        }
    }

    /**
     * processPacket: 将 imei 传递给各子处理函数，确保后续保存操作可以基于 imei 查找/创建设备并关联记录。
     */
    private void processPacket(BraceletPacket packet, String raw, String clientInfo, Socket clientSocket, String imei) {
        String proto = packet.getProtocol();
        // 解析统一从 raw 中截取 payload（header+proto 长度为 2 + 4 = 6）
        String payload = "";
        if (raw != null && raw.length() > 6) {
            // 去掉开头 IWAPxx 并去掉尾部的 '#'
            int end = raw.endsWith(END_MARKER) ? raw.length() - 1 : raw.length();
            payload = raw.substring(6, end);
        }

        try {
            switch (proto) {
                case "AP00":
                    // 登录包：IWAP00+IMEI# 或 IWAP00+IMEI,MCC|MNC|APN# 或 IWAP00+IMEI,ICCID,IMSI#
                    handleAp00(payload, clientSocket, clientInfo);
                    break;
                case "AP01":
                    // 定位包：使用 ProtocolParser 的解析结果
                    Map<String, String> pktParams = packet.getParams();
                    // 直接把解析器的 params 作为保存参数传入（创建副本以避免共享修改）
                    Map<String, String> paramsToSave = new LinkedHashMap<>();
                    if (pktParams != null) paramsToSave.putAll(pktParams);
                    // 如果 GPS 无效但解析器提供了 wifiGeoLat/wifiGeoLon（或其他 wifi 坐标），优先使用它们作为定位结果
                    boolean hasLatOrLon = paramsToSave.containsKey("lat") || paramsToSave.containsKey("lon") || paramsToSave.containsKey("lng");
                    if (!hasLatOrLon) {
                        String wlat = paramsToSave.get("wifiGeoLat");
                        String wlon = paramsToSave.get("wifiGeoLon");
                        if (wlat != null && wlon != null && !wlat.isEmpty() && !wlon.isEmpty()) {
                            paramsToSave.put("lat", wlat);
                            paramsToSave.put("lon", wlon);
                            paramsToSave.put("locationSource", "wifi");
                        }
                    }
                    // 确保兼容键名：如果解析器只提供 lng，则把它映射到 lon
                    if (!paramsToSave.containsKey("lon") && paramsToSave.containsKey("lng")) paramsToSave.put("lon", paramsToSave.get("lng"));
                    saveLocationData(paramsToSave, imei);
                    log.debug("✅ AP01 处理完成, IMEI={}", imei);
                    break;
                case "AP02":
                    // 健康包（旧版 AP02 可能另用，此处如无明确需求可当作健康数据）
                    saveHealthData(parseKeyValueParams(payload), imei);
                    log.debug("✅ AP02 处理完成, IMEI={}", imei);
                    break;
                case "AP03":
                    handleAp03(payload, clientInfo, imei);
                    log.debug("✅ AP03 处理完成, IMEI={}", imei);
                    break;
                case "AP04":
                    handleAp04(payload, clientInfo, imei);
                    log.debug("✅ AP04 处理完成, IMEI={}", imei);
                    break;
                case "AP10":
                    handleAp10(payload, clientInfo, imei);
                    log.debug("✅ AP10 处理完成, IMEI={}", imei);
                    break;
                case "AP42":
                    handleAp42(payload, clientInfo, imei);
                    log.debug("✅ AP42 处理完成, IMEI={}", imei);
                    break;
                case "APBL":
                    handleApBl(payload, clientInfo, imei);
                    log.debug("✅ APBL 处理完成, IMEI={}", imei);
                    break;
                case "APJK":
                    handleApJk(payload, clientInfo, imei);
                    log.debug("✅ APJK 处理完成, IMEI={}", imei);
                    break;
                case "APTP":
                    handleApTp(payload, clientInfo, imei);
                    log.debug("✅ APTP 处理完成, IMEI={}", imei);
                    break;
                case "APVR":
                    handleApVr(payload, clientInfo, imei);
                    log.debug("✅ APVR 处理完成, IMEI={}", imei);
                    break;
                case "APWR":
                    handleApWr(payload, clientInfo, imei);
                    log.debug("✅ APWR 处理完成, IMEI={}", imei);
                    break;
                case "AP16":
                    log.debug("✅ AP16 处理完成, IMEI={}", imei);
                    break;
                default:
                    log.warn("❓ 未知协议类型: {} raw={}", proto, raw);
            }
        } catch (Exception e) {
            log.error("💥 处理数据包失败: 协议={}, raw={}, IMEI={}, 错误={}", proto, raw, imei, e.getMessage(), e);
        }
    }

    // --------------------------- 各协议处理函数（签名加入 imei 参数） ---------------------------

    /** 处理 AP00 登录包：使用 ProtocolParser 的解析结果 */
    private void handleAp00(String payload, Socket clientSocket, String clientInfo) {
        if (payload == null || payload.isEmpty()) return;
        String[] parts = payload.split(",");
        String imei = parts[0].trim();
        if (imei.length() != 15 || !imei.chars().allMatch(Character::isDigit)) {
            // 不再打印WARN级别日志，保持控制台简洁
            // log.warn("AP00: 无效 IMEI: {}");
            return;
        }

        // 注册下行连接
        try {
            downlinkManager.register(imei, clientSocket);
        } catch (Exception e) {
            // 不再打印WARN级别日志，保持控制台简洁
            // log.warn("注册下行连接失败 IMEI={}");
        }

        // 保存设备信息（若不存在）
        try {
            Optional<Device> opt = deviceRepository.findByImei(imei);
            if (opt.isPresent()) {
                // 不再打印INFO级别日志，保持控制台简洁
                // log.info("设备已存在 IMEI={}");
                return;
            }

            Device device = new Device();
            device.setImei(imei);
            device.setCreatedAt(new Date());

            if (parts.length >= 2) {
                String second = parts[1];
                if (second.contains("|")) {
                    String[] net = second.split("\\|", 3);
                    if (net.length >= 1) device.setMcc(net[0]);
                    if (net.length >= 2) device.setMnc(net[1]);
                    if (net.length >= 3) device.setApn(net[2]);
                } else if (parts.length >= 3) {
                    device.setIccid(parts[1].trim());
                    device.setImsi(parts[2].trim());
                }
            }
            deviceRepository.save(device);
            // 不再打印INFO级别日志，保持控制台简洁
            // log.info("💾 新设备已保存 IMEI={} 来自 {}");
        } catch (Exception e) {
            // 保留致命错误日志
            log.error("保存设备失败 IMEI={}");
        }
    }

    private void handleAp03(String payload, String clientInfo, String imei) {
        if (payload == null || payload.isEmpty()) return;
        // 不再打印DEBUG级别日志，保持控制台简洁
        // log.debug("AP03 来自 {} 的 payload: {}");
        String[] parts = payload.split(",");
        Map<String, String> params = new LinkedHashMap<>();
        params.put("param_block", parts.length > 0 ? parts[0] : "");
        if (parts.length > 1) params.put("steps", parts[1]);
        if (parts.length > 2) params.put("roll_count", parts[2]);
        if (parts.length > 3) params.put("work_mode", parts[3]);
        if (parts.length > 4) params.put("interval", parts[4]);

        saveHeartbeatData(params, imei);
        // 更新设备在线状态：收到AP03心跳包即表示设备在线
        if (imei != null && !imei.isEmpty()) {
            deviceRepository.findByImei(imei).ifPresent(device -> {
                DeviceStatus status = new DeviceStatus();
                status.setDeviceId(device.getId());
                status.setImei(imei);
                status.setIsOnline(true);
                status.setUpdatedAt(new Date());
                deviceStatusRepository.upsert(status);
                log.debug("✅ 更新设备在线状态: IMEI={}, 在线状态={}", imei, true);
            });
        }
        // 不再打印INFO级别日志，保持控制台简洁
        // log.info("AP03 心跳包已保存: {}");
    }

    private void handleAp04(String payload, String clientInfo, String imei) {
        if (payload == null || payload.isEmpty()) return;
        // 不再打印DEBUG级别日志，保持控制台简洁
        // log.debug("AP04 来自 {} 的 payload: {}");
        String[] parts = payload.split(",");
        Map<String, String> params = new LinkedHashMap<>();
        params.put("battery", parts.length > 0 ? parts[0] : "");
        saveHeartbeatData(params, imei);
        // 不再打印INFO级别日志，保持控制台简洁
        // log.info("AP04 低电报警已处理: {}");
    }

    private void handleAp10(String payload, String clientInfo, String imei) {
        // 不再打印DEBUG级别日志，保持控制台简洁
        // log.debug("AP10 来自 {} 的 payload: {}");
        Map<String, String> params = new LinkedHashMap<>();
        params.put("raw", payload);
        saveLocationData(params, imei);
        // 不再打印INFO级别日志，保持控制台简洁
        // log.info("AP10 报警上报，原始内容: {}");
    }

    private void handleAp42(String payload, String clientInfo, String imei) {
        // 不再打印DEBUG级别日志，保持控制台简洁
        // log.debug("AP42 来自 {} 的 payload: {}");
        String[] p = payload.split(",", 5);
        // reference imei to avoid unused parameter warning and help tracing
        if (imei != null && !imei.isEmpty()) {
            // 不再打印DEBUG级别日志，保持控制台简洁
            // log.debug("AP42 associated imei={}");
        }
        String time = p.length > 0 ? p[0] : "";
        String total = p.length > 1 ? p[1] : "";
        String seq = p.length > 2 ? p[2] : "";
        // 不再打印INFO级别日志，保持控制台简洁
        // log.info("AP42 图片包 time={} total={} seq={}");
    }

    private void handleApBl(String payload, String clientInfo, String imei) {
        // 不再打印DEBUG级别日志，保持控制台简洁
        // log.debug("APBL 来自 {} 的 payload: {}");
        Map<String, String> params = new LinkedHashMap<>();
        params.put("raw", payload);
        if (imei != null && !imei.isEmpty()) params.put("imei", imei);
        // 不再打印INFO级别日志，保持控制台简洁
        // log.info("APBL 蓝牙数据: {}");
    }

    private void handleApJk(String payload, String clientInfo, String imei) {
        if (payload == null || payload.isEmpty()) return;
        // 不再打印DEBUG级别日志，保持控制台简洁
        // log.debug("APJK 来自 {} 的 payload: {}");

        // 期望形式为：timestamp,type,value  （value 可能含有 '|' 分隔多个子值）
        // 例如: 2021-05-29 13:00:00,1,69|120
        String[] parts = payload.split(",", 3);
        Map<String, String> params = new LinkedHashMap<>();
        if (parts.length >= 3) {
            String timeStr = parts[0].trim();
            String typeStr = parts[1].trim();
            String valStr = parts[2].trim();
            params.put("timestamp", timeStr);
            params.put("type", typeStr);
            params.put("raw_value", valStr);

            // 解析类型并把值拆解为更具体字段
            switch (typeStr) {
                case "1": // 血压: diastolic|systolic
                    params.put("data_type", "blood_pressure");
                    if (valStr.contains("|")) {
                        String[] vs = valStr.split("\\|", 2);
                        params.put("bp_diastolic", vs[0]);
                        params.put("bp_systolic", vs[1]);
                        params.put("value", vs[0] + "|" + vs[1]);
                    } else {
                        // 若没有分隔符，仍当作 raw 保存
                        params.put("value", valStr);
                    }
                    break;
                case "2": // 心率
                    params.put("data_type", "heart_rate");
                    params.put("value", valStr);
                    break;
                case "3": // 体温
                    params.put("data_type", "temperature");
                    params.put("value", valStr);
                    break;
                case "4": // 血氧
                    params.put("data_type", "spo2");
                    params.put("value", valStr);
                    break;
                default:
                    // 未知类型，回退为 raw
                    params.put("data_type", "unknown");
                    params.put("value", valStr);
            }
            // 将 imei 从 payload 或 raw 中尝试提取（若这个 payload 中包含 imei，虽然协议通常不在此处带 imei）
            if (imei != null) params.put("imei", imei);

            saveHealthData(params, imei);
            // 不再打印INFO级别日志，保持控制台简洁
            // log.info("APJK 健康数据已保存（结构化）: {}");
            return;
        }

        // 回退：如果不符合新的三段式格式，则尝试解析为 key=value 键值对（向后兼容）
        Map<String, String> kv = parseKeyValueParams(payload);
        if (imei != null) kv.put("imei", imei);
        saveHealthData(kv, imei);
        // 不再打印INFO级别日志，保持控制台简洁
        // log.info("APJK 健康数据已按 kv 解析并保存: {}");
    }

    // --------------------------- 辅助方法 ---------------------------

    /** 将类似 key=value,key2=value2 的简单字符串解析为 map；如果不是该格式则保存为 raw */
    private Map<String, String> parseKeyValueParams(String s) {
        Map<String, String> m = new LinkedHashMap<>();
        if (s == null || s.isEmpty()) return m;
        if (s.contains("=")) {
            String[] kvs = s.split(",");
            for (String kv : kvs) {
                String[] t = kv.split("=", 2);
                if (t.length == 2) m.put(t[0], t[1]);
            }
        } else {
            m.put("raw", s);
        }
        return m;
    }

    /** 把经纬度字符串 ddmm.mmmmN 或 dddmm.mmmmE 转成十进制度数（可能返回 null） */
    private Double parseLat(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.length() < 2) return null;
        char hemi = s.charAt(s.length() - 1);
        String body = s.substring(0, s.length() - 1);
        try {
            // 大多数设备采用 ddmm.mmmm 格式：前两位为度，剩余为分（含小数）
            if (body.length() < 4) return null;
            String degPart = body.substring(0, 2);
            String minPart = body.substring(2);
            double deg = Double.parseDouble(degPart);
            double min = Double.parseDouble(minPart);
            double val = deg + min / 60.0;
            if (hemi == 'S' || hemi == 's') val = -val;
            return val;
        } catch (Exception e) {
            return null;
        }
    }

    private Double parseLon(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.length() < 2) return null;
        char hemi = s.charAt(s.length() - 1);
        String body = s.substring(0, s.length() - 1);
        try {
            // longitude dddmm.mmmm -> deg 3 digits
            String degPart = body.substring(0, 3);
            String minPart = body.substring(3);
            double deg = Double.parseDouble(degPart);
            double min = Double.parseDouble(minPart);
            double val = deg + min / 60.0;
            if (hemi == 'W' || hemi == 'w') val = -val;
            return val;
        } catch (Exception e) {
            return null;
        }
    }

    // --------------------------- 改良的 createResponse（AP00 要求 UTC 时间） ---------------------------
    /** 创建回复给手环的数据包 */
    private String createResponse(BraceletPacket packet) {
        String responseProtocol = "B" + packet.getProtocol().substring(1);

        if ("AP00".equals(packet.getProtocol())) {
            // 按协议要求：返回 IWBP00,20150101125223,8,Asia/Shanghai#
            // 其中时间为 UTC 0 时区时间（yyyyMMddHHmmss），第二项为服务器当前时区小时偏移
            DateTimeFormatter fmtUtc = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(java.time.ZoneOffset.UTC);
            String utc = fmtUtc.format(java.time.Instant.now());

            int tzHours = ZoneId.systemDefault().getRules().getOffset(java.time.Instant.now()).getTotalSeconds() / 3600;
            String zoneId = ZoneId.systemDefault().getId();
            if(responseKey != null && !responseKey.isEmpty()) {
                zoneId += "," + responseKey;
            }
            return HEADER + responseProtocol + "," + utc + "," + tzHours + "," + zoneId + END_MARKER;
        }

        // 其它包统一返回带状态或空的 BPxx（简单实现）
        return switch (packet.getProtocol()) {
            case "AP01" -> "IWBP01#";
            case "AP03" -> HEADER + "BP03" + END_MARKER;
            case "AP04" -> HEADER + "BP04" + END_MARKER;
            case "AP10" -> HEADER + "BP10" + END_MARKER;
            case "AP42" -> HEADER + "BP42" + END_MARKER;
            case "APBL" -> HEADER + "BPBL" + END_MARKER;
            case "APJK" -> HEADER + "BPJK" + END_MARKER;
            case "APTP" -> HEADER + "BPTP" + END_MARKER;
            case "APVR" -> HEADER + "BPVR" + END_MARKER;
            case "APWR" -> HEADER + "BPWR" + END_MARKER;
            default -> HEADER + responseProtocol + END_MARKER;
        };
    }

    // --------------------------- 新增辅助实现 ---------------------------

    /**
     * 将响应写回设备。使用 BufferedOutputStream 保证字符编码，并按配置决定是否追加 CRLF。
     * 注意：很多设备只需要 '#' 作为结束，但有些设备要求同时收到 CRLF 才处理，因此提供配置控制。
     */
    private void writeFrame(BufferedOutputStream out, String s, Socket clientSocket, String clientInfo) throws IOException {
        if (out == null || s == null) return;
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        out.write(bytes);
        if (appendNewlineAfterResponse) {
            out.write('\r');
            out.write('\n');
        }
        out.flush();
        
        // 记录服务器回复信息到日志文件
        String imei = null;
        if (clientSocket != null) {
            try {
                imei = downlinkManager.getImeiBySocket(clientSocket);
            } catch (Exception ex) {
                // 忽略异常
            }
        }
        
        // 记录发送的响应
        log.debug("📤 发送回复: {} (newlineAppended={})", s, appendNewlineAfterResponse);
        
        // 将回复信息追加到设备日志文件
        try {
            saveResponseLog(s, imei, clientInfo);
        } catch (Exception ex) {
            log.warn("💾 保存回复日志失败: {}", ex.getMessage());
        }
    }

    /**
     * 保存定位数据到数据库（AP01 / AP10 的简化保存）。
     * 现在接受 imei，若 imei 不为空则尝试关联 Device（若不存在则创建）。
     */
    private void saveLocationData(Map<String, String> params, String imei) {
        try {
            LocationRecord rec = new LocationRecord();
            // 解析常用字段
            // 优先使用显式的 lat/lon 字段，如果不存在则回退到 wifiGeoLat/wifiGeoLon
            Double latVal = null;
            Double lonVal = null;
            if (params.containsKey("lat")) latVal = parseDoubleSafely(params.get("lat"));
            else if (params.containsKey("wifiGeoLat")) latVal = parseDoubleSafely(params.get("wifiGeoLat"));
            if (params.containsKey("lon")) lonVal = parseDoubleSafely(params.get("lon"));
            else if (params.containsKey("wifiGeoLon")) lonVal = parseDoubleSafely(params.get("wifiGeoLon"));
            if (latVal != null) rec.setLatitude(latVal);
            if (lonVal != null) rec.setLongitude(lonVal);

            // 若 GPS 坐标为占位 (0 或 null) 且存在 wifiGeoLat/wifiGeoLon，则覆盖
            boolean gpsPlaceholder = (latVal == null || lonVal == null || (latVal != null && latVal.doubleValue() == 0.0) || (lonVal != null && lonVal.doubleValue() == 0.0));
            if (gpsPlaceholder && params.containsKey("wifiGeoLat") && params.containsKey("wifiGeoLon")) {
                Double wlat = parseDoubleSafely(params.get("wifiGeoLat"));
                Double wlon = parseDoubleSafely(params.get("wifiGeoLon"));
                if (wlat != null && wlon != null) {
                    rec.setLatitude(wlat);
                    rec.setLongitude(wlon);
                    params.put("locationSource", "wifi");
                }
            }

            if (!hasValidCoordinate(rec.getLatitude(), rec.getLongitude())
                    && params.containsKey("mcc")
                    && params.containsKey("mnc")
                    && params.containsKey("lac")
                    && params.containsKey("cid")) {
                Map<String, Double> lbsLocation = amapLocationService.locateByCell(
                        imei,
                        params.get("mcc"),
                        params.get("mnc"),
                        params.get("lac"),
                        params.get("cid"),
                        params.get("gsm"));
                if (lbsLocation != null) {
                    rec.setLatitude(lbsLocation.get("lat"));
                    rec.setLongitude(lbsLocation.get("lng"));
                    params.put("locationSource", "lbs");
                }
            }
            
            // 保存速度和方向
            if (params.containsKey("speed")) {
                try {
                    rec.setSpeed(Double.parseDouble(params.get("speed")));
                } catch (NumberFormatException e) {
                    // 如果解析失败，尝试保存为null
                    rec.setSpeed(null);
                    log.debug("无法解析speed参数: {}", params.get("speed"));
                }
            }
            if (params.containsKey("direction")) {
                try {
                    rec.setDirection(Double.parseDouble(params.get("direction")));
                } catch (NumberFormatException e) {
                    // 如果解析失败，尝试保存为null
                    rec.setDirection(null);
                    log.debug("无法解析direction参数: {}", params.get("direction"));
                }
            }
            rec.setBatteryLevel(parseIntegerParam(params, "battery_level", "batteryLevel", "battery"));
            
            // 获取地址信息
            if (rec.getLatitude() != null && rec.getLongitude() != null) {
                String address = amapLocationService.regeoAddress(rec.getLatitude(), rec.getLongitude());
                if (address != null) {
                    rec.setAddress(address);
                }
            }
            
            // 保存地址信息（如果参数中已有则优先使用）
            if (params.containsKey("address")) rec.setAddress(params.get("address"));
            
            // 保存定位源
            if (params.containsKey("locationSource")) rec.setSource(params.get("locationSource"));
            else if (params.containsKey("source")) rec.setSource(params.get("source"));
            
            // 保存原始GPS数据
            if (params.containsKey("gps_raw")) {
                rec.setGpsRaw(params.get("gps_raw"));
            } else if (params.containsKey("rawGpsPart")) {
                rec.setGpsRaw(params.get("rawGpsPart"));
            }
            
            // 保存额外原始数据
            if (params.containsKey("extra_raw")) {
                rec.setExtraRaw(params.get("extra_raw"));
            } else if (params.containsKey("rawExtraPart")) {
                rec.setExtraRaw(params.get("rawExtraPart"));
            } else {
                rec.setExtraRaw(params.toString());
            }

            // 试图关联 imei
            if (imei != null && !imei.isEmpty()) {
                rec.setImei(imei);
                Device d = findOrCreateDeviceByImei(imei);
                if (d != null) {
                    rec.setDevice(d);
                    trackingService.processLocationRecord(rec);
                }
            }

            locationRecordRepository.save(rec);
            log.debug("✅ 成功保存定位数据: IMEI={}, 位置=({}, {})");
        } catch (Exception e) {
            log.error("❌ 保存定位数据失败: IMEI={}, 错误={}", imei, e.getMessage());
        }
    }

    /** 将字符串安全转换为 Double（返回 null 表示转换失败或为空） */
    private Double parseDoubleSafely(String s) {
        if (s == null) return null;
        try { return Double.parseDouble(s); } catch (Exception e) { return null; }
    }

    private Integer parseIntegerParam(Map<String, String> params, String... keys) {
        if (params == null || keys == null) return null;
        for (String key : keys) {
            String value = params.get(key);
            if (value == null || value.isBlank()) continue;
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException ignored) {
                log.warn("Invalid integer value for {}: {}", key, value);
            }
        }
        return null;
    }

    private boolean hasValidCoordinate(Double lat, Double lng) {
        return lat != null && lng != null
                && lat >= -90 && lat <= 90
                && lng >= -180 && lng <= 180
                && !(Double.compare(lat, 0.0) == 0 && Double.compare(lng, 0.0) == 0);
    }

    /**
     * 保存心跳数据到数据库（AP03 / AP04）。
     * 接收 imei 并关联设备。
     */
    private void saveHeartbeatData(Map<String, String> params, String imei) {
        try {
            HeartbeatRecord rec = new HeartbeatRecord();
            // 保存状态块
            rec.setStatusBlock(params.getOrDefault("param_block", 
                              params.getOrDefault("status_block", 
                              params.getOrDefault("statusBlock", null))));
            // 保存步数/计数器
            rec.setCounter(params.getOrDefault("steps", 
                          params.getOrDefault("counter", null)));
            // 保存滚动计数
            rec.setRollCount(params.getOrDefault("roll_count", 
                           params.getOrDefault("rollCount", null)));
            // 保存工作模式
            rec.setWorkMode(params.getOrDefault("work_mode", 
                           params.getOrDefault("workMode", null)));
            // 保存间隔秒数
            if (params.containsKey("interval")) {
                try { rec.setIntervalSeconds(Integer.parseInt(params.get("interval"))); } catch (Exception ignored) {}
            }
            if (params.containsKey("interval_seconds")) {
                try { rec.setIntervalSeconds(Integer.parseInt(params.get("interval_seconds"))); } catch (Exception ignored) {}
            }
            // 保存原始负载
            rec.setRawPayload(params.toString());

            // 关联设备
            if (imei != null && !imei.isEmpty()) {
                rec.setImei(imei);
                Device d = findOrCreateDeviceByImei(imei);
                if (d != null) rec.setDevice(d);
            }

            heartbeatRecordRepository.save(rec);
            log.debug("✅ 成功保存心跳数据: IMEI={}", imei);
        } catch (Exception e) {
            log.error("❌ 保存心跳数据失败: IMEI={}, 错误={}", imei, e.getMessage());
        }
    }

    /**
     * 保存健康数据到数据库（APJK / APTP 等）。
     * 接收 imei 并关联设备。
     */
    private void saveHealthData(Map<String, String> params, String imei) {
        try {
            HealthRecord rec = new HealthRecord();
            // 保存数据类型和值
            if (params.containsKey("temp")) {
                rec.setDataType("temperature");
                rec.setValue(params.get("temp"));
            } else if (params.containsKey("data_type")) {
                rec.setDataType(params.get("data_type"));
                rec.setValue(params.getOrDefault("value", params.toString()));
            } else if (params.containsKey("wrist_temp")) {
                rec.setDataType("temperature");
                rec.setValue(params.get("wrist_temp"));
            } else {
                rec.setDataType("unknown");
                rec.setValue(params.toString());
            }

            // 保存时间戳
            if (params.containsKey("timestamp")) {
                try {
                    java.time.format.DateTimeFormatter df = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(params.get("timestamp"), df);
                    java.time.Instant inst = ldt.toInstant(java.time.ZoneOffset.UTC);
                    rec.setRecvTime(Date.from(inst));
                } catch (Exception ignored) {
                    // 尝试解析其他时间格式
                    try {
                        java.time.format.DateTimeFormatter df2 = java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
                        java.time.LocalDateTime ldt2 = java.time.LocalDateTime.parse(params.get("timestamp"), df2);
                        java.time.Instant inst2 = ldt2.toInstant(java.time.ZoneOffset.UTC);
                        rec.setRecvTime(Date.from(inst2));
                    } catch (Exception ignored2) {}
                }
            }

            // 关联设备
            if (imei != null && !imei.isEmpty()) {
                rec.setImei(imei);
                Device d = findOrCreateDeviceByImei(imei);
                if (d != null) rec.setDeviceId(d.getId());
            }
            
            // 保存额外信息
            rec.setRawData(params.toString());
            
            healthRecordRepository.save(rec);
            log.debug("✅ 成功保存健康数据: IMEI={}, 类型={}", imei, rec.getDataType());
        } catch (Exception e) {
            log.error("❌ 保存健康数据失败: IMEI={}, 错误={}", imei, e.getMessage());
        }
    }

    /**
     * 根据 imei 查找设备，若不存在则创建一个最小信息的 Device 并保存。
     * 这样可以保证后续的记录（定位/心跳/健康）都有 Device 外键关联
     */
    private Device findOrCreateDeviceByImei(String imei) {
        if (imei == null || imei.isEmpty()) return null;
        try {
            Optional<Device> opt = deviceRepository.findByImei(imei);
            if (opt.isPresent()) {
                log.debug("✅ 设备已存在: imei={}", imei);
                return opt.get();
            }
            // 设备不存在，创建新设备
            Device d = new Device();
            d.setImei(imei);
            d.setCreatedAt(new Date());
            deviceRepository.save(d);
            log.info("📱 自动创建设备记录: imei={}", imei);
            return d;
        } catch (Exception e) {
            log.error("❌ 查找或创建 Device 失败: imei={}, 错误={}", imei, e.getMessage());
            return null;
        }
    }

    /**
     * 关闭 ServerSocket 的安全方法（忽略异常）。
     */
    private void closeQuietly(ServerSocket s) {
        if (s == null) return;
        try { s.close(); } catch (IOException ignored) {}
    }

    /**
     * 获取当前绑定的端口号（若未启动返回配置端口）。
     */
    private int getBoundPort() {
        if (serverSocket != null && serverSocket.isBound()) return serverSocket.getLocalPort();
        return port;
    }

    @Override
    public synchronized void stop() {
        if (!running) return;
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {}
        if (clientPool != null) {
            clientPool.shutdownNow();
            try {
                boolean term = clientPool.awaitTermination(2, TimeUnit.SECONDS);
                if (!term) log.warn("clientPool 未能在规定时间内终止");
            } catch (InterruptedException ignored) {}
        }
        log.info("MpbandServer stopped");
    }

    @Override
    public boolean isRunning() { return running; }

    @Override
    public int getPhase() { return 0; }

    @Override
    public boolean isAutoStartup() { return true; }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    private String getImeiFromRaw(String raw) {
        if (raw == null) return null;
        
        log.debug("📌 正在从原始报文提取IMEI: {}", raw);
        
        // 提取协议类型
        String protocol = "";
        if (raw.startsWith("IW") && raw.length() >= 6) {
            protocol = raw.substring(2, 6);
        }
        
        // 对于AP03协议，完全跳过IMEI提取，因为AP03报文本身不包含IMEI信息
        if ("AP03".equals(protocol)) {
            log.debug("📌 识别到AP03协议，跳过IMEI提取");
            return null;
        }
        
        // 特别处理IWAP03报文，确保不会提取出无效IMEI
        if (raw.contains("IWAP03")) {
            log.debug("📌 识别到包含IWAP03的报文，跳过IMEI提取");
            return null;
        }
        
        // 去掉开头 'IW' + 协议号 'APxx' (共6位) 后再查找 15 位数字
        String s = raw;
        if (s.startsWith("IW") && s.length() > 6) {
            int end = s.endsWith(END_MARKER) ? s.length() - 1 : s.length();
            s = s.substring(6, end);
        }
        
        Matcher m = IMEI_PATTERN.matcher(s);
        if (m.find()) {
            String imei = m.group(0);
            
            // 额外检查：确保提取的IMEI是有效的
            if (!isValidImei(imei)) {
                log.debug("📌 识别到无效IMEI值{}，跳过", imei);
                return null;
            }
            
            log.debug("📌 提取到有效IMEI: {}", imei);
            return imei;
        }
        
        log.debug("📌 未找到IMEI");
        return null;
    }
    
    /**
     * 验证IMEI是否有效
     * @param imei 要验证的IMEI
     * @return true if valid, false otherwise
     */
    private boolean isValidImei(String imei) {
        if (imei == null || imei.length() != 15) {
            return false;
        }
        
        // 排除已知的无效IMEI
        if ("05700008100008".equals(imei) || "000570001000000".equals(imei)) {
            return false;
        }
        
        // 排除全是0的IMEI
        if (imei.matches("^0+$")) {
            return false;
        }
        
        // 排除以000开头的IMEI，这些看起来像是无效的测试值
        if (imei.startsWith("000")) {
            return false;
        }
        
        // 可以添加Luhn算法验证，这里暂时省略
        return true;
    }

    /**
     * 保存原始报文到设备日志文件（按 IMEI）
     * 设备日志追加到: {saveBasePath}/devices/{imei}_yyyyMMdd.log
     */
    private void saveRawAndDeviceLog(String rawMessage, String imei, String clientInfo) {
        // 只保留设备号加日期的日志，IMEI为空时不生成日志
        if (imei == null || imei.isEmpty()) {
            // IMEI为空时只记录到系统日志
            log.debug("📨 收到原始数据但IMEI为空: {} [{}]", rawMessage, clientInfo);
            return;
        }

        // 追加到设备日志
        try {
            String safe = sanitizeFilename(imei);
            String day = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneId.systemDefault()).format(java.time.Instant.now());
            // 设备日志文件名格式：{imei}_yyyyMMdd.log，每个设备每天一个文件
            String deviceLogFileName = String.format("%s_%s.log", safe, day);
            Path deviceLog = Paths.get(System.getProperty("user.dir")).resolve(deviceLogFileName);

            String time = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault()).format(java.time.Instant.now());
            String entry = String.format("%s [%s] %s%n", time, clientInfo == null ? "-" : clientInfo, rawMessage);

            // 简单的同步写入
            synchronized (this) {
                Files.write(deviceLog, entry.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
        } catch (Exception e) {
            log.warn("💾 保存设备日志失败: {}", e.getMessage());
        }
    }

    /** 简单文件名清洗，移除非法字符 */
    private String sanitizeFilename(String in) {
        if (in == null) return "unknown";
        return in.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
    
    /**
     * 保存服务器回复信息到设备日志文件
     * 格式：{时间} [客户端信息] [SENT] {回复内容}
     */
    private void saveResponseLog(String response, String imei, String clientInfo) {
        // 只保留设备号加日期的日志，IMEI为空时不生成日志
        if (imei == null || imei.isEmpty()) {
            // IMEI为空时只记录到系统日志
            log.debug("📤 发送回复但IMEI为空: {} [{}]", response, clientInfo);
            return;
        }

        // 追加到设备日志
        try {
            String safe = sanitizeFilename(imei);
            String day = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneId.systemDefault()).format(java.time.Instant.now());
            // 设备日志文件名格式：{imei}_yyyyMMdd.log，每个设备每天一个文件
            String deviceLogFileName = String.format("%s_%s.log", safe, day);
            Path deviceLog = Paths.get(System.getProperty("user.dir")).resolve(deviceLogFileName);

            String time = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault()).format(java.time.Instant.now());
            // 使用 [SENT] 标记这是服务器发送的回复
            String entry = String.format("%s [%s] [SENT] %s%n", time, clientInfo == null ? "-" : clientInfo, response);

            // 简单的同步写入
            synchronized (this) {
                Files.write(deviceLog, entry.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
        } catch (Exception e) {
            log.warn("💾 保存回复日志失败: {}", e.getMessage());
        }
    }

    private void handleApTp(String payload, String clientInfo, String imei) {
        // APTP：体温包，格式示例： temp,wrist_temp  或单个值
        if (payload == null || payload.isEmpty()) return;
        // 不再打印DEBUG级别日志，保持控制台简洁
        // log.debug("APTP 来自 {} 的 payload: {}");
        String[] p = payload.split(",");
        Map<String, String> params = new LinkedHashMap<>();
        if (p.length >= 1 && p[0] != null && !p[0].isEmpty()) params.put("temp", p[0]);
        if (p.length >= 2 && p[1] != null && !p[1].isEmpty()) params.put("wrist_temp", p[1]);
        // 保存为健康数据（saveHealthData 会识别 temp 字段并标记为 temperature）
        saveHealthData(params, imei);
        // 不再打印INFO级别日志，保持控制台简洁
        // log.info("APTP 体温数据已保存: {}");
    }

    private void handleApVr(String payload, String clientInfo, String imei) {
        // APVR：版本信息，示例： imei,firmware  或单个 firmware
        if (payload == null || payload.isEmpty()) return;
        // 不再打印DEBUG级别日志，保持控制台简洁
        // log.debug("APVR 来自 {} 的 payload: {}");
        String[] p = payload.split(",", 2);
        Map<String, String> params = new LinkedHashMap<>();
        if (p.length >= 2) {
            params.put("imei", p[0]);
            params.put("firmware", p[1]);
        } else {
            params.put("firmware", payload);
        }
        // 将版本信息作为额外的健康/状态记录保存，以便审计（也可扩展为专门表）
        params.put("data_type", "firmware_info");
        params.put("value", params.getOrDefault("firmware", payload));
        // 如果上层传入了 imei，则优先使用上层 imei 作为设备关联；否则尝试从内容里取
        if (imei == null || imei.isEmpty()) {
            String pImei = params.get("imei");
            if (pImei != null) imei = pImei;
        }
        saveHealthData(params, imei);
        // 不再打印INFO级别日志，保持控制台简洁
        // log.info("APVR 版本信息已保存: {}");
    }

    private void handleApWr(String payload, String clientInfo, String imei) {
        // APWR：佩戴状态，示例： imei,wear_flag,timestamp
        if (payload == null || payload.isEmpty()) return;
        // 不再打印DEBUG级别日志，保持控制台简洁
        // log.debug("APWR 来自 {} 的 payload: {}");
        String[] p = payload.split(",", 3);
        Map<String, String> params = new LinkedHashMap<>();
        if (p.length >= 1) params.put("imei", p[0]);
        if (p.length >= 2) params.put("wear_flag", p[1]);
        if (p.length >= 3) params.put("timestamp", p[2]);
        // 优先使用外部传入的 imei（如果有），否则使用 payload 中的 imei 字段
        if ((imei == null || imei.isEmpty()) && params.containsKey("imei")) {
            imei = params.get("imei");
        }
        // 把佩戴状态作为心跳/状态保存
        saveHeartbeatData(params, imei);
        // 不再打印INFO级别日志，保持控制台简洁
        // log.info("APWR 佩戴状态已保存: {}");
    }
}
