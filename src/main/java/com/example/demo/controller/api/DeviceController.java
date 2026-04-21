package com.example.demo.controller.api;

import com.example.demo.model.Device;
import com.example.demo.model.dto.DeviceInfoDto;
import com.example.demo.model.dto.PageResponse;
import com.example.demo.service.DeviceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {
    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    /**
     * 创建设备
     */
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Device d) {
        Long id = deviceService.createDevice(d);
        return ResponseEntity.created(URI.create("/api/devices/" + id)).body(id);
    }

    /**
     * 更新设备
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable long id, @RequestBody Device d) {
        if (!deviceService.existsById(id)) return ResponseEntity.notFound().build();
        deviceService.updateDevice(id, d);
        return ResponseEntity.ok().build();
    }

    /**
     * 根据ID获取设备详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable long id) {
        DeviceInfoDto device = deviceService.getDeviceById(id);
        if (device == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(device);
    }

    /**
     * 根据IMEI获取设备详情
     */
    @GetMapping("/by-imei/{imei}")
    public ResponseEntity<?> getByImei(@PathVariable("imei") String imei) {
        DeviceInfoDto device = deviceService.getDeviceByImei(imei);
        if (device == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(device);
    }

    /**
     * 分页查询设备列表（兼容page/size和limit/offset两种分页参数）
     */
    @GetMapping(produces = "application/json")
    public ResponseEntity<PageResponse<DeviceInfoDto>> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset,
            @RequestParam(required = false) String search) {
        int effectivePage = (page != null) ? page : ((offset != null && limit != null) ? offset / limit : 0);
        int effectiveSize = (size != null) ? size : (limit != null ? limit : 20);
        return ResponseEntity.ok(deviceService.listDevices(effectivePage, effectiveSize, search));
    }

    /**
     * 删除设备
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable long id) {
        if (!deviceService.existsById(id)) return ResponseEntity.notFound().build();
        deviceService.deleteDevice(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 获取未关联设备列表
     */
    @GetMapping("/available")
    public List<DeviceInfoDto> getAvailableDevices() {
        return deviceService.getAvailableDevices();
    }
}
