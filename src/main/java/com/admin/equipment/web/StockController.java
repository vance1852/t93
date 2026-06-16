package com.admin.equipment.web;

import com.admin.equipment.model.SparePartStock;
import com.admin.equipment.model.StockTransaction;
import com.admin.equipment.repo.SparePartStockRepository;
import com.admin.equipment.repo.StockTransactionRepository;
import com.admin.equipment.service.StockService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stocks")
public class StockController {

    private final SparePartStockRepository stockRepo;
    private final StockTransactionRepository transactionRepo;
    private final StockService stockService;

    public StockController(SparePartStockRepository stockRepo,
                           StockTransactionRepository transactionRepo,
                           StockService stockService) {
        this.stockRepo = stockRepo;
        this.transactionRepo = transactionRepo;
        this.stockService = stockService;
    }

    @GetMapping
    public List<SparePartStock> list(@RequestParam(required = false) Long warehouseId,
                                     @RequestParam(required = false) Long sparePartId) {
        if (warehouseId != null) {
            return stockRepo.findByWarehouseId(warehouseId);
        }
        if (sparePartId != null) {
            return stockRepo.findBySparePartId(sparePartId);
        }
        return stockRepo.findAll();
    }

    @GetMapping("/{sparePartId}/{warehouseId}")
    public ResponseEntity<?> getStock(@PathVariable Long sparePartId, @PathVariable Long warehouseId) {
        SparePartStock stock = stockRepo.findBySparePartIdAndWarehouseId(sparePartId, warehouseId).orElse(null);
        if (stock == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "库存记录不存在"));
        }
        return ResponseEntity.ok(stock);
    }

    @GetMapping("/transactions")
    public List<StockTransaction> listTransactions(
            @RequestParam(required = false) Long sparePartId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long workOrderId,
            @RequestParam(required = false) Long purchaseOrderId,
            @RequestParam(required = false) String type) {
        if (sparePartId != null && warehouseId != null) {
            return transactionRepo.findBySparePartIdAndWarehouseIdOrderByCreatedAtDesc(sparePartId, warehouseId);
        }
        if (sparePartId != null) {
            return transactionRepo.findBySparePartIdOrderByCreatedAtDesc(sparePartId);
        }
        if (warehouseId != null) {
            return transactionRepo.findByWarehouseIdOrderByCreatedAtDesc(warehouseId);
        }
        if (workOrderId != null) {
            return transactionRepo.findByWorkOrderIdOrderByCreatedAtDesc(workOrderId);
        }
        if (purchaseOrderId != null) {
            return transactionRepo.findByPurchaseOrderIdOrderByCreatedAtDesc(purchaseOrderId);
        }
        if (type != null) {
            return transactionRepo.findByTypeOrderByCreatedAtDesc(type);
        }
        return transactionRepo.findAll();
    }

    @GetMapping("/available/{sparePartId}/{warehouseId}")
    public ResponseEntity<?> getAvailableQuantity(@PathVariable Long sparePartId,
                                                   @PathVariable Long warehouseId) {
        int available = stockService.getAvailableQuantity(sparePartId, warehouseId);
        return ResponseEntity.ok(Map.of(
                "sparePartId", sparePartId,
                "warehouseId", warehouseId,
                "availableQuantity", available
        ));
    }
}
