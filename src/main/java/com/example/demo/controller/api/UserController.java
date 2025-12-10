package com.example.demo.controller.api;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserRepository repo;

    public UserController(UserRepository repo) { this.repo = repo; }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody User u) {
        User saved = repo.save(u);
        return ResponseEntity.created(URI.create("/api/users/" + saved.getId())).body(saved.getId());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable long id) {
        Optional<User> u = repo.findById(id);
        return u.<ResponseEntity<?>>map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<User> list(@RequestParam(defaultValue = "20") int limit,
                           @RequestParam(defaultValue = "0") int offset) {
        // 使用CrudRepository的findAll()方法，然后手动分页
        return StreamSupport.stream(repo.findAll().spliterator(), false)
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable long id, @RequestBody User u) {
        if (!repo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        u.setId(id);
        repo.save(u);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable long id) {
        if (!repo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

