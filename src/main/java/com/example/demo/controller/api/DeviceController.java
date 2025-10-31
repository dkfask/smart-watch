package com.example.demo.controller.api;

import com.example.demo.model.Device;
import com.example.demo.repository.DeviceRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {
    private final DeviceRepository repo;

    public DeviceController(DeviceRepository repo) { this.repo = repo; }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Device d) {
        Device saved = repo.save(d);
        Long id = saved.getId();
        return ResponseEntity.created(URI.create("/api/devices/" + id)).body(id);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable long id, @RequestBody Device d) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        d.setId(id);
        repo.save(d);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable long id) {
        Optional<Device> d = repo.findById(id);
        return d.<ResponseEntity<?>>map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/by-imei/{imei}")
    public ResponseEntity<?> getByImei(@PathVariable("imei") String imei) {
        Optional<Device> d = repo.findByImei(imei);
        return d.<ResponseEntity<?>>map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<Device> list(@RequestParam(defaultValue = "20") int limit,
                             @RequestParam(defaultValue = "0") int offset) {
        // 简单内存分页：offset 表示起始位置（0-based）
        Iterable<Device> allIter = repo.findAll();
        List<Device> all = new ArrayList<>();
        for (Device d : allIter) all.add(d);
        if (limit <= 0) limit = 20;
        if (offset < 0) offset = 0;
        int from = Math.min(offset, all.size());
        int to = Math.min(from + limit, all.size());
        return all.subList(from, to);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable long id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
