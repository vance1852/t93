package com.admin.equipment.web;

import com.admin.equipment.model.SparePart;
import com.admin.equipment.model.SparePartApplicability;
import com.admin.equipment.model.SparePartStock;
import com.admin.equipment.repo.SparePartApplicabilityRepository;
import com.admin.equipment.repo.SparePartRepository;
import com.admin.equipment.repo.SparePartStockRepository;
import com.admin.equipment.service.StockService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/spare-parts")
public class SparePartController {

    private final SparePartRepository sparePartRepo;
    private final SparePartApplicabilityRepository applicabilityRepo;
    private final SparePartStockRepository stockRepo;
    private final StockService stockService;

    public SparePartController(SparePartRepository sparePartRepo,
                               SparePartApplicabilityRepository applicabilityRepo,
                               SparePartStockRepository stockRepo,
                               StockService stockService) {
        this.sparePartRepo = sparePartRepo;
        this.applicabilityRepo = applicabilityRepo;
        this.stockRepo = stockRepo;
        this.stockService = stockService;
    }

    public record SparePartRequest(String code, String name, String specification,
                                   BigDecimal unitPrice, Integer safetyStock, String unit, String remark) {}

    public record ApplicabilityRequest(Long sparePartId, Long equipmentId, String equipmentType) {}

    public record StockInitRequest(Long warehouseId, Integer quantity) {}

    @GetMapping
    public List<SparePart> list() {
        return sparePartRepo.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        SparePart part = sparePartRepo.findById(id).orElse(null);
        if (part == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "备件不存在"));
        }
        return ResponseEntity.ok(part);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody SparePartRequest req) {
        if (req.code() == null || req.code().isBlank()) {
            return ResponseEntity.unprocessableEntity().body(Map.of("detail", "备件编码必填"));
        }
        if (req.name() == null || req.name().isBlank()) {
            return ResponseEntity.unprocessableEntity().body(Map.of("detail", "备件名称必填"));
        }
        if (sparePartRepo.existsByCode(req.code())) {
            return ResponseEntity.unprocessableEntity().body(Map.of("detail", "备件编码已存在"));
        }

        SparePart part = new SparePart();
        part.setCode(req.code());
        part.setName(req.name());
        part.setSpecification(req.specification() != null ? req.specification() : "");
        part.setUnitPrice(req.unitPrice() != null ? req.unitPrice() : BigDecimal.ZERO);
        part.setSafetyStock(req.safetyStock() != null ? req.safetyStock() : 0);
        part.setUnit(req.unit() != null ? req.unit() : "个");
        part.setRemark(req.remark() != null ? req.remark() : "");
        return ResponseEntity.status(HttpStatus.CREATED).body(sparePartRepo.save(part));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody SparePartRequest req) {
        SparePart part = sparePartRepo.findById(id).orElse(null);
        if (part == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "备件不存在"));
        }
        if (req.name() != null && !req.name().isBlank()) part.setName(req.name());
        if (req.specification() != null) part.setSpecification(req.specification());
        if (req.unitPrice() != null) part.setUnitPrice(req.unitPrice());
        if (req.safetyStock() != null) part.setSafetyStock(req.safetyStock());
        if (req.unit() != null) part.setUnit(req.unit());
        if (req.remark() != null) part.setRemark(req.remark());
        return ResponseEntity.ok(sparePartRepo.save(part));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!sparePartRepo.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "备件不存在"));
        }
        sparePartRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/applicability")
    public List<SparePartApplicability> getApplicability(@PathVariable Long id) {
        return applicabilityRepo.findBySparePartId(id);
    }

    @PostMapping("/applicability")
    public ResponseEntity<?> addApplicability(@RequestBody ApplicabilityRequest req) {
        if (req.sparePartId() == null) {
            return ResponseEntity.unprocessableEntity().body(Map.of("detail", "备件ID必填"));
        }
        if (!sparePartRepo.existsById(req.sparePartId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "备件不存在"));
        }
        if (req.equipmentId() == null && (req.equipmentType() == null || req.equipmentType().isBlank())) {
            return ResponseEntity.unprocessableEntity().body(Map.of("detail", "设备ID或设备类型至少填一个"));
        }

        if (req.equipmentId() != null && applicabilityRepo.existsBySparePartIdAndEquipmentId(req.sparePartId(), req.equipmentId())) {
            return ResponseEntity.unprocessableEntity().body(Map.of("detail", "该适用关系已存在"));
        }
        if (req.equipmentType() != null && applicabilityRepo.existsBySparePartIdAndEquipmentType(req.sparePartId(), req.equipmentType())) {
            return ResponseEntity.unprocessableEntity().body(Map.of("detail", "该适用关系已存在"));
        }

        SparePartApplicability app = new SparePartApplicability();
        app.setSparePartId(req.sparePartId());
        app.setEquipmentId(req.equipmentId());
        app.setEquipmentType(req.equipmentType());
        return ResponseEntity.status(HttpStatus.CREATED).body(applicabilityRepo.save(app));
    }

    @DeleteMapping("/applicability/{id}")
    public ResponseEntity<?> removeApplicability(@PathVariable Long id) {
        if (!applicabilityRepo.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "适用关系不存在"));
        }
        applicabilityRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/stocks")
    public List<SparePartStock> getStocks(@PathVariable Long id) {
        return stockRepo.findBySparePartId(id);
    }

    @PostMapping("/{id}/stocks")
    public ResponseEntity<?> initStock(@PathVariable Long id, @RequestBody StockInitRequest req) {
        if (!sparePartRepo.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "备件不存在"));
        }
        if (req.warehouseId() == null) {
            return ResponseEntity.unprocessableEntity().body(Map.of("detail", "仓库ID必填"));
        }
        SparePartStock stock = stockService.getOrCreateStock(id, req.warehouseId());
        if (req.quantity() != null) {
            stock.setQuantity(req.quantity());
            stockRepo.save(stock);
        }
        return ResponseEntity.ok(stock);
    }
}
