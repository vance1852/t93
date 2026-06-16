package com.admin.equipment.web;

import com.admin.equipment.model.PurchaseOrder;
import com.admin.equipment.model.PurchaseOrderItem;
import com.admin.equipment.repo.PurchaseOrderRepository;
import com.admin.equipment.service.PurchaseService;
import com.admin.equipment.service.StatsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/purchase-orders")
public class PurchaseOrderController {

    private final PurchaseOrderRepository purchaseOrderRepo;
    private final PurchaseService purchaseService;
    private final StatsService statsService;

    public PurchaseOrderController(PurchaseOrderRepository purchaseOrderRepo,
                                   PurchaseService purchaseService,
                                   StatsService statsService) {
        this.purchaseOrderRepo = purchaseOrderRepo;
        this.purchaseService = purchaseService;
        this.statsService = statsService;
    }

    public record PoItemRequest(Long sparePartId, Integer quantity, BigDecimal unitPrice,
                                Long warehouseId, String remark) {}

    public record PoCreateRequest(List<PoItemRequest> items, String supplier,
                                  String remark, String createdBy) {}

    public record PoReceiveRequest(Long itemId, Integer quantity, Long warehouseId, String operator) {}

    @GetMapping
    public List<PurchaseOrder> list(@RequestParam(required = false) String status) {
        if (status != null) {
            return purchaseOrderRepo.findByStatusOrderByIdDesc(status);
        }
        return purchaseOrderRepo.findAllByOrderByIdDesc();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        PurchaseOrder po = purchaseOrderRepo.findById(id).orElse(null);
        if (po == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "采购单不存在"));
        }
        return ResponseEntity.ok(po);
    }

    @GetMapping("/{id}/items")
    public ResponseEntity<?> getItems(@PathVariable Long id) {
        if (!purchaseOrderRepo.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "采购单不存在"));
        }
        List<PurchaseOrderItem> items = purchaseService.getPurchaseOrderItems(id);
        return ResponseEntity.ok(items);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody PoCreateRequest req) {
        if (req.items() == null || req.items().isEmpty()) {
            return ResponseEntity.unprocessableEntity().body(Map.of("detail", "采购明细不能为空"));
        }

        try {
            List<PurchaseOrderItem> items = req.items().stream()
                    .map(itemReq -> {
                        PurchaseOrderItem item = new PurchaseOrderItem();
                        item.setSparePartId(itemReq.sparePartId());
                        item.setQuantity(itemReq.quantity());
                        item.setUnitPrice(itemReq.unitPrice() != null ? itemReq.unitPrice() : BigDecimal.ZERO);
                        item.setWarehouseId(itemReq.warehouseId());
                        item.setRemark(itemReq.remark() != null ? itemReq.remark() : "");
                        return item;
                    }).toList();

            PurchaseOrder po = purchaseService.createPurchaseOrder(
                    items, req.supplier(), req.remark(), req.createdBy());
            return ResponseEntity.status(HttpStatus.CREATED).body(po);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.unprocessableEntity().body(Map.of("detail", e.getMessage()));
        }
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable Long id,
                                     @RequestBody(required = false) Map<String, String> body) {
        try {
            String approver = body != null ? body.get("approver") : null;
            PurchaseOrder po = purchaseService.approvePurchaseOrder(id, approver);
            return ResponseEntity.ok(po);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.unprocessableEntity().body(Map.of("detail", e.getMessage()));
        }
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<?> startPurchasing(@PathVariable Long id) {
        try {
            PurchaseOrder po = purchaseService.startPurchasing(id);
            return ResponseEntity.ok(po);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.unprocessableEntity().body(Map.of("detail", e.getMessage()));
        }
    }

    @PostMapping("/{id}/receive")
    public ResponseEntity<?> receiveItem(@PathVariable Long id,
                                         @RequestBody PoReceiveRequest req) {
        if (req.itemId() == null || req.quantity() == null) {
            return ResponseEntity.unprocessableEntity().body(Map.of("detail", "明细ID和数量必填"));
        }
        try {
            PurchaseOrder po = purchaseService.receivePurchaseOrderItem(
                    id, req.itemId(), req.quantity(), req.warehouseId(),
                    req.operator() != null ? req.operator() : "");
            return ResponseEntity.ok(po);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.unprocessableEntity().body(Map.of("detail", e.getMessage()));
        }
    }

    @GetMapping("/suggestions/low-stock")
    public List<PurchaseService.PurchaseSuggestion> getLowStockSuggestions(
            @RequestParam(required = false) Long warehouseId) {
        return purchaseService.generatePurchaseSuggestions(warehouseId);
    }

    @GetMapping("/suggestions/forecast")
    public List<PurchaseService.ForecastSuggestion> getForecastSuggestions(
            @RequestParam(defaultValue = "30") int forecastDays,
            @RequestParam(defaultValue = "90") int historyDays,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String equipmentType,
            @RequestParam(required = false) Long equipmentId) {
        PurchaseService.ForecastRequest req = new PurchaseService.ForecastRequest();
        req.forecastDays = forecastDays;
        req.historyDays = historyDays;
        req.warehouseId = warehouseId;
        req.equipmentType = equipmentType;
        req.equipmentId = equipmentId;
        return purchaseService.generateForecastSuggestions(req);
    }

    @GetMapping("/warnings/low-stock")
    public List<StatsService.LowStockWarning> getLowStockWarnings() {
        return statsService.getLowStockWarnings();
    }
}
