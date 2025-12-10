package com.example.demo.socket.downlink;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.Socket;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.example.demo.model.DownlinkCommand;
import com.example.demo.repository.DownlinkCommandRepository;
import com.example.demo.repository.DeviceRepository;

/**
 * DownlinkManager - 管理在线设备连接并提供下行指令下发功能
 */
@Component
public class DownlinkManager {
    private static final Logger log = LoggerFactory.getLogger(DownlinkManager.class);

    // imei -> socket 映射（便于按 IMEI 下发指令）
    private final Map<String, Socket> imeiToSocket = new ConcurrentHashMap<>();

    // socket hash -> imei 映射，用于断开时清理
    private final Map<Integer, String> socketToImei = new ConcurrentHashMap<>();

    // 内部的下行包构造与发送器
    private final DownlinkService downlinkService = new DownlinkService();

    // 下行命令持久化仓库（记录平台发出的下行命令与状态）
    private final DownlinkCommandRepository downlinkCommandRepository;
    private final DeviceRepository deviceRepository;

    public DownlinkManager(DownlinkCommandRepository downlinkCommandRepository, DeviceRepository deviceRepository) {
        this.downlinkCommandRepository = downlinkCommandRepository;
        this.deviceRepository = deviceRepository;
    }

    /**
     * 注册设备连接（在收到设备 AP00 登录后由服务器调用）
     *
     * @param imei 设备 imei（唯一）
     * @param socket 已连接的 socket
     */
    public void register(String imei, Socket socket) {
        if (imei == null || socket == null) return;
        imeiToSocket.put(imei, socket);
        socketToImei.put(System.identityHashCode(socket), imei);
        log.info("Registered device IMEI={} socket={}:{}", imei, socket.getInetAddress().getHostAddress(), socket.getPort());
    }

    /**
     * 注销指定 socket（连接断开时调用）
     *
     * @param socket 要注销的 socket
     */
    public void unregisterBySocket(Socket socket) {
        if (socket == null) return;
        String imei = socketToImei.remove(System.identityHashCode(socket));
        if (imei != null) {
            imeiToSocket.remove(imei);
            log.info("Unregistered device IMEI={} for socket {}:{}", imei, socket.getInetAddress().getHostAddress(), socket.getPort());
        }
    }

    /**
     * 注销指定 imei（主动断开或替换连接时调用）
     */
    public void unregisterByImei(String imei) {
        if (imei == null) return;
        Socket s = imeiToSocket.remove(imei);
        if (s != null) socketToImei.remove(System.identityHashCode(s));
    }

    /**
     * 通过 IMEI 向设备发送下行指令（阻塞，可能抛出 IOException）
     * 该方法会在数据库中先写入 DownlinkCommand（状态 PENDING），发送后更新为 SENT 或 FAILED。
     *
     * @param imei    目标设备 imei
     * @param message 完整的下行消息（已包含 IW 和 #）
     */
    public void sendToImei(String imei, String message) throws IOException {
        Socket s = imeiToSocket.get(imei);
        if (s == null || s.isClosed()) {
            throw new IOException("Device not connected: " + imei);
        }

        // 在数据库中创建下行记录（PENDING）
        DownlinkCommand cmd = new DownlinkCommand();
        cmd.setImei(imei);
        cmd.setPayload(message);
        // 尝试关联 device
        try {
            if (imei != null && !imei.isEmpty()) {
                deviceRepository.findByImei(imei).ifPresent(cmd::setDevice);
            }
        } catch (Exception e) {
            // 忽略查找异常
        }
        // 尝试解析协议与流水号（格式：IWBPxx,IMEI,seq,...#）
        try {
            if (message != null && message.length() >= 6) {
                String proto = message.substring(2, Math.min(6, message.length()));
                cmd.setProtocol(proto);
                String body = message.substring(2, message.length() - 1); // 去掉头部 IW 和尾部 # 的一部分
                String[] parts = body.split(",");
                if (parts.length > 2) cmd.setSeq(parts[2]);
            }
        } catch (Exception ex) {
            // 忽略解析异常，继续保存原始 payload
        }
        cmd.setStatus("PENDING");
        downlinkCommandRepository.save(cmd);

        try {
            downlinkService.send(s, message);
            cmd.setStatus("SENT");
            cmd.setSentAt(new Date());
            downlinkCommandRepository.save(cmd);
            log.info("Sent downlink to IMEI={}: {}", imei, message);
        } catch (IOException ioe) {
            cmd.setStatus("FAILED");
            cmd.setRetryCount((cmd.getRetryCount() == null ? 0 : cmd.getRetryCount()) + 1);
            downlinkCommandRepository.save(cmd);
            log.error("Failed to send downlink to IMEI={}: {}", imei, ioe.getMessage());
            throw ioe;
        }
    }

    /**
     * 获取当前在线 IMEI 列表（只读）
     */
    public Set<String> getOnlineImeis() {
        return Collections.unmodifiableSet(imeiToSocket.keySet());
    }

    /**
     * 获取当前 socket 对应的 IMEI（若该 socket 已在 register 时登记过）。
     * 目的：当上行报文本身不携带 IMEI 时，服务器可以根据已注册的连接找到对应设备号，
     * 并在保存定位/心跳/健康数据时把 IMEI 一并关联保存，便于按设备号查询。
     */
    public String getImeiBySocket(Socket socket) {
        if (socket == null) return null;
        return socketToImei.get(System.identityHashCode(socket));
    }

}
