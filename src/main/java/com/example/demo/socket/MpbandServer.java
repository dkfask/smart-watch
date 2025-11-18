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
        String imei = null;
        try {
            imei = getImeiFromRaw(message);
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

        // 处理（传入原始消息与已提取 imei）
        processPacket(packet, message, clientInfo, clientSocket, imei);

        // 构造并发送回复
        String response = createResponse(packet);
        writeFrame(writer, response);
        log.info("📤 发送回复: {} (newlineAppended={})", response, appendNewlineAfterResponse);
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

        switch (proto) {
            case "AP00":
                // 登录包：IWAP00+IMEI# 或 IWAP00+IMEI,MCC|MNC|APN# 或 IWAP00+IMEI,ICCID,IMSI#
                handleAp00(payload, clientSocket, clientInfo, imei);
                break;
            case "AP01":
                // 定位包
                handleAp01(payload, clientInfo, imei);
                break;
            case "AP02":
                // 健康包（旧版 AP02 可能另用，此处如无明确需求可当作健康数据）
                saveHealthData(parseKeyValueParams(payload), imei);
                break;
            case "AP03":
                handleAp03(payload, clientInfo, imei);
                break;
            case "AP04":
                handleAp04(payload, clientInfo, imei);
                break;
            case "AP10":
                handleAp10(payload, clientInfo, imei);
                break;
            case "AP42":
                handleAp42(payload, clientInfo, imei);
                break;
            case "APBL":
                handleApBl(payload, clientInfo, imei);
                break;
            case "APJK":
                handleApJk(payload, clientInfo, imei);
                break;
            case "APTP":
                handleApTp(payload, clientInfo, imei);
                break;
            case "APVR":
                handleApVr(payload, clientInfo, imei);
                break;
            case "APWR":
                handleApWr(payload, clientInfo, imei);
                break;
            default:
                log.info("❓ 未知协议类型: {} raw={}", proto, payload);
        }
    }

    // --------------------------- 各协议处理函数（签名加入 imei 参数） ---------------------------

    /** 处理 AP00 登录包：根据三种格式解析并保存设备信息 */
    private void handleAp00(String payload, Socket clientSocket, String clientInfo, String imeiFromRaw) {
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
        try {
            Optional<Device> opt = deviceRepository.findByImei(imei);
            if (opt.isPresent()) {
                log.info("设备已存在 IMEI={}", imei);
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
            log.info("💾 新设备已保存 IMEI={} 来自 {}", imei, clientInfo);
        } catch (Exception e) {
            log.error("保存设备失败 IMEI={}", imei, e);
        }
    }

    /**
     * 处理 AP01 定位包的简化解析与存储，加入 imei 参数以便把定位记录与设备关联。
     */
    private void handleAp01(String payload, String clientInfo, String imei) {
        if (payload == null || payload.isEmpty()) return;
        log.debug("AP01 来自 {} 的 payload: {}", clientInfo, payload);
        // 按逗号分段：第一个段包含大块 GPS 信息，其后一般依次为 MCC,MNC,LAC,CID,...,wifi
        String[] segments = payload.split(",", 6);
        String gpsBlock = segments.length > 0 ? segments[0] : "";

        Map<String, String> params = new LinkedHashMap<>();
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

        // 保存到数据库：传入 imei，确保记录关联到设备
        saveLocationData(params, imei);
        log.info("AP01 处理完成, 保存解析字段: {}", params.keySet());
    }

    private void handleAp03(String payload, String clientInfo, String imei) {
        if (payload == null || payload.isEmpty()) return;
        log.debug("AP03 来自 {} 的 payload: {}", clientInfo, payload);
        String[] parts = payload.split(",");
        Map<String, String> params = new LinkedHashMap<>();
        params.put("param_block", parts.length > 0 ? parts[0] : "");
        if (parts.length > 1) params.put("steps", parts[1]);
        if (parts.length > 2) params.put("roll_count", parts[2]);
        if (parts.length > 3) params.put("work_mode", parts[3]);
        if (parts.length > 4) params.put("interval", parts[4]);

        saveHeartbeatData(params, imei);
        log.info("AP03 心跳包已保存: {}", params);
    }

    private void handleAp04(String payload, String clientInfo, String imei) {
        if (payload == null || payload.isEmpty()) return;
        log.debug("AP04 来自 {} 的 payload: {}", clientInfo, payload);
        String[] parts = payload.split(",");
        Map<String, String> params = new LinkedHashMap<>();
        params.put("battery", parts.length > 0 ? parts[0] : "");
        saveHeartbeatData(params, imei);
        log.info("AP04 低电报警已处理: {}", params);
    }

    private void handleAp10(String payload, String clientInfo, String imei) {
        log.debug("AP10 来自 {} 的 payload: {}", clientInfo, payload);
        Map<String, String> params = new LinkedHashMap<>();
        params.put("raw", payload);
        saveLocationData(params, imei);
        log.info("AP10 报警上报，原始内容: {}", payload);
    }

    private void handleAp42(String payload, String clientInfo, String imei) {
        log.debug("AP42 来自 {} 的 payload: {}", clientInfo, payload);
        String[] p = payload.split(",", 5);
        String time = p.length > 0 ? p[0] : "";
        String total = p.length > 1 ? p[1] : "";
        String seq = p.length > 2 ? p[2] : "";
        log.info("AP42 图片包 time={} total={} seq={}", time, total, seq);
    }

    private void handleApBl(String payload, String clientInfo, String imei) {
        log.debug("APBL 来自 {} 的 payload: {}", clientInfo, payload);
        Map<String, String> params = new LinkedHashMap<>();
        params.put("raw", payload);
        log.info("APBL 蓝牙数据: {}", payload);
    }

    private void handleApJk(String payload, String clientInfo, String imei) {
        if (payload == null || payload.isEmpty()) return;
        log.debug("APJK 来自 {} 的 payload: {}", clientInfo, payload);

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
            log.info("APJK 健康数据已保存（结构化）: {}", params);
            return;
        }

        // 回退：如果不符合新的三段式格式，则尝试解析为 key=value 键值对（向后兼容）
        Map<String, String> kv = parseKeyValueParams(payload);
        if (imei != null) kv.put("imei", imei);
        saveHealthData(kv, imei);
        log.info("APJK 健康数据已按 kv 解析并保存: {}", kv);
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
            // 其中时间为 UTC 0 时区时间（yyyyMMddHHmmss），��二项为服务器当前时区小时偏移
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
     * 现在接受 imei，若 imei 不为空则尝试关联 Device（若不存在则创建）。
     */
    private void saveLocationData(Map<String, String> params, String imei) {
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
            if (imei != null && !imei.isEmpty()) {
                rec.setImei(imei);
                Device d = findOrCreateDeviceByImei(imei);
                if (d != null) rec.setDevice(d);
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
     * 接收 imei 并关联设备。
     */
    private void saveHeartbeatData(Map<String, String> params, String imei) {
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

            if (imei != null && !imei.isEmpty()) {
                rec.setImei(imei);
                Device d = findOrCreateDeviceByImei(imei);
                if (d != null) rec.setDevice(d);
            }

            heartbeatRecordRepository.save(rec);
        } catch (Exception e) {
            log.warn("保存心跳数据失败: {}", e.getMessage());
        }
    }

    /**
     * 保存健康数据到数据库（APJK / APTP 等）。
     * 接收 imei 并关联设备。
     */
    private void saveHealthData(Map<String, String> params, String imei) {
        try {
            HealthRecord rec = new HealthRecord();
            if (params.containsKey("temp")) {
                rec.setDataType("temperature");
                rec.setValue(params.get("temp"));
            } else if (params.containsKey("data_type")) {
                rec.setDataType(params.get("data_type"));
                rec.setValue(params.getOrDefault("value", params.toString()));
            } else {
                rec.setDataType("unknown");
                rec.setValue(params.toString());
            }

            if (params.containsKey("timestamp")) {
                try {
                    java.time.format.DateTimeFormatter df = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(params.get("timestamp"), df);
                    java.time.Instant inst = ldt.toInstant(java.time.ZoneOffset.UTC);
                    rec.setRecvTime(Date.from(inst));
                } catch (Exception ignored) {}
            }

            if (imei != null && !imei.isEmpty()) {
                rec.setImei(imei);
                Device d = findOrCreateDeviceByImei(imei);
                if (d != null) rec.setDevice(d);
            }
            rec.setExtra(params.toString());
            healthRecordRepository.save(rec);
        } catch (Exception e) {
            log.warn("保存健康数据失败: {}", e.getMessage());
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
            if (opt.isPresent()) return opt.get();
            Device d = new Device();
            d.setImei(imei);
            d.setCreatedAt(new Date());
            deviceRepository.save(d);
            log.info("自动创建设备记录 imei={}", imei);
            return d;
        } catch (Exception e) {
            log.warn("查找或创建 Device 失败 imei={}: {}", imei, e.getMessage());
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

    private void handleApTp(String payload, String clientInfo, String imei) {
        // APTP：体温包，格式示例： temp,wrist_temp  或单个值
        if (payload == null || payload.isEmpty()) return;
        log.debug("APTP 来自 {} 的 payload: {}", clientInfo, payload);
        String[] p = payload.split(",");
        Map<String, String> params = new LinkedHashMap<>();
        if (p.length >= 1 && p[0] != null && !p[0].isEmpty()) params.put("temp", p[0]);
        if (p.length >= 2 && p[1] != null && !p[1].isEmpty()) params.put("wrist_temp", p[1]);
        // 保存为健康数据（saveHealthData 会识别 temp 字段并标记为 temperature）
        saveHealthData(params, imei);
        log.info("APTP 体温数据已保存: {}", params);
    }

    private void handleApVr(String payload, String clientInfo, String imei) {
        // APVR：版本信息，示例： imei,firmware  或单个 firmware
        if (payload == null || payload.isEmpty()) return;
        log.debug("APVR 来自 {} 的 payload: {}", clientInfo, payload);
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
        log.info("APVR 版本信息已保存: {}", params);
    }

    private void handleApWr(String payload, String clientInfo, String imei) {
        // APWR：佩戴状态，示例： imei,wear_flag,timestamp
        if (payload == null || payload.isEmpty()) return;
        log.debug("APWR 来自 {} 的 payload: {}", clientInfo, payload);
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
        log.info("APWR 佩戴状态已保存: {}", params);
    }
}
