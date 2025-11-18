package com.example.demo.controller;

import com.example.demo.socket.downlink.DownlinkManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/downlink")
public class DownlinkApiController {
    private static final Logger log = LoggerFactory.getLogger(DownlinkApiController.class);

    private final DownlinkManager downlinkManager;

    public DownlinkApiController(DownlinkManager downlinkManager) {
        this.downlinkManager = downlinkManager;
    }

    @GetMapping("/online")
    public ResponseEntity<Set<String>> online() {
        Set<String> online = downlinkManager.getOnlineImeis();
        return ResponseEntity.ok(online);
    }

    /**
     * Send BP00 time-sync command to device. Query params: imei (required), timezone (optional int hours)
     */
    @PostMapping("/bp00")
    public ResponseEntity<?> sendBp00(@RequestParam String imei, @RequestParam(required = false) Integer timezone) {
        if (imei == null || imei.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "imei required"));

        DateTimeFormatter fmtUtc = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.of("UTC"));
        String utc = fmtUtc.format(Instant.now());
        int tzHours = timezone != null ? timezone : ZoneId.systemDefault().getRules().getOffset(Instant.now()).getTotalSeconds() / 3600;
        String zoneId = ZoneId.systemDefault().getId();

        String msg = String.format("IWBP00,%s,%d,%s#", utc, tzHours, zoneId);
        try {
            downlinkManager.sendToImei(imei, msg);
            return ResponseEntity.ok(Map.of("status", "sent", "message", msg));
        } catch (IOException e) {
            log.warn("sendBp00 failed imei={} msg={} err={}", imei, msg, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("status", "failed", "error", e.getMessage()));
        }
    }

    /**
     * Send BP12 (SOS numbers) to device. Body JSON: { "imei":"...","seq":"1","sos":["138...","",""] }
     */
    @PostMapping("/bp12")
    public ResponseEntity<?> sendBp12(@RequestBody Map<String, Object> body) {
        String imei = (String) body.get("imei");
        if (imei == null || imei.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "imei required"));
        String seq = Objects.toString(body.getOrDefault("seq", "1"));
        List<String> sosList = new ArrayList<>();
        Object sosObj = body.get("sos");
        if (sosObj instanceof List) {
            for (Object o : (List<?>) sosObj) sosList.add(Objects.toString(o, ""));
        } else if (sosObj instanceof String) {
            // allow comma separated
            String s = (String) sosObj;
            sosList.addAll(Arrays.asList(s.split("[|,]", -1)));
        }
        while (sosList.size() < 3) sosList.add("");
        String sosJoined = String.join("|", sosList.subList(0, 3));

        String msg = String.format("IWBP12,%s,%s,%s#", imei, seq, sosJoined);
        try {
            downlinkManager.sendToImei(imei, msg);
            return ResponseEntity.ok(Map.of("status", "sent", "message", msg));
        } catch (IOException e) {
            log.warn("sendBp12 failed imei={} msg={} err={}", imei, msg, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("status", "failed", "error", e.getMessage()));
        }
    }

    /**
     * Send custom raw payload. Body JSON: { "imei":"...", "payload":"IWBPxx,...#" }
     */
    @PostMapping("/custom")
    public ResponseEntity<?> sendCustom(@RequestBody Map<String, String> body) {
        String imei = body.get("imei");
        String payload = body.get("payload");
        if (imei == null || imei.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "imei required"));
        if (payload == null || payload.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "payload required"));

        String msg = payload.trim();
        if (!msg.startsWith("IW")) msg = "IW" + msg;
        if (!msg.endsWith("#")) msg = msg + "#";

        try {
            downlinkManager.sendToImei(imei, msg);
            return ResponseEntity.ok(Map.of("status", "sent", "message", msg));
        } catch (IOException e) {
            log.warn("sendCustom failed imei={} msg={} err={}", imei, msg, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("status", "failed", "error", e.getMessage()));
        }
    }
}

