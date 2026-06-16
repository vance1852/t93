package com.admin.equipment.web;

import com.admin.equipment.model.Warehouse;
import com.admin.equipment.repo.WarehouseRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/warehouses")
public class WarehouseController {

    private final WarehouseRepository repo;

    public WarehouseController(WarehouseRepository repo) {
        this.repo = repo;
    }

    public record WarehouseRequest(String code, String name, String location, String manager) {}

    @GetMapping
    public List<Warehouse> list() {
        return repo.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        Warehouse w = repo.findById(id).orElse(null);
        if (w == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "仓库不存在"));
        }
        return ResponseEntity.ok(w);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody WarehouseRequest req) {
        if (req.code() == null || req.code().isBlank()) {
            return ResponseEntity.unprocessableEntity().body(Map.of("detail", "仓库编码必填"));
        }
        if (req.name() == null || req.name().isBlank()) {
            return ResponseEntity.unprocessableEntity().body(Map.of("detail", "仓库名称必填"));
        }
        if (repo.existsByCode(req.code())) {
            return ResponseEntity.unprocessableEntity().body(Map.of("detail", "仓库编码已存在"));
        }

        Warehouse w = new Warehouse();
        w.setCode(req.code());
        w.setName(req.name());
        w.setLocation(req.location() != null ? req.location() : "");
        w.setManager(req.manager() != null ? req.manager() : "");
        return ResponseEntity.status(HttpStatus.CREATED).body(repo.save(w));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody WarehouseRequest req) {
        Warehouse w = repo.findById(id).orElse(null);
        if (w == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "仓库不存在"));
        }
        if (req.name() != null && !req.name().isBlank()) {
            w.setName(req.name());
        }
        if (req.location() != null) {
            w.setLocation(req.location());
        }
        if (req.manager() != null) {
            w.setManager(req.manager());
        }
        return ResponseEntity.ok(repo.save(w));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!repo.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "仓库不存在"));
        }
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
