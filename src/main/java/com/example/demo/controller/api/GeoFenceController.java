package com.example.demo.controller.api;

import com.example.demo.model.dto.FenceCreateRequest;
import com.example.demo.model.dto.FenceDto;
import com.example.demo.model.dto.FenceUpdateRequest;
import com.example.demo.repository.GeoFenceRepository;
import com.example.demo.service.FenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/fences")
public class GeoFenceController {
    private final GeoFenceRepository fenceRepo;
    private final FenceService fenceService;

    public GeoFenceController(GeoFenceRepository fenceRepo, FenceService fenceService) {
        this.fenceRepo = fenceRepo;
        this.fenceService = fenceService;
    }

    /**
     * 创建围栏
     */
    @PostMapping
    public ResponseEntity<?> create(@RequestBody FenceCreateRequest request) {
        long id = fenceService.createFence(request);
        return ResponseEntity.created(URI.create("/api/fences/" + id)).body(id);
    }

    /**
     * 更新围栏
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable long id, @RequestBody FenceUpdateRequest request) {
        boolean success = fenceService.updateFence(id, request);
        return success ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    /**
     * 获取围栏详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable long id) {
        FenceDto fence = fenceService.getFenceDetail(id);
        if (fence == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(fence);
    }

    /**
     * 获取围栏列表
     */
    @GetMapping
    public List<FenceDto> getFences(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return fenceService.listFencesWithPatients(fenceRepo.listAll());
    }

    /**
     * 获取所有围栏
     */
    @GetMapping("/all")
    public List<FenceDto> getAllFences() {
        return fenceService.listFencesWithPatients(fenceRepo.listAll());
    }

    /**
     * 获取活跃围栏
     */
    @GetMapping("/active")
    public List<FenceDto> getActiveFences() {
        return fenceService.listFencesWithPatients(fenceRepo.listActive());
    }

    /**
     * 按用户获取围栏
     */
    @GetMapping("/by-user/{userId}")
    public List<FenceDto> listByUser(@PathVariable long userId) {
        return fenceService.listFencesWithPatients(fenceRepo.listByUser(userId));
    }

    /**
     * 按用户获取活跃围栏
     */
    @GetMapping("/by-user/{userId}/active")
    public List<FenceDto> listActiveByUser(@PathVariable long userId) {
        return fenceService.listFencesWithPatients(fenceRepo.listActiveByUser(userId));
    }

    /**
     * 删除围栏
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable long id) {
        boolean success = fenceService.deleteFence(id);
        return success ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
