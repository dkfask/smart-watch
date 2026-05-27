package com.example.demo.controller.api;

import com.example.demo.model.Patient;
import com.example.demo.model.dto.PageResponse;
import com.example.demo.repository.PatientRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Optional;

@RestController
@RequestMapping("/api/patients")
public class PatientController {
    private final PatientRepository repo;

    public PatientController(PatientRepository repo) { this.repo = repo; }

    /**
     * 创建病人
     */
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Patient patient) {
        Patient saved = repo.save(patient);
        Long id = saved.getId();
        return ResponseEntity.created(URI.create("/api/patients/" + id)).body(saved);
    }

    /**
     * 获取病人列表（分页，兼容page/size和limit/offset参数）
     */
    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset) {

        int effectivePage = (page != null) ? page : ((offset != null && limit != null) ? offset / limit : 0);
        int effectiveSize = (size != null) ? size : (limit != null ? limit : 20);
        Pageable pageable = Pageable.ofSize(effectiveSize).withPage(effectivePage);

        Page<Patient> patientPage;
        if (search != null && !search.isEmpty()) {
            patientPage = repo.findByNameContainingOrIdCardContainingOrWardContaining(search, search, search, pageable);
        } else {
            patientPage = repo.findAll(pageable);
        }

        return ResponseEntity.ok(PageResponse.from(patientPage));
    }

    /**
     * 获取病人详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable long id) {
        Optional<Patient> patient = repo.findById(id);
        return patient.<ResponseEntity<?>>map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * 更新病人信息
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable long id, @RequestBody Patient patient) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        patient.setId(id);
        Patient updated = repo.save(patient);
        return ResponseEntity.ok(updated);
    }

    /**
     * 删除病人
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable long id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
