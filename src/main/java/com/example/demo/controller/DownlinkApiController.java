package com.example.demo.controller;

import com.example.demo.socket.downlink.DownlinkManager;
import com.example.demo.socket.downlink.DownlinkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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

    @GetMapping("/battery-report")
    public ResponseEntity<?> batteryReport(@RequestParam String imei) {
        if (imei == null || imei.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "imei required"));
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("imei", imei);
        report.put("online", downlinkManager.getOnlineImeis().contains(imei));
        report.put("batteryLevel", null);
        report.put("message", "当前设备未上报电量数据，已返回设备在线状态。");
        report.put("generatedAt", Instant.now().toString());
        return ResponseEntity.ok(report);
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

    /**
     * Send BP14 command (set contact whitelist). Query params: imei (required)
     * Body JSON: { "names":["name1","name2",...], "phones":["phone1","phone2",...], "seq":"1" }
     */
    @PostMapping("/bp14")
    public ResponseEntity<?> sendBp14(@RequestParam String imei, @RequestBody Map<String, Object> body) {
        if (imei == null || imei.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "imei required"));
        String seq = Objects.toString(body.getOrDefault("seq", "1"));
        List<String> names = (List<String>) body.getOrDefault("names", Collections.emptyList());
        List<String> phones = (List<String>) body.getOrDefault("phones", Collections.emptyList());

        DownlinkService service = new DownlinkService();
        String msg = service.buildBP14(imei, seq, names, phones);
        try {
            downlinkManager.sendToImei(imei, msg);
            return ResponseEntity.ok(Map.of("status", "sent", "message", msg));
        } catch (IOException e) {
            log.warn("sendBp14 failed imei={} msg={} err={}", imei, msg, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("status", "failed", "error", e.getMessage()));
        }
    }

    /**
     * Send BP15 command (GPS location interval). Query params: imei (required), interval (required, seconds)
     */
    @PostMapping("/bp15")
    public ResponseEntity<?> sendBp15(@RequestParam String imei, @RequestParam int interval, @RequestParam(required = false) String seq) {
        if (imei == null || imei.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "imei required"));
        if (interval < 0) return ResponseEntity.badRequest().body(Map.of("error", "interval must be non-negative"));
        String seqVal = seq != null ? seq : "1";

        DownlinkService service = new DownlinkService();
        String msg = service.buildBP15(imei, seqVal, interval);
        try {
            downlinkManager.sendToImei(imei, msg);
            return ResponseEntity.ok(Map.of("status", "sent", "message", msg));
        } catch (IOException e) {
            log.warn("sendBp15 failed imei={} msg={} err={}", imei, msg, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("status", "failed", "error", e.getMessage()));
        }
    }

    /**
     * Send BP16 command (immediate location). Query params: imei (required)
     */
    @PostMapping("/bp16")
    public ResponseEntity<?> sendBp16(@RequestParam String imei, @RequestParam(required = false) String seq) {
        if (imei == null || imei.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "imei required"));
        String seqVal = seq != null ? seq : "1";

        DownlinkService service = new DownlinkService();
        String msg = service.buildBP16(imei, seqVal);
        try {
            downlinkManager.sendToImei(imei, msg);
            return ResponseEntity.ok(Map.of("status", "sent", "message", msg));
        } catch (IOException e) {
            log.warn("sendBp16 failed imei={} msg={} err={}", imei, msg, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("status", "failed", "error", e.getMessage()));
        }
    }

    /**
     * Send BP17 command (factory reset). Query params: imei (required)
     */
    @PostMapping("/bp17")
    public ResponseEntity<?> sendBp17(@RequestParam String imei, @RequestParam(required = false) String seq) {
        if (imei == null || imei.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "imei required"));
        String seqVal = seq != null ? seq : "1";

        DownlinkService service = new DownlinkService();
        String msg = service.buildBP17(imei, seqVal);
        try {
            downlinkManager.sendToImei(imei, msg);
            return ResponseEntity.ok(Map.of("status", "sent", "message", msg));
        } catch (IOException e) {
            log.warn("sendBp17 failed imei={} msg={} err={}", imei, msg, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("status", "failed", "error", e.getMessage()));
        }
    }

    /**
     * Send BP18 command (restart device). Query params: imei (required)
     */
    @PostMapping("/bp18")
    public ResponseEntity<?> sendBp18(@RequestParam String imei, @RequestParam(required = false) String seq) {
        if (imei == null || imei.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "imei required"));
        String seqVal = seq != null ? seq : "1";

        DownlinkService service = new DownlinkService();
        String msg = service.buildBP18(imei, seqVal);
        try {
            downlinkManager.sendToImei(imei, msg);
            return ResponseEntity.ok(Map.of("status", "sent", "message", msg));
        } catch (IOException e) {
            log.warn("sendBp18 failed imei={} msg={} err={}", imei, msg, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("status", "failed", "error", e.getMessage()));
        }
    }

    /**
     * Send BP31 command (shutdown device). Query params: imei (required)
     */
    @PostMapping("/bp31")
    public ResponseEntity<?> sendBp31(@RequestParam String imei, @RequestParam(required = false) String seq) {
        if (imei == null || imei.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "imei required"));
        String seqVal = seq != null ? seq : "1";

        DownlinkService service = new DownlinkService();
        String msg = service.buildBP31(imei, seqVal);
        try {
            downlinkManager.sendToImei(imei, msg);
            return ResponseEntity.ok(Map.of("status", "sent", "message", msg));
        } catch (IOException e) {
            log.warn("sendBp31 failed imei={} msg={} err={}", imei, msg, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("status", "failed", "error", e.getMessage()));
        }
    }

    /**
     * Send BP19 command (set server info). Query params: imei (required), domainFlag (0=IP, 1=domain), hostOrIp, port
     */
    @PostMapping("/bp19")
    public ResponseEntity<?> sendBp19(@RequestParam String imei, @RequestParam int domainFlag, 
                                     @RequestParam String hostOrIp, @RequestParam int port, 
                                     @RequestParam(required = false) String seq) {
        if (imei == null || imei.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "imei required"));
        if (hostOrIp == null || hostOrIp.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "hostOrIp required"));
        String seqVal = seq != null ? seq : "1";

        DownlinkService service = new DownlinkService();
        String msg = service.buildBP19(imei, seqVal, domainFlag, hostOrIp, port);
        try {
            downlinkManager.sendToImei(imei, msg);
            return ResponseEntity.ok(Map.of("status", "sent", "message", msg));
        } catch (IOException e) {
            log.warn("sendBp19 failed imei={} msg={} err={}", imei, msg, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("status", "failed", "error", e.getMessage()));
        }
    }

    /**
     * Send BP20 command (set language and timezone). Query params: imei (required), language (0=Chinese, 1=English), timezone
     */
    @PostMapping("/bp20")
    public ResponseEntity<?> sendBp20(@RequestParam String imei, @RequestParam int language, 
                                     @RequestParam int timezone, @RequestParam(required = false) String seq) {
        if (imei == null || imei.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "imei required"));
        String seqVal = seq != null ? seq : "1";

        DownlinkService service = new DownlinkService();
        String msg = service.buildBP20(imei, seqVal, language, timezone);
        try {
            downlinkManager.sendToImei(imei, msg);
            return ResponseEntity.ok(Map.of("status", "sent", "message", msg));
        } catch (IOException e) {
            log.warn("sendBp20 failed imei={} msg={} err={}", imei, msg, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("status", "failed", "error", e.getMessage()));
        }
    }

    /**
     * Send BP32 command (make call). Query params: imei (required), phone (required)
     */
    @PostMapping("/bp32")
    public ResponseEntity<?> sendBp32(@RequestParam String imei, @RequestParam String phone, 
                                     @RequestParam(required = false) String seq) {
        if (imei == null || imei.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "imei required"));
        if (phone == null || phone.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "phone required"));
        String seqVal = seq != null ? seq : "1";

        DownlinkService service = new DownlinkService();
        String msg = service.buildBP32(imei, seqVal, phone);
        try {
            downlinkManager.sendToImei(imei, msg);
            return ResponseEntity.ok(Map.of("status", "sent", "message", msg));
        } catch (IOException e) {
            log.warn("sendBp32 failed imei={} msg={} err={}", imei, msg, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("status", "failed", "error", e.getMessage()));
        }
    }

    /**
     * Send BP33 command (work mode). Query params: imei (required), mode (1=normal, 2=power saving, 3=emergency)
     */
    @PostMapping("/bp33")
    public ResponseEntity<?> sendBp33(@RequestParam String imei, @RequestParam int mode, 
                                     @RequestParam(required = false) String seq) {
        if (imei == null || imei.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "imei required"));
        if (mode < 1 || mode > 3) return ResponseEntity.badRequest().body(Map.of("error", "mode must be 1, 2, or 3"));
        String seqVal = seq != null ? seq : "1";

        DownlinkService service = new DownlinkService();
        String msg = service.buildBP33(imei, seqVal, mode);
        try {
            downlinkManager.sendToImei(imei, msg);
            return ResponseEntity.ok(Map.of("status", "sent", "message", msg));
        } catch (IOException e) {
            log.warn("sendBp33 failed imei={} msg={} err={}", imei, msg, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("status", "failed", "error", e.getMessage()));
        }
    }

    /**
     * Send BP34 command (custom location mode). Query params: imei (required), mode, intervalSec, gpsFlag (0=off, 1=on)
     */
    @PostMapping("/bp34")
    public ResponseEntity<?> sendBp34(@RequestParam String imei, @RequestParam int mode, 
                                     @RequestParam int intervalSec, @RequestParam int gpsFlag, 
                                     @RequestParam(required = false) String seq) {
        if (imei == null || imei.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "imei required"));
        String seqVal = seq != null ? seq : "1";

        DownlinkService service = new DownlinkService();
        String msg = service.buildBP34(imei, seqVal, mode, intervalSec, gpsFlag);
        try {
            downlinkManager.sendToImei(imei, msg);
            return ResponseEntity.ok(Map.of("status", "sent", "message", msg));
        } catch (IOException e) {
            log.warn("sendBp34 failed imei={} msg={} err={}", imei, msg, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("status", "failed", "error", e.getMessage()));
        }
    }

    /**
     * Send BP40 command (quick command). Query params: imei (required)
     * Body JSON: { "payload":">*photo@1*<", "seq":"1" }
     */
    @PostMapping("/bp40")
    public ResponseEntity<?> sendBp40(@RequestParam String imei, @RequestBody Map<String, Object> body) {
        if (imei == null || imei.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "imei required"));
        String payload = Objects.toString(body.getOrDefault("payload", ""));
        String seq = Objects.toString(body.getOrDefault("seq", "1"));

        DownlinkService service = new DownlinkService();
        String msg = service.buildBP40(imei, seq, payload);
        try {
            downlinkManager.sendToImei(imei, msg);
            return ResponseEntity.ok(Map.of("status", "sent", "message", msg));
        } catch (IOException e) {
            log.warn("sendBp40 failed imei={} msg={} err={}", imei, msg, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("status", "failed", "error", e.getMessage()));
        }
    }

    /**
     * Send BP46 command (take photo). Query params: imei (required)
     */
    @PostMapping("/bp46")
    public ResponseEntity<?> sendBp46(@RequestParam String imei, @RequestParam(required = false) String seq, 
                                     @RequestParam(required = false, defaultValue = "1") int cmdValue, 
                                     @RequestParam(required = false) String param) {
        if (imei == null || imei.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "imei required"));
        String seqVal = seq != null ? seq : "1";

        DownlinkService service = new DownlinkService();
        String msg = service.buildBP46(imei, seqVal, cmdValue, param);
        try {
            downlinkManager.sendToImei(imei, msg);
            return ResponseEntity.ok(Map.of("status", "sent", "message", msg));
        } catch (IOException e) {
            log.warn("sendBp46 failed imei={} msg={} err={}", imei, msg, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("status", "failed", "error", e.getMessage()));
        }
    }

    /**
     * Send BP50 command (heartbeat test). Query params: imei (required)
     */
    @PostMapping("/bp50")
    public ResponseEntity<?> sendBp50(@RequestParam String imei, @RequestParam(required = false) String seq) {
        if (imei == null || imei.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "imei required"));
        String seqVal = seq != null ? seq : "1";

        DownlinkService service = new DownlinkService();
        String msg = service.buildBP50(imei, seqVal);
        try {
            downlinkManager.sendToImei(imei, msg);
            return ResponseEntity.ok(Map.of("status", "sent", "message", msg));
        } catch (IOException e) {
            log.warn("sendBp50 failed imei={} msg={} err={}", imei, msg, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("status", "failed", "error", e.getMessage()));
        }
    }

    /**
     * Send BP51 command (add contact). Query params: imei (required), name, phone
     */
    @PostMapping("/bp51")
    public ResponseEntity<?> sendBp51(@RequestParam String imei, @RequestParam String name, 
                                     @RequestParam String phone, @RequestParam(required = false) String seq) {
        if (imei == null || imei.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "imei required"));
        String seqVal = seq != null ? seq : "1";

        DownlinkService service = new DownlinkService();
        String msg = service.buildBP51(imei, seqVal, name, phone);
        try {
            downlinkManager.sendToImei(imei, msg);
            return ResponseEntity.ok(Map.of("status", "sent", "message", msg));
        } catch (IOException e) {
            log.warn("sendBp51 failed imei={} msg={} err={}", imei, msg, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("status", "failed", "error", e.getMessage()));
        }
    }

    /**
     * Send BP52 command (delete contact). Query params: imei (required), phone
     */
    @PostMapping("/bp52")
    public ResponseEntity<?> sendBp52(@RequestParam String imei, @RequestParam String phone, 
                                     @RequestParam(required = false) String seq) {
        if (imei == null || imei.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "imei required"));
        if (phone == null || phone.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "phone required"));
        String seqVal = seq != null ? seq : "1";

        DownlinkService service = new DownlinkService();
        String msg = service.buildBP52(imei, seqVal, phone);
        try {
            downlinkManager.sendToImei(imei, msg);
            return ResponseEntity.ok(Map.of("status", "sent", "message", msg));
        } catch (IOException e) {
            log.warn("sendBp52 failed imei={} msg={} err={}", imei, msg, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("status", "failed", "error", e.getMessage()));
        }
    }

    /**
     * Send BP84 command (whitelist switch). Query params: imei (required), setting (0=off, 1=on)
     */
    @PostMapping("/bp84")
    public ResponseEntity<?> sendBp84(@RequestParam String imei, @RequestParam int setting, 
                                     @RequestParam(required = false) String seq) {
        if (imei == null || imei.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "imei required"));
        String seqVal = seq != null ? seq : "1";

        DownlinkService service = new DownlinkService();
        String msg = service.buildBP84(imei, seqVal, setting);
        try {
            downlinkManager.sendToImei(imei, msg);
            return ResponseEntity.ok(Map.of("status", "sent", "message", msg));
        } catch (IOException e) {
            log.warn("sendBp84 failed imei={} msg={} err={}", imei, msg, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("status", "failed", "error", e.getMessage()));
        }
    }

    /**
     * Send BP86 command (health monitoring interval). Query params: imei (required), onOff (0=off, 1=on), minutes
     */
    @PostMapping("/bp86")
    public ResponseEntity<?> sendBp86(@RequestParam String imei, @RequestParam int onOff, 
                                     @RequestParam int minutes, @RequestParam(required = false) String seq) {
        if (imei == null || imei.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "imei required"));
        String seqVal = seq != null ? seq : "1";

        DownlinkService service = new DownlinkService();
        String msg = service.buildBP86(imei, seqVal, onOff, minutes);
        try {
            downlinkManager.sendToImei(imei, msg);
            return ResponseEntity.ok(Map.of("status", "sent", "message", msg));
        } catch (IOException e) {
            log.warn("sendBp86 failed imei={} msg={} err={}", imei, msg, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("status", "failed", "error", e.getMessage()));
        }
    }

    /**
     * Send BP88 command (find device). Query params: imei (required)
     */
    @PostMapping("/bp88")
    public ResponseEntity<?> sendBp88(@RequestParam String imei, @RequestParam(required = false) String seq) {
        if (imei == null || imei.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "imei required"));
        String seqVal = seq != null ? seq : "1";

        DownlinkService service = new DownlinkService();
        String msg = service.buildBP88(imei, seqVal);
        try {
            downlinkManager.sendToImei(imei, msg);
            return ResponseEntity.ok(Map.of("status", "sent", "message", msg));
        } catch (IOException e) {
            log.warn("sendBp88 failed imei={} msg={} err={}", imei, msg, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("status", "failed", "error", e.getMessage()));
        }
    }

    /**
     * Send BPMC command (motion detection control). Query params: imei (required), setting (0=off, 1=on, 2=get status)
     */
    @PostMapping("/bpmc")
    public ResponseEntity<?> sendBpmc(@RequestParam String imei, @RequestParam int setting, 
                                     @RequestParam(required = false) String seq) {
        if (imei == null || imei.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "imei required"));
        String seqVal = seq != null ? seq : "1";

        DownlinkService service = new DownlinkService();
        String msg = service.buildBPMC(imei, seqVal, setting);
        try {
            downlinkManager.sendToImei(imei, msg);
            return ResponseEntity.ok(Map.of("status", "sent", "message", msg));
        } catch (IOException e) {
            log.warn("sendBpmc failed imei={} msg={} err={}", imei, msg, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("status", "failed", "error", e.getMessage()));
        }
    }

    /**
     * Get raw logs for a device with pagination support.
     * Query params:
     * - imei: Device IMEI (required)
     * - startTime: Start time, ISO format (required)
     * - endTime: End time, ISO format (required)
     * - page: Page number, default 1
     * - size: Page size, default 100
     * - keyword: Optional keyword for filtering logs
     */
    @GetMapping("/raw-logs")
    public ResponseEntity<?> getRawLogs(
            @RequestParam String imei,
            @RequestParam String startTime,
            @RequestParam String endTime,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(required = false) String keyword) {
        if (imei == null || imei.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "imei required"));
        if (startTime == null || startTime.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "startTime required"));
        if (endTime == null || endTime.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "endTime required"));
        if (page < 1) page = 1;
        if (size < 1 || size > 1000) size = 100;

        try {
            // 解析开始和结束时间
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
            LocalDateTime start = LocalDateTime.parse(startTime, formatter);
            LocalDateTime end = LocalDateTime.parse(endTime, formatter);

            // 生成日期范围
            List<LocalDate> dates = new ArrayList<>();
            LocalDate current = start.toLocalDate();
            LocalDate endDate = end.toLocalDate();
            while (!current.isAfter(endDate)) {
                dates.add(current);
                current = current.plusDays(1);
            }

            // 日志文件存储目录
            String saveBasePath = "mpband_data";
            Path deviceLogsPath = Paths.get(System.getProperty("user.dir"), saveBasePath, "devices");

            // 收集所有符合条件的日志行
            List<String> allLogLines = new ArrayList<>();
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");

            for (LocalDate date : dates) {
                // 设备日志文件名格式：{imei}_yyyyMMdd.log
                String logFileName = String.format("%s_%s.log", imei, dateFormatter.format(date));
                Path logFile = deviceLogsPath.resolve(logFileName);

                if (Files.exists(logFile)) {
                    // 读取文件内容并过滤
                    List<String> lines = Files.readAllLines(logFile);
                    for (String line : lines) {
                        // 检查时间范围和关键词
                        if (isLineInTimeRange(line, start, end) && (keyword == null || keyword.isEmpty() || line.contains(keyword))) {
                            allLogLines.add(line);
                        }
                    }
                }
            }

            // 计算分页
            int total = allLogLines.size();
            int startIndex = (page - 1) * size;
            int endIndex = Math.min(startIndex + size, total);
            List<String> pageLines = new ArrayList<>();
            if (startIndex < endIndex) {
                pageLines = allLogLines.subList(startIndex, endIndex);
            }

            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("total", total);
            response.put("page", page);
            response.put("size", size);
            response.put("pages", (total + size - 1) / size);
            response.put("logs", String.join("\n", pageLines));
            response.put("count", pageLines.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get raw logs for imei={}: {}", imei, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "failed", "error", e.getMessage()));
        }
    }
    
    /**
     * 检查日志行是否在指定时间范围内
     */
    private boolean isLineInTimeRange(String line, LocalDateTime start, LocalDateTime end) {
        try {
            // 日志行格式：[2025-12-17 11:11:38.336] ...
            if (line.length() < 24) return false;
            String timeStr = line.substring(1, 24);
            DateTimeFormatter lineFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
            LocalDateTime lineTime = LocalDateTime.parse(timeStr, lineFormatter);
            return !lineTime.isBefore(start) && !lineTime.isAfter(end);
        } catch (Exception e) {
            // 解析失败的日志行默认包含
            return true;
        }
    }

    /**
     * Send BPPH command (SOS call switch). Query params: imei (required), setting (0=off, 1=on)
     */
    @PostMapping("/bpph")
    public ResponseEntity<?> sendBpph(@RequestParam String imei, @RequestParam int setting, 
                                     @RequestParam(required = false) String seq) {
        if (imei == null || imei.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "imei required"));
        String seqVal = seq != null ? seq : "1";

        DownlinkService service = new DownlinkService();
        String msg = service.buildBPPH(imei, seqVal, setting);
        try {
            downlinkManager.sendToImei(imei, msg);
            return ResponseEntity.ok(Map.of("status", "sent", "message", msg));
        } catch (IOException e) {
            log.warn("sendBpph failed imei={} msg={} err={}", imei, msg, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("status", "failed", "error", e.getMessage()));
        }
    }

    /**
     * Send BPSM command (SMS command). Query params: imei (required)
     * Body JSON: { "content":"@wifictl@=connect-123-12345678-psk", "seq":"1" }
     */
    @PostMapping("/bpsm")
    public ResponseEntity<?> sendBpsm(@RequestParam String imei, @RequestBody Map<String, Object> body) {
        if (imei == null || imei.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "imei required"));
        String content = Objects.toString(body.getOrDefault("content", ""));
        String seq = Objects.toString(body.getOrDefault("seq", "1"));

        DownlinkService service = new DownlinkService();
        String msg = service.buildBPSM(imei, seq, content);
        try {
            downlinkManager.sendToImei(imei, msg);
            return ResponseEntity.ok(Map.of("status", "sent", "message", msg));
        } catch (IOException e) {
            log.warn("sendBpsm failed imei={} msg={} err={}", imei, msg, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("status", "failed", "error", e.getMessage()));
        }
    }

    /**
     * Send BPTF command (time format). Query params: imei (required), setting (1=24h, 2=12h)
     */
    @PostMapping("/bptf")
    public ResponseEntity<?> sendBptf(@RequestParam String imei, @RequestParam int setting) {
        if (imei == null || imei.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "imei required"));
        if (setting < 1 || setting > 2) return ResponseEntity.badRequest().body(Map.of("error", "setting must be 1 or 2"));

        DownlinkService service = new DownlinkService();
        String msg = service.buildBPTF(imei, setting);
        try {
            downlinkManager.sendToImei(imei, msg);
            return ResponseEntity.ok(Map.of("status", "sent", "message", msg));
        } catch (IOException e) {
            log.warn("sendBptf failed imei={} msg={} err={}", imei, msg, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("status", "failed", "error", e.getMessage()));
        }
    }

    /**
     * Send BPWL command (set bound contact whitelist). Query params: imei (required)
     * Body JSON: { "contacts":["name1|phone1|deviceImei1",...], "seq":"1" }
     */
    @PostMapping("/bpwl")
    public ResponseEntity<?> sendBpwl(@RequestParam String imei, @RequestBody Map<String, Object> body) {
        if (imei == null || imei.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "imei required"));
        String seq = Objects.toString(body.getOrDefault("seq", "1"));
        List<String> contacts = (List<String>) body.getOrDefault("contacts", Collections.emptyList());

        DownlinkService service = new DownlinkService();
        String msg = service.buildBPWL(imei, seq, contacts);
        try {
            downlinkManager.sendToImei(imei, msg);
            return ResponseEntity.ok(Map.of("status", "sent", "message", msg));
        } catch (IOException e) {
            log.warn("sendBpwl failed imei={} msg={} err={}", imei, msg, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("status", "failed", "error", e.getMessage()));
        }
    }

    /**
     * Send BPXL command (measure heart rate). Query params: imei (required)
     */
    @PostMapping("/bpxl")
    public ResponseEntity<?> sendBpxl(@RequestParam String imei, @RequestParam(required = false) String seq) {
        if (imei == null || imei.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "imei required"));
        String seqVal = seq != null ? seq : "1";

        DownlinkService service = new DownlinkService();
        String msg = service.buildBPXL(imei, seqVal);
        try {
            downlinkManager.sendToImei(imei, msg);
            return ResponseEntity.ok(Map.of("status", "sent", "message", msg));
        } catch (IOException e) {
            log.warn("sendBpxl failed imei={} msg={} err={}", imei, msg, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("status", "failed", "error", e.getMessage()));
        }
    }

    /**
     * Send BPXY command (measure blood pressure). Query params: imei (required)
     */
    @PostMapping("/bpxy")
    public ResponseEntity<?> sendBpxy(@RequestParam String imei, @RequestParam(required = false) String seq) {
        if (imei == null || imei.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "imei required"));
        String seqVal = seq != null ? seq : "1";

        DownlinkService service = new DownlinkService();
        String msg = service.buildBPXY(imei, seqVal);
        try {
            downlinkManager.sendToImei(imei, msg);
            return ResponseEntity.ok(Map.of("status", "sent", "message", msg));
        } catch (IOException e) {
            log.warn("sendBpxy failed imei={} msg={} err={}", imei, msg, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("status", "failed", "error", e.getMessage()));
        }
    }

    /**
     * Send BPXZ command (measure blood oxygen). Query params: imei (required)
     */
    @PostMapping("/bpxz")
    public ResponseEntity<?> sendBpxz(@RequestParam String imei, @RequestParam(required = false) String seq) {
        if (imei == null || imei.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "imei required"));
        String seqVal = seq != null ? seq : "1";

        DownlinkService service = new DownlinkService();
        String msg = service.buildBPXZ(imei, seqVal);
        try {
            downlinkManager.sendToImei(imei, msg);
            return ResponseEntity.ok(Map.of("status", "sent", "message", msg));
        } catch (IOException e) {
            log.warn("sendBpxz failed imei={} msg={} err={}", imei, msg, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("status", "failed", "error", e.getMessage()));
        }
    }

    /**
     * Send BPXX command (measure temperature). Query params: imei (required)
     */
    @PostMapping("/bpxx")
    public ResponseEntity<?> sendBpxx(@RequestParam String imei, @RequestParam(required = false) String seq) {
        if (imei == null || imei.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "imei required"));
        String seqVal = seq != null ? seq : "1";

        DownlinkService service = new DownlinkService();
        String msg = service.buildBPXX(imei, seqVal);
        try {
            downlinkManager.sendToImei(imei, msg);
            return ResponseEntity.ok(Map.of("status", "sent", "message", msg));
        } catch (IOException e) {
            log.warn("sendBpxx failed imei={} msg={} err={}", imei, msg, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("status", "failed", "error", e.getMessage()));
        }
    }
}

