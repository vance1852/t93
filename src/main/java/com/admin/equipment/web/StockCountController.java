package com.admin.equipment.web;

import com.admin.equipment.model.StockCount;
import com.admin.equipment.model.StockCountItem;
import com.admin.equipment.repo.StockCountRepository;
import com.admin.equipment.service.StockCountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stock-counts")
public class StockCountController {

    private final StockCountRepository stockCountRepo;
    private final StockCountService stockCountService;

    public StockCountController(StockCountRepository stockCountRepo,
                                StockCountService stockCountService) {
        this.stockCountRepo = stockCountRepo;
        this.stockCountService = stockCountService;
    }

    public record CreateRequest(Long warehouseId, String remark, String createdBy) {}
    public record UpdateItemRequest(Integer actualQuantity, String remark) {}
    public record SubmitRequest(String countedBy) {}
    public record AdjustRequest(String operator) {}

    @GetMapping
    public List<StockCount> list(@RequestParam(required = false) String status,
                                 @RequestParam(required = false) Long warehouseId) {
        if (status != null) {
            return stockCountRepo.findByStatusOrderByIdDesc(status);
        }
        if (warehouseId != null) {
            return stockCountRepo.findByWarehouseIdOrderByIdDesc(warehouseId);
        }
        return stockCountRepo.findAllByOrderByIdDesc();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        StockCount count = stockCountRepo.findById(id).orElse(null);
        if (count == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "盘点单不存在"));
        }
        return ResponseEntity.ok(count);
    }

    @GetMapping("/{id}/items")
    public ResponseEntity<?> getItems(@PathVariable Long id) {
        if (!stockCountRepo.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "盘点单不存在"));
        }
        List<StockCountItem> items = stockCountService.getCountItems(id);
        return ResponseEntity.ok(items);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateRequest req) {
        if (req.warehouseId() == null) {
            return ResponseEntity.unprocessableEntity().body(Map.of("detail", "仓库ID必填"));
        }
        StockCount count = stockCountService.createStockCount(
                req.warehouseId(), req.remark(), req.createdBy());
        return ResponseEntity.status(HttpStatus.CREATED).body(count);
    }

    @PostMapping("/{id}/generate-items")
    public ResponseEntity<?> generateItems(@PathVariable Long id) {
        try {
            StockCount count = stockCountService.generateCountItems(id);
            return ResponseEntity.ok(count);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.unprocessableEntity().body(Map.of("detail", e.getMessage()));
        }
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<?> updateItem(@PathVariable Long itemId,
                                        @RequestBody UpdateItemRequest req) {
        try {
            StockCountItem item = stockCountService.updateCountItem(
                    itemId, req.actualQuantity(), req.remark());
            return ResponseEntity.ok(item);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.unprocessableEntity().body(Map.of("detail", e.getMessage()));
        }
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<?> submit(@PathVariable Long id,
                                    @RequestBody(required = false) SubmitRequest req) {
        try {
            String countedBy = req != null ? req.countedBy() : null;
            StockCount count = stockCountService.submitCount(id, countedBy);
            return ResponseEntity.ok(count);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.unprocessableEntity().body(Map.of("detail", e.getMessage()));
        }
    }

    @PostMapping("/{id}/adjust")
    public ResponseEntity<?> adjust(@PathVariable Long id,
                                    @RequestBody(required = false) AdjustRequest req) {
        try {
            String operator = req != null ? req.operator() : null;
            StockCount count = stockCountService.adjustStock(id, operator);
            return ResponseEntity.ok(count);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.unprocessableEntity().body(Map.of("detail", e.getMessage()));
        }
    }
}
