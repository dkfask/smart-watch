package com.example.demo.controller.api;

import com.example.demo.model.GeoFence;
import com.example.demo.repository.FencePatientRepository;
import com.example.demo.repository.GeoFenceRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/fences")
public class GeoFenceController {
    private final GeoFenceRepository repo;
    private final FencePatientRepository fencePatientRepo;

    public GeoFenceController(GeoFenceRepository repo, FencePatientRepository fencePatientRepo) {
        this.repo = repo;
        this.fencePatientRepo = fencePatientRepo;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> create(@RequestBody Map<String, Object> requestBody) {
        // 创建GeoFence对象
        GeoFence f = new GeoFence();
        f.setName((String) requestBody.get("name"));
        f.setType((String) requestBody.get("type"));
        f.setRadius((Integer) requestBody.get("radius"));
        f.setCoordinates((String) requestBody.get("coordinates"));
        f.setStatus((String) requestBody.get("status"));
        f.setDescription((String) requestBody.get("description"));
        f.setCreatedBy(requestBody.get("createdBy") != null ? ((Number) requestBody.get("createdBy")).longValue() : null);
        f.setPatientId(requestBody.get("patientId") != null ? ((Number) requestBody.get("patientId")).longValue() : null);
        f.setIsMultiPatient((Boolean) requestBody.get("isMultiPatient"));
        f.setCenterLat((Double) requestBody.get("centerLat"));
        f.setCenterLng((Double) requestBody.get("centerLng"));
        
        // 保存围栏基本信息
        long id = repo.create(f);
        
        // 处理病人关联
        List<?> patientIdsRaw = (List<?>) requestBody.get("patientIds");
        if (patientIdsRaw != null && !patientIdsRaw.isEmpty()) {
            // 将patientIds转换为Long类型列表
            List<Long> patientIds = new ArrayList<>();
            for (Object rawId : patientIdsRaw) {
                if (rawId != null) {
                    patientIds.add(((Number) rawId).longValue());
                }
            }
            // 绑定病人到围栏
            fencePatientRepo.bindPatientsToFence(id, patientIds);
        }
        
        return ResponseEntity.created(URI.create("/api/fences/" + id)).body(id);
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> update(@PathVariable long id, @RequestBody Map<String, Object> requestBody) {
        // 创建GeoFence对象
        GeoFence f = new GeoFence();
        f.setId(id);
        f.setName((String) requestBody.get("name"));
        f.setType((String) requestBody.get("type"));
        f.setRadius((Integer) requestBody.get("radius"));
        f.setCoordinates((String) requestBody.get("coordinates"));
        f.setStatus((String) requestBody.get("status"));
        f.setDescription((String) requestBody.get("description"));
        f.setCreatedBy(requestBody.get("createdBy") != null ? ((Number) requestBody.get("createdBy")).longValue() : null);
        f.setPatientId(requestBody.get("patientId") != null ? ((Number) requestBody.get("patientId")).longValue() : null);
        f.setIsMultiPatient((Boolean) requestBody.get("isMultiPatient"));
        f.setCenterLat((Double) requestBody.get("centerLat"));
        f.setCenterLng((Double) requestBody.get("centerLng"));
        
        // 更新围栏基本信息
        int n = repo.update(f);
        if (n <= 0) {
            return ResponseEntity.notFound().build();
        }
        
        // 处理病人关联
        List<?> patientIdsRaw = (List<?>) requestBody.get("patientIds");
        // 先删除该围栏的所有病人关联
        fencePatientRepo.clearFencePatients(id);
        // 再绑定新的病人关联
        if (patientIdsRaw != null && !patientIdsRaw.isEmpty()) {
            // 将patientIds转换为Long类型列表
            List<Long> patientIds = new ArrayList<>();
            for (Object rawId : patientIdsRaw) {
                if (rawId != null) {
                    patientIds.add(((Number) rawId).longValue());
                }
            }
            fencePatientRepo.bindPatientsToFence(id, patientIds);
        }
        
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable long id) {
        Optional<GeoFence> optionalFence = repo.findById(id);
        if (optionalFence.isPresent()) {
            GeoFence fence = optionalFence.get();
            
            // 构建包含patientIds的响应
            Map<String, Object> fenceMap = new HashMap<>();
            fenceMap.put("id", fence.getId());
            fenceMap.put("name", fence.getName());
            fenceMap.put("type", fence.getType());
            fenceMap.put("centerLat", fence.getCenterLat());
            fenceMap.put("centerLng", fence.getCenterLng());
            fenceMap.put("radius", fence.getRadius());
            fenceMap.put("coordinates", fence.getCoordinates());
            fenceMap.put("status", fence.getStatus());
            fenceMap.put("description", fence.getDescription());
            fenceMap.put("createdBy", fence.getCreatedBy());
            fenceMap.put("createdAt", fence.getCreatedAt());
            fenceMap.put("updatedAt", fence.getUpdatedAt());
            fenceMap.put("patientId", fence.getPatientId());
            fenceMap.put("isMultiPatient", fence.getIsMultiPatient());
            
            // 获取围栏关联的所有病人ID
            List<Long> patientIds = new ArrayList<>();
            
            // 1. 添加旧格式的patient_id
            if (fence.getPatientId() != null) {
                patientIds.add(fence.getPatientId());
            }
            
            // 2. 添加从fence_patients表获取的病人ID
            List<com.example.demo.model.FencePatient> fencePatients = fencePatientRepo.findByFenceId(fence.getId());
            for (com.example.demo.model.FencePatient fp : fencePatients) {
                if (fp.getPatient() != null && fp.getPatient().getId() != null && !patientIds.contains(fp.getPatient().getId())) {
                    patientIds.add(fp.getPatient().getId());
                }
            }
            
            fenceMap.put("patientIds", patientIds);
            
            return ResponseEntity.ok(fenceMap);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    public List<Map<String, Object>> getFences(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        // 目前返回所有围栏，后续可根据需求添加真正的分页逻辑
        return buildFenceResponse(repo.listAll());
    }

    @GetMapping("/all")
    public List<Map<String, Object>> getAllFences() {
        return buildFenceResponse(repo.listAll());
    }

    @GetMapping("/active")
    public List<Map<String, Object>> getActiveFences() {
        return buildFenceResponse(repo.listActive());
    }

    @GetMapping("/by-user/{userId}")
    public List<Map<String, Object>> listByUser(@PathVariable long userId) {
        return buildFenceResponse(repo.listByUser(userId));
    }

    @GetMapping("/by-user/{userId}/active")
    public List<Map<String, Object>> listActiveByUser(@PathVariable long userId) {
        return buildFenceResponse(repo.listActiveByUser(userId));
    }
    
    /**
     * 构建包含关联病人ID列表的围栏响应
     * @param fences 原始围栏列表
     * @return 包含patientIds字段的围栏列表
     */
    private List<Map<String, Object>> buildFenceResponse(List<GeoFence> fences) {
        List<Map<String, Object>> response = new ArrayList<>();
        
        for (GeoFence fence : fences) {
            Map<String, Object> fenceMap = new HashMap<>();
            fenceMap.put("id", fence.getId());
            fenceMap.put("name", fence.getName());
            fenceMap.put("type", fence.getType());
            fenceMap.put("centerLat", fence.getCenterLat());
            fenceMap.put("centerLng", fence.getCenterLng());
            fenceMap.put("radius", fence.getRadius());
            fenceMap.put("coordinates", fence.getCoordinates());
            fenceMap.put("status", fence.getStatus());
            fenceMap.put("description", fence.getDescription());
            fenceMap.put("createdBy", fence.getCreatedBy());
            fenceMap.put("createdAt", fence.getCreatedAt());
            fenceMap.put("updatedAt", fence.getUpdatedAt());
            fenceMap.put("patientId", fence.getPatientId());
            fenceMap.put("isMultiPatient", fence.getIsMultiPatient());
            
            // 获取围栏关联的所有病人ID
            List<Long> patientIds = new ArrayList<>();
            
            // 1. 添加旧格式的patient_id
            if (fence.getPatientId() != null) {
                patientIds.add(fence.getPatientId());
            }
            
            // 2. 添加从fence_patients表获取的病人ID
            List<com.example.demo.model.FencePatient> fencePatients = fencePatientRepo.findByFenceId(fence.getId());
            for (com.example.demo.model.FencePatient fp : fencePatients) {
                if (fp.getPatient() != null && fp.getPatient().getId() != null && !patientIds.contains(fp.getPatient().getId())) {
                    patientIds.add(fp.getPatient().getId());
                }
            }
            
            fenceMap.put("patientIds", patientIds);
            response.add(fenceMap);
        }
        
        return response;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable long id) {
        int n = repo.delete(id);
        return n > 0 ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}

