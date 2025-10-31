package com.example.demo.controller.api;

import com.example.demo.model.GeoFence;
import com.example.demo.repository.GeoFenceRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/fences")
public class GeoFenceController {
    private final GeoFenceRepository repo;

    public GeoFenceController(GeoFenceRepository repo) { this.repo = repo; }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody GeoFence f) {
        long id = repo.create(f);
        return ResponseEntity.created(URI.create("/api/fences/" + id)).body(id);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable long id, @RequestBody GeoFence f) {
        f.setFenceId(id);
        int n = repo.update(f);
        return n > 0 ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable long id) {
        Optional<GeoFence> f = repo.findById(id);
        return f.<ResponseEntity<?>>map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/by-user/{userId}")
    public List<GeoFence> listByUser(@PathVariable long userId) {
        return repo.listByUser(userId);
    }

    @GetMapping("/by-user/{userId}/active")
    public List<GeoFence> listActiveByUser(@PathVariable long userId) {
        return repo.listActiveByUser(userId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable long id) {
        int n = repo.delete(id);
        return n > 0 ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}

