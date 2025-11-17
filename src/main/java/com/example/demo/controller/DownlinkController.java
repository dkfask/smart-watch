package com.example.demo.controller;

import com.example.demo.socket.downlink.DownlinkManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * DownlinkController
 *
 * 提供一个简单的 REST 接口用于查询当前平台认为“在线”的设备 IMEI 列表。
 *
 * 设计说明（中文注释）：
 * - 在线设备的记录由 `DownlinkManager` 维护（内部使用 ConcurrentHashMap 保存 imei -> socket 映射）。
 * - 手环在登录（AP00 包）时，服务器会调用 DownlinkManager.register(imei, socket) 注册该连接。
 * - 连接断开或超时时，服务器会调用 DownlinkManager.unregisterBySocket(socket) 注销。
 * - 本控制器仅做只读透传（不修改内部状态），返回不可变集合以保证线程安全。
 */
@RestController
@RequestMapping("/api")
public class DownlinkController {
    private static final Logger log = LoggerFactory.getLogger(DownlinkController.class);

    private final DownlinkManager downlinkManager;

    public DownlinkController(DownlinkManager downlinkManager) {
        this.downlinkManager = downlinkManager;
    }

    /**
     * GET /api/online-devices
     * 返回当前在线设备 IMEI 列表（JSON 数组）。
     * 例如: ["860000000000000","860000000000001"]
     */
    @GetMapping("/online-devices")
    public ResponseEntity<Set<String>> getOnlineDevices() {
        Set<String> online = downlinkManager.getOnlineImeis();
        log.debug("返回在线设备列表，count={}", online.size());
        return ResponseEntity.ok(online);
    }
}

