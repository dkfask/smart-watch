package com.example.demo.controller.api;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserRepository repo;

    public UserController(UserRepository repo) { this.repo = repo; }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody User u) {
        long id = repo.create(u);
        return ResponseEntity.created(URI.create("/api/users/" + id)).body(id);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable long id) {
        Optional<User> u = repo.findById(id);
        return u.<ResponseEntity<?>>map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<User> list(@RequestParam(defaultValue = "20") int limit,
                           @RequestParam(defaultValue = "0") int offset) {
        return repo.list(limit, offset);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable long id, @RequestBody User u) {
        u.setUserId(id);
        int n = repo.update(u);
        return n > 0 ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable long id) {
        int n = repo.delete(id);
        return n > 0 ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}

