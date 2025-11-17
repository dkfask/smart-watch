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
import com.example.demo.repository.DeviceRepository;
import com.example.demo.repository.LocationRecordRepository;
import com.example.demo.repository.HeartbeatRecordRepository;
import com.example.demo.repository.HealthRecordRepository;
import com.example.demo.socket.protocol.BraceletPacket;
import com.example.demo.socket.protocol.ProtocolParser;
import com.example.demo.socket.downlink.DownlinkManager;

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

    @Value("${app.mpband.maxFrameLength:4096}")
    private int maxFrameLength;

    // 新增：保存目录（相对于运行目录 user.dir）
    @Value("${app.mpband.saveDir:mpband_data}")
    private String saveDirName;

    // 注入依赖
    private final DeviceRepository deviceRepository;
    private final DownlinkManager downlinkManager;
    private final LocationRecordRepository locationRecordRepository;
    private final HeartbeatRecordRepository heartbeatRecordRepository;
    private final HealthRecordRepository healthRecordRepository;

    // 协议常量
    private static final String HEADER = "IW";
    private static final String END_MARKER = "#";

    private volatile boolean running = false;
    private ServerSocket serverSocket;
    private ExecutorService clientPool;
    private Thread acceptThread;

    // 新增：磁盘路径与并发锁
    private Path saveBasePath;
    private Path rawDirPath;
    private Path deviceDirPath;
    private final Map<String, Object> deviceLocks = new ConcurrentHashMap<>();
    // 新增：原始报文按天归档写入的全局锁，避免并发写入冲突
    private final Object rawFileLock = new Object();

    // 用于从 payload 中识别 IMEI（15 位数字）
    private static final Pattern IMEI_PATTERN = Pattern.compile("\\b(\\d{15})\\b");

    public MpbandServer(DeviceRepository deviceRepository,
                        DownlinkManager downlinkManager,
                        LocationRecordRepository locationRecordRepository,
                        HeartbeatRecordRepository heartbeatRecordRepository,
                        HealthRecordRepository healthRecordRepository) {
        this.deviceRepository = deviceRepository;
        this.downlinkManager = downlinkManager;
        this.locationRecordRepository = locationRecordRepository;
        this.heartbeatRecordRepository = heartbeatRecordRepository;
        this.healthRecordRepository = healthRecordRepository;
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

            // 启动时确保磁盘保存目录存在
            try {
                saveBasePath = Paths.get(System.getProperty("user.dir")).resolve(saveDirName);
                rawDirPath = saveBasePath.resolve("raw");
                deviceDirPath = saveBasePath.resolve("devices");
                Files.createDirectories(rawDirPath);
                Files.createDirectories(deviceDirPath);
                log.info("📁 数据保存目录准备就绪: {}", saveBasePath.toAbsolutePath());
            } catch (Exception ex) {
                log.warn("无法创建保存目录 {}: {}", saveDirName, ex.getMessage());
            }

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
        String clientInfo = clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort();
        BufferedReader reader = null;
        BufferedWriter writer = null;
        InputStream rawIn;
        try {
            // 设置底层 Socket 选项，增加连接稳定性
            try {
                clientSocket.setKeepAlive(true);
                clientSocket.setSoTimeout(soTimeoutMillis);
                clientSocket.setTcpNoDelay(true);
            } catch (Exception e) {
                log.warn("无法设置 Socket 选项: {}", e.getMessage());
            }

            reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
            writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8));
            rawIn = clientSocket.getInputStream();

            log.info("🔄 开始处理手环: {} (useDelimiterReader={})", clientInfo, useDelimiterReader);

            if (useDelimiterReader) {
                // 采用分隔符读取模式：按字符读取直到遇到分隔符（默认 '#')，不要求换行
                readFramesLoop(rawIn, writer, clientSocket, clientInfo);
            } else {
                // 回退：兼容旧逻辑（设备发送 '\\r\\n'）：按行读取
                String message;
                while ((message = reader.readLine()) != null) {
                    handleOneMessage(message, writer, clientSocket, clientInfo);
                }
            }
        } catch (SocketTimeoutException ste) {
            log.warn("读取超时，断开连接 client={}", clientInfo);
        } catch (IOException e) {
            log.info("🔌 手环断开连接: {} - {}", clientInfo, e.getMessage());
        } finally {
            // 断开时注销下行管理器中的映射
            try { downlinkManager.unregisterBySocket(clientSocket); } catch (Exception ignored) {}
            try { if (reader != null) reader.close(); } catch (IOException ignored) {}
            try { if (writer != null) writer.close(); } catch (IOException ignored) {}
            try { if (!clientSocket.isClosed()) clientSocket.close(); } catch (IOException ignored) {}
        }
    }

    /**
     * 新增：分隔符读取主循环。读取字节直到遇到 frameDelimiter（默认 '#')，将之前累积的内容（含包头）作为一个完整上行报文。
     * 说明：
     * 1. 很多手环/定位终端协议使用 '#' 作为结束符，但不保证追加换行；旧实现依赖 readLine() 可能导致阻塞，从而设备端等待 ACK 超时后主动重连 -> 形成“反复连接”现象。
     * 2. 这里采用逐字节累积 + 分隔符判断，避免阻塞在 readLine，同时也支持设备如果仍然发送换行不会出错（换行会被当作普通字符保留或过滤——此处简单过滤）。
     */
    private void readFramesLoop(InputStream in, BufferedWriter writer, Socket clientSocket, String clientInfo) throws IOException {
        String delim = (frameDelimiter == null || frameDelimiter.isEmpty()) ? "#" : frameDelimiter;
        char endChar = delim.charAt(0); // 当前仅支持单字符分隔符

        StringBuilder frame = new StringBuilder();
        byte[] buf = new byte[512];
        int len;
        while ((len = in.read(buf)) != -1) {
            for (int i = 0; i < len; i++) {
                char c = (char) (buf[i] & 0xFF);
                // 可选：过滤回车/换行，避免混入 frame
                if (c == '\r' || c == '\n') continue;
                frame.append(c);
                if (frame.length() > maxFrameLength) {
                    log.warn("帧长度超过限制({})，丢弃当前缓冲，防御性清空。", maxFrameLength);
                    frame.setLength(0);
                    continue;
                }
                if (c == endChar) {
                    // 完整帧（已包含结束符）
                    String rawMessage = frame.toString();
                    frame.setLength(0); // 准备下一个
                    // 与原逻辑保持一致：传给 handleOneMessage 需要不含换行但可以包含 '#'
                    handleOneMessage(rawMessage, writer, clientSocket, clientInfo);
                }
            }
        }
    }

    /**
     * 统一处理单条上行报文（无论来源是行模式还是分隔符模式）。
     * @param rawMessage 原始报文，应该以 '#' 结束（若设备未按协议发送，则可能解析失败）。
     */
    private void handleOneMessage(String rawMessage, BufferedWriter writer, Socket clientSocket, String clientInfo) throws IOException {
        if (rawMessage == null) return;
        String message = rawMessage.trim();
        log.info("📨 收到原始数据: {}", message);

        // 保存原始报文到磁盘并追加到设备日志（尽量提取 IMEI）
        try {
            String imei = getImeiFromRaw(message);
            saveRawAndDeviceLog(message, imei, clientInfo);
        } catch (Exception ex) {
            log.warn("保存上行报文到磁盘失败: {}", ex.getMessage());
        }

        // 使用协议解析器（保留），同时把原始报文也传入以便按协议做更精确的解析
        BraceletPacket packet = ProtocolParser.parse(message);
        if (packet == null) {
            log.warn("❌ 数据包格式错误(无法解析) raw={}", message);
            return;
        }

        // 处理（传入原始消息以便自行解析字段）
        processPacket(packet, message, clientInfo, clientSocket);

        // 构造并发送回复
        String response = createResponse(packet);
        writeFrame(writer, response);
        log.info("📤 发送回复: {} (newlineAppended={})", response, appendNewlineAfterResponse);
    }

    /**
     * 处理解析后的数据包（重构版）
     * @param packet 已由 ProtocolParser 粗略解析的包对象（包含 protocol）
     * @param raw 原始报文字符串（含头尾符号），格式如 IWAP00...#
     */
    private void processPacket(BraceletPacket packet, String raw, String clientInfo, Socket clientSocket) {
        String proto = packet.getProtocol();
        // 解析统一从 raw 中截取 payload（header+proto 长度为 2 + 4 = 6）
        String payload = "";
        if (raw != null && raw.length() > 6) {
            // 去掉开头 IWAPxx 并去掉尾部的 '#'
            int end = raw.endsWith(END_MARKER) ? raw.length() - 1 : raw.length();
            payload = raw.substring(6, end);
        }

        switch (proto) {
            case "AP00":
                // 登录包：IWAP00+IMEI# 或 IWAP00+IMEI,MCC|MNC|APN# 或 IWAP00+IMEI,ICCID,IMSI#
                handleAp00(payload, clientSocket, clientInfo);
                break;
            case "AP01":
                // 定位包
                handleAp01(payload, clientInfo);
                break;
            case "AP02":
                // 健康包（旧版 AP02 可能另用，此处如无明确需求可当作健康数据）
                saveHealthData(parseKeyValueParams(payload));
                break;
            case "AP03":
                handleAp03(payload, clientInfo);
                break;
            case "AP04":
                handleAp04(payload, clientInfo);
                break;
            case "AP10":
                handleAp10(payload, clientInfo);
                break;
            case "AP42":
                handleAp42(payload, clientInfo);
                break;
            case "APBL":
                handleApBl(payload, clientInfo);
                break;
            case "APJK":
                handleApJk(payload, clientInfo);
                break;
            case "APTP":
                handleApTp(payload, clientInfo);
                break;
            case "APVR":
                handleApVr(payload, clientInfo);
                break;
            case "APWR":
                handleApWr(payload, clientInfo);
                break;
            default:
                log.info("❓ 未知协议类型: {} raw={}", proto, payload);
        }
    }

    // --------------------------- 各协议处理函数 ---------------------------

    /**
     * 处理 AP00 登录包：根据三种格式解析并保存设备信息
     * payload 示例：
     *  - 353456789012345
     *  - 353456789012345,460|00|CMNET
     *  - 357653050858997,89962030221137165263,416032113716526
     */
    private void handleAp00(String payload, Socket clientSocket, String clientInfo) {
        if (payload == null || payload.isEmpty()) return;
        String[] parts = payload.split(",");
        String imei = parts[0].trim();
        if (imei.length() != 15 || !imei.chars().allMatch(Character::isDigit)) {
            log.warn("AP00: 无效 IMEI: {}", imei);
            return;
        }

        // 注册下行连接
        try {
            downlinkManager.register(imei, clientSocket);
        } catch (Exception e) {
            log.warn("注册下行连接失败 IMEI={}", imei, e);
        }

        // 保存设备信息（若不存在）
        Optional<Device> opt = deviceRepository.findByImei(imei);
        if (opt.isPresent()) {
            log.info("设备已存在 IMEI={}", imei);
            return;
        }

        Device device = new Device();
        device.setImei(imei);
        device.setCreatedAt(new Date());

        try {
            if (parts.length >= 2) {
                String second = parts[1];
                if (second.contains("|")) {
                    // MCC|MNC|APN
                    String[] net = second.split("\\|", 3);
                    if (net.length >= 1) device.setMcc(net[0]);
                    if (net.length >= 2) device.setMnc(net[1]);
                    if (net.length >= 3) device.setApn(net[2]);
                } else if (parts.length >= 3) {
                    // ICCID, IMSI
                    device.setIccid(parts[1].trim());
                    device.setImsi(parts[2].trim());
                }
            }
            deviceRepository.save(device);
            log.info("💾 新设备已保存 IMEI={} 来自 {}", imei, clientInfo);
        } catch (Exception e) {
            log.error("保存设备失败 IMEI={}", imei, e);
        }
    }

    /**
     * 处理 AP01 定位包的简化解析与存储，尽量按协议文档抽取常用字段：time, valid, lat, lon, speed, gpsTime, direction, paramBlock, LBS(will be array), wifiRaw
     */
    private void handleAp01(String payload, String clientInfo) {
        if (payload == null || payload.isEmpty()) return;
        // 记录来源以避免未使用参数的静态分析警告，同时便于调试
        log.debug("AP01 来自 {} 的 payload: {}", clientInfo, payload);
        // 按逗号分段：第一个段包含大块 GPS 信息，其后一般依次为 MCC,MNC,LAC,CID,...,wifi
        String[] segments = payload.split(",", 6);
        String gpsBlock = segments.length > 0 ? segments[0] : "";

        Map<String, String> params = new LinkedHashMap<>();
        // 解析 gpsBlock，采用正则匹配常见格式
        // 格式示例：080524A2232.9806N11404.9355E000.1061830323.8706000908000102
        try {
            // 时间(6) + valid(1)
            if (gpsBlock.length() >= 7) {
                params.put("date", gpsBlock.substring(0, 6));
                params.put("valid", gpsBlock.substring(6, 7));
            }
            // 找纬度结束符 N/S
            int idx = 7;
            int latEnd = Math.max(gpsBlock.indexOf('N', idx), gpsBlock.indexOf('S', idx));
            if (latEnd > 0) {
                String latStr = gpsBlock.substring(7, latEnd + 1); // 包含 N/S
                params.put("lat_raw", latStr);
                Double lat = parseLat(latStr);
                if (lat != null) params.put("lat", String.valueOf(lat));
                idx = latEnd + 1;
            }
            int lonEnd = Math.max(gpsBlock.indexOf('E', idx), gpsBlock.indexOf('W', idx));
            if (lonEnd > 0) {
                String lonStr = gpsBlock.substring(idx, lonEnd + 1);
                params.put("lon_raw", lonStr);
                Double lon = parseLon(lonStr);
                if (lon != null) params.put("lon", String.valueOf(lon));
                idx = lonEnd + 1;
            }
            // 速度（格式如 000.1）
            String tail = gpsBlock.substring(idx);
            // tail 可能包含 speed + gpsTime + dir + paramBlock ；我们尝试用数字序列提取
            // 简单匹配：先尝试速度(包含小数点)
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("([0-9]{3}\\.[0-9]+)").matcher(tail);
            if (m.find()) {
                params.put("speed", m.group(1));
                int pos = m.end();
                String rest = tail.substring(pos);
                // gpsTime 6位
                if (rest.length() >= 6) {
                    params.put("gps_time", rest.substring(0, 6));
                    rest = rest.substring(6);
                    // direction 可为浮点
                    java.util.regex.Matcher m2 = java.util.regex.Pattern.compile("([0-9]{1,3}\\.[0-9]+)").matcher(rest);
                    if (m2.find()) {
                        params.put("direction", m2.group(1));
                        rest = rest.substring(m2.end());
                    }
                    params.put("param_block", rest);
                }
            } else {
                params.put("speed", "0");
            }
        } catch (Exception ex) {
            log.debug("解析 AP01 GPSBlock 失败: {}", ex.getMessage());
        }

        // 后续 segments[1..] 可能包含 LBS 和 wifi 等信息
        if (segments.length >= 2) {
            String lbsCsv = segments[1];
            params.put("lbs_raw", lbsCsv);
            String[] lbsParts = lbsCsv.split(",");
            if (lbsParts.length >= 4) {
                params.put("mcc", lbsParts[0]);
                params.put("mnc", lbsParts[1]);
                params.put("lac", lbsParts[2]);
                params.put("cid", lbsParts[3]);
            }
        }
        if (segments.length >= 6) {
            params.put("wifi_raw", segments[5]);
        } else if (segments.length >= 3) {
            params.put("wifi_raw", segments[segments.length - 1]);
        }

        // 保存到数据库（复用 saveLocationData 的结构，传入解析后的 params）
        saveLocationData(params);
        log.info("AP01 处理完成, 保存解析字段: {}", params.keySet());
    }

    /**
     * 解析 AP03 心跳包
     * 格式：IWAP03,06000908000102,5555,30# 或 IWAP03,06300706800008,0,00,8,600#
     */
    private void handleAp03(String payload, String clientInfo) {
        if (payload == null || payload.isEmpty()) return;
        log.debug("AP03 来自 {} 的 payload: {}", clientInfo, payload);
        String[] parts = payload.split(",");
        Map<String, String> params = new LinkedHashMap<>();
        params.put("param_block", parts.length > 0 ? parts[0] : "");
        if (parts.length > 1) params.put("steps", parts[1]);
        if (parts.length > 2) params.put("roll_count", parts[2]);
        if (parts.length > 3) params.put("work_mode", parts[3]);
        if (parts.length > 4) params.put("interval", parts[4]);

        saveHeartbeatData(params);
        log.info("AP03 心跳包已保存: {}", params);
    }

    /** AP04 低电量报警 */
    private void handleAp04(String payload, String clientInfo) {
        if (payload == null || payload.isEmpty()) return;
        log.debug("AP04 来自 {} 的 payload: {}", clientInfo, payload);
        String[] parts = payload.split(",");
        Map<String, String> params = new LinkedHashMap<>();
        params.put("battery", parts.length > 0 ? parts[0] : "");
        // 暂时把此信息存为健康数据的一部分或心跳数据的扩展
        saveHeartbeatData(params);
        log.info("AP04 低电报警已处理: {}", params);
    }

    /** AP10 报警与地址回复，保存原始并在需要时回复地址（此处仅保存与记录） */
    private void handleAp10(String payload, String clientInfo) {
        log.debug("AP10 来自 {} 的 payload: {}", clientInfo, payload);
        Map<String, String> params = new LinkedHashMap<>();
        params.put("raw", payload);
        // 若需要可进一步解析与存储报警信息
        log.info("AP10 报警上报，原始内容: {}", payload);
        // 这里只记录，不做复杂地址回复；保存为位置记录备用
        saveLocationData(params);
    }

    /** AP42 图片分包，payload 格式: time,totalCount,seq,len,data
     *  简单实现：记录接收并回复已成功（receiver flag=1）
     */
    private void handleAp42(String payload, String clientInfo) {
        log.debug("AP42 来自 {} 的 payload: {}", clientInfo, payload);
        String[] p = payload.split(",", 5);
        String time = p.length > 0 ? p[0] : "";
        String total = p.length > 1 ? p[1] : "";
        String seq = p.length > 2 ? p[2] : "";
        // 数据内容暂不持久化到数据库（可按需实现），这里只写日志
        log.info("AP42 图片包 time={} total={} seq={}", time, total, seq);
        // 平台需要回复 IWBP42,time,total,seq,1# 表示接收成功
        // 我们通过创建一个临时 BraceletPacket（或直接通过下行管理器发送）来返回，
        // 但当前框架的 createResponse 基于 packet.getProtocol()，因此这里仅记录日志；
        // 实际运行时设备会等待 BP42 响应，createResponse 会被调用产生 BP42（如果发起相应的 packet）
    }

    /** APBL 蓝牙数据 */
    private void handleApBl(String payload, String clientInfo) {
        log.debug("APBL 来自 {} 的 payload: {}", clientInfo, payload);
        Map<String, String> params = new LinkedHashMap<>();
        params.put("raw", payload);
        log.info("APBL 蓝牙数据: {}", payload);
        // 可持久化或保存到专门表
    }

    /** APJK 健康数据 */
    private void handleApJk(String payload, String clientInfo) {
        log.debug("APJK 来自 {} 的 payload: {}", clientInfo, payload);
        Map<String, String> params = parseKeyValueParams(payload);
        saveHealthData(params);
        log.info("APJK 健康数据已保存: {}", params);
    }

    /** APTP 体温 */
    private void handleApTp(String payload, String clientInfo) {
        log.debug("APTP 来自 {} 的 payload: {}", clientInfo, payload);
        Map<String, String> params = new LinkedHashMap<>();
        String[] p = payload.split(",");
        if (p.length >= 1) params.put("temp", p[0]);
        if (p.length >= 2) params.put("wrist_temp", p[1]);
        saveHealthData(params);
        log.info("APTP 体温已保存: {}", params);
    }

    /** APVR 版本 */
    private void handleApVr(String payload, String clientInfo) {
        log.debug("APVR 来自 {} 的 payload: {}", clientInfo, payload);
        Map<String, String> params = new LinkedHashMap<>();
        String[] p = payload.split(",", 2);
        if (p.length >= 2) {
            params.put("imei", p[0]);
            params.put("firmware", p[1]);
        } else params.put("raw", payload);
        log.info("APVR 版本信息: {}", params);
    }

    /** APWR 佩戴状态 */
    private void handleApWr(String payload, String clientInfo) {
        log.debug("APWR 来自 {} 的 payload: {}", clientInfo, payload);
        Map<String, String> params = new LinkedHashMap<>();
        String[] p = payload.split(",", 3);
        if (p.length >= 3) {
            params.put("imei", p[0]);
            params.put("wear_flag", p[1]);
            params.put("timestamp", p[2]);
        } else params.put("raw", payload);
        log.info("APWR 佩戴状态: {}", params);
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

            return HEADER + responseProtocol + "," + utc + "," + tzHours + "," + zoneId + END_MARKER;
        }

        // 其它包统一返回带状态或空的 BPxx（简单实现）
        switch (packet.getProtocol()) {
            case "AP01": return HEADER + "BP01" + END_MARKER;
            case "AP03": return HEADER + "BP03" + END_MARKER;
            case "AP04": return HEADER + "BP04" + END_MARKER;
            case "AP10": return HEADER + "BP10" + END_MARKER;
            case "AP42": return HEADER + "BP42" + END_MARKER;
            case "APBL": return HEADER + "BPBL" + END_MARKER;
            case "APJK": return HEADER + "BPJK" + END_MARKER;
            case "APTP": return HEADER + "BPTP" + END_MARKER;
            case "APVR": return HEADER + "BPVR" + END_MARKER;
            case "APWR": return HEADER + "BPWR" + END_MARKER;
            default: return HEADER + responseProtocol + END_MARKER;
        }
    }

    // --------------------------- 新增辅助实现 ---------------------------

    /**
     * 将响应写回设备。使用 BufferedWriter 保证字符编码，并按配置决定是否追加 CRLF。
     * 注意：很多设备只需要 '#' 作为结束，但有些设备要求同时收到 CRLF 才处理，因此提供配置控制。
     */
    private void writeFrame(BufferedWriter out, String s) throws IOException {
        if (out == null || s == null) return;
        out.write(s);
        if (appendNewlineAfterResponse) {
            out.write('\r');
            out.write('\n');
        }
        out.flush();
    }

    /**
     * 保存定位数据到数据库（AP01 / AP10 的简化保存）。
     * 该实现遵循简单映射：尝试从 params 中读取 lat/lon/speed/direction/gpsRaw/wifi_raw，若包含 imei 则关联设备。
     * 所有异常在本方法内部捕获，避免影响主循环。
     */
    private void saveLocationData(Map<String, String> params) {
        try {
            LocationRecord rec = new LocationRecord();
            // 解析常用字段
            if (params.containsKey("lat")) rec.setLatitude(parseDoubleSafely(params.get("lat")));
            if (params.containsKey("lon")) rec.setLongitude(parseDoubleSafely(params.get("lon")));
            if (params.containsKey("speed")) rec.setSpeed(params.get("speed"));
            if (params.containsKey("direction")) rec.setDirection(params.get("direction"));

            // 原始块：优先拼接 lat_raw / lon_raw / param_block，便于调试
            StringBuilder gpsRaw = new StringBuilder();
            if (params.containsKey("lat_raw")) gpsRaw.append(params.get("lat_raw"));
            if (params.containsKey("lon_raw")) {
                if (gpsRaw.length() > 0) gpsRaw.append(",");
                gpsRaw.append(params.get("lon_raw"));
            }
            if (params.containsKey("param_block")) {
                if (gpsRaw.length() > 0) gpsRaw.append(",");
                gpsRaw.append(params.get("param_block"));
            }
            if (!gpsRaw.isEmpty()) rec.setGpsRaw(gpsRaw.toString());

            // 额外原始内容
            rec.setExtraRaw(params.toString());

            // 试图关联 imei
            String imei = params.getOrDefault("imei", null);
            if (imei != null && !imei.isEmpty()) {
                rec.setImei(imei);
                try {
                    Optional<Device> d = deviceRepository.findByImei(imei);
                    d.ifPresent(rec::setDevice);
                } catch (Exception ignored) {}
            }

            locationRecordRepository.save(rec);
        } catch (Exception e) {
            log.warn("保存定位数据失败: {}", e.getMessage());
        }
    }

    /** 将字符串安全转换为 Double（返回 null 表示转换失败或为空） */
    private Double parseDoubleSafely(String s) {
        if (s == null) return null;
        try { return Double.parseDouble(s); } catch (Exception e) { return null; }
    }

    /**
     * 保存心跳数据到数据库（AP03 / AP04）。
     * 将 param_block 写入 statusBlock, steps 写入 counter, roll_count 写入 rollCount, interval 写入 intervalSeconds。
     */
    private void saveHeartbeatData(Map<String, String> params) {
        try {
            HeartbeatRecord rec = new HeartbeatRecord();
            rec.setStatusBlock(params.getOrDefault("param_block", params.getOrDefault("status_block", null)));
            rec.setCounter(params.getOrDefault("steps", params.getOrDefault("counter", null)));
            rec.setRollCount(params.getOrDefault("roll_count", null));
            rec.setWorkMode(params.getOrDefault("work_mode", null));
            if (params.containsKey("interval")) {
                try { rec.setIntervalSeconds(Integer.parseInt(params.get("interval"))); } catch (Exception ignored) {}
            }
            rec.setRawPayload(params.toString());

            String imei = params.getOrDefault("imei", null);
            if (imei != null && !imei.isEmpty()) {
                rec.setImei(imei);
                try { Optional<Device> d = deviceRepository.findByImei(imei); d.ifPresent(rec::setDevice); } catch (Exception ignored) {}
            }

            heartbeatRecordRepository.save(rec);
        } catch (Exception e) {
            log.warn("保存心跳数据失败: {}", e.getMessage());
        }
    }

    /**
     * 保存健康数据到数据库（APJK / APTP 等）。
     * dataType 使用 params 内常见键（如 temp 或 type），value 存储为整个 params.toString() 以便后续分析。
     */
    private void saveHealthData(Map<String, String> params) {
        try {
            HealthRecord rec = new HealthRecord();
            if (params.containsKey("temp")) {
                rec.setDataType("temperature");
                rec.setValue(params.get("temp"));
            } else if (params.containsKey("type")) {
                rec.setDataType(params.get("type"));
                rec.setValue(params.getOrDefault("value", params.toString()));
            } else {
                rec.setDataType("unknown");
                rec.setValue(params.toString());
            }

            String imei = params.getOrDefault("imei", null);
            if (imei != null && !imei.isEmpty()) {
                rec.setImei(imei);
                try { Optional<Device> d = deviceRepository.findByImei(imei); d.ifPresent(rec::setDevice); } catch (Exception ignored) {}
            }
            rec.setExtra(params.toString());
            healthRecordRepository.save(rec);
        } catch (Exception e) {
            log.warn("保存健康数据失败: {}", e.getMessage());
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
        // 去掉开头 'IW' + 协议号 'APxx' (共6位) 后再查找 15 位数字
        String s = raw;
        if (s.startsWith("IW") && s.length() > 6) {
            int end = s.endsWith(END_MARKER) ? s.length() - 1 : s.length();
            s = s.substring(6, end);
        }
        Matcher m = IMEI_PATTERN.matcher(s);
        if (m.find()) return m.group(1);
        return null;
    }

    /**
     * 保存原始报文到磁盘（单独文件）并把消息追加到设备日志文件（按 IMEI）
     * 1) raw 保存到: {saveBasePath}/raw/yyyyMMdd_HHmmss_SSS_uuid.txt
     * 2) 设备日志追加到: {saveBasePath}/devices/{imei}.log （如果 imei==null 则保存在 devices/unknown.log）
     */
    private void saveRawAndDeviceLog(String rawMessage, String imei, String clientInfo) {
        // 保存原始文件（按天滚动合并）：
        //  - 每天一个归档文件：{saveBasePath}/raw/raw_yyyyMMdd.log
        //  - 追加写入，写入时追加换行，以便人眼与工具按行读取
        try {
            if (rawDirPath != null) {
                String day = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneId.systemDefault()).format(java.time.Instant.now());
                String fileName = String.format("raw_%s.log", day);
                Path f = rawDirPath.resolve(fileName);
                String time = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault()).format(java.time.Instant.now());
                String entry = String.format("%s [%s] %s%n", time, clientInfo == null ? "-" : clientInfo, rawMessage);
                // 使用全局 rawFileLock 保护，避免多线程同时创建/追加导致竞态
                synchronized (rawFileLock) {
                    Files.write(f, entry.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                }
            }
        } catch (Exception e) {
            log.warn("写入原始报文文件失败: {}", e.getMessage());
        }

        // 追加到设备日志
        try {
            String id = (imei != null && !imei.isEmpty()) ? imei : (clientInfo != null ? clientInfo.replace(':','_').replace('/','_').replace('\\','_') : "unknown");
            String safe = sanitizeFilename(id);
            Path deviceLog = (deviceDirPath != null) ? deviceDirPath.resolve(safe + ".log") : Paths.get(System.getProperty("user.dir")).resolve(safe + ".log");

            String time = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault()).format(java.time.Instant.now());
            String entry = String.format("%s [%s] %s%n", time, clientInfo == null ? "-" : clientInfo, rawMessage);

            // 并发写入保护：每个 imei/useKey 一个锁对象
            Object lock = deviceLocks.computeIfAbsent(safe, k -> new Object());
            synchronized (lock) {
                Files.write(deviceLog, entry.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
        } catch (Exception e) {
            log.warn("追加设备日志失败: {}", e.getMessage());
        }
    }

    /** 简单文件名清洗，移除非法字符 */
    private String sanitizeFilename(String in) {
        if (in == null) return "unknown";
        return in.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
