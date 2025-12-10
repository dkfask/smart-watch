package com.example.demo.controller.api;

import com.example.demo.model.Patient;
import com.example.demo.repository.PatientRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/patients")
public class PatientController {
    private final PatientRepository repo;

    public PatientController(PatientRepository repo) { this.repo = repo; }

    // 创建病人
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Patient patient) {
        Patient saved = repo.save(patient);
        Long id = saved.getId();
        return ResponseEntity.created(URI.create("/api/patients/" + id)).body(saved);
    }

    // 获取病人列表
    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        // 使用Pageable实现分页
        Pageable pageable = Pageable.ofSize(size).withPage(page);
        
        Page<Patient> patientPage;
        if (search != null && !search.isEmpty()) {
            // 实现按姓名、ID、身份证号等条件搜索
            patientPage = repo.findByNameContainingOrIdCardContainingOrWardContaining(search, search, search, pageable);
        } else {
            patientPage = repo.findAll(pageable);
        }
        
        // 构造响应对象，包含数据和总数
        return ResponseEntity.ok(new PageResponse<>(patientPage.getContent(), patientPage.getTotalElements()));
    }
    
    // 内部类用于封装分页响应
    public static class PageResponse<T> {
        private List<T> data;
        private long total;
        
        public PageResponse(List<T> data, long total) {
            this.data = data;
            this.total = total;
        }
        
        public List<T> getData() { return data; }
        public void setData(List<T> data) { this.data = data; }
        public long getTotal() { return total; }
        public void setTotal(long total) { this.total = total; }
    }

    // 获取病人详情
    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable long id) {
        Optional<Patient> patient = repo.findById(id);
        return patient.<ResponseEntity<?>>map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // 更新病人信息
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable long id, @RequestBody Patient patient) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        patient.setId(id);
        Patient updated = repo.save(patient);
        return ResponseEntity.ok(updated);
    }

    // 删除病人
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable long id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
