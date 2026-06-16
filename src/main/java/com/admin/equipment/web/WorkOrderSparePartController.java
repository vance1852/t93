package com.admin.equipment.web;

import com.admin.equipment.model.StockReservation;
import com.admin.equipment.model.StockTransaction;
import com.admin.equipment.repo.WorkOrderRepository;
import com.admin.equipment.service.ReservationService;
import com.admin.equipment.service.StockService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/work-orders/{workOrderId}/spare-parts")
public class WorkOrderSparePartController {

    private final WorkOrderRepository workOrderRepo;
    private final StockService stockService;
    private final ReservationService reservationService;

    public WorkOrderSparePartController(WorkOrderRepository workOrderRepo,
                                        StockService stockService,
                                        ReservationService reservationService) {
        this.workOrderRepo = workOrderRepo;
        this.stockService = stockService;
        this.reservationService = reservationService;
    }

    public record IssueRequest(Long sparePartId, Long warehouseId, Integer quantity, String operator, String remark) {}
    public record ReturnRequest(Long sparePartId, Long warehouseId, Integer quantity, String operator, String remark) {}
    public record ReserveRequest(Long sparePartId, Long warehouseId, Integer quantity,
                                 LocalDateTime expiresAt, String operator) {}
    public record FulfillRequest(Long reservationId, Integer quantity, String operator) {}

    private boolean workOrderExists(Long workOrderId) {
        return workOrderRepo.existsById(workOrderId);
    }

    @PostMapping("/issue")
    public ResponseEntity<?> issue(@PathVariable Long workOrderId, @RequestBody IssueRequest req) {
        if (!workOrderExists(workOrderId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "工单不存在"));
        }
        if (req.sparePartId() == null || req.warehouseId() == null || req.quantity() == null) {
            return ResponseEntity.unprocessableEntity().body(Map.of("detail", "备件ID、仓库ID、数量必填"));
        }
        try {
            StockTransaction tx = stockService.issueStock(
                    req.sparePartId(), req.warehouseId(), req.quantity(),
                    workOrderId, req.operator() != null ? req.operator() : "",
                    req.remark());
            return ResponseEntity.ok(tx);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.unprocessableEntity().body(Map.of("detail", e.getMessage()));
        }
    }

    @PostMapping("/return")
    public ResponseEntity<?> returnStock(@PathVariable Long workOrderId, @RequestBody ReturnRequest req) {
        if (!workOrderExists(workOrderId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "工单不存在"));
        }
        if (req.sparePartId() == null || req.warehouseId() == null || req.quantity() == null) {
            return ResponseEntity.unprocessableEntity().body(Map.of("detail", "备件ID、仓库ID、数量必填"));
        }
        try {
            StockTransaction tx = stockService.returnStock(
                    req.sparePartId(), req.warehouseId(), req.quantity(),
                    workOrderId, req.operator() != null ? req.operator() : "",
                    req.remark());
            return ResponseEntity.ok(tx);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.unprocessableEntity().body(Map.of("detail", e.getMessage()));
        }
    }

    @PostMapping("/reserve")
    public ResponseEntity<?> reserve(@PathVariable Long workOrderId, @RequestBody ReserveRequest req) {
        if (!workOrderExists(workOrderId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "工单不存在"));
        }
        if (req.sparePartId() == null || req.warehouseId() == null || req.quantity() == null) {
            return ResponseEntity.unprocessableEntity().body(Map.of("detail", "备件ID、仓库ID、数量必填"));
        }
        try {
            StockReservation reservation = reservationService.createReservation(
                    workOrderId, req.sparePartId(), req.warehouseId(), req.quantity(),
                    req.expiresAt());
            return ResponseEntity.status(HttpStatus.CREATED).body(reservation);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.unprocessableEntity().body(Map.of("detail", e.getMessage()));
        }
    }

    @PostMapping("/fulfill")
    public ResponseEntity<?> fulfill(@PathVariable Long workOrderId, @RequestBody FulfillRequest req) {
        if (!workOrderExists(workOrderId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "工单不存在"));
        }
        if (req.reservationId() == null || req.quantity() == null) {
            return ResponseEntity.unprocessableEntity().body(Map.of("detail", "预留ID、数量必填"));
        }
        try {
            StockTransaction tx = reservationService.fulfillReservation(
                    req.reservationId(), req.quantity(),
                    req.operator() != null ? req.operator() : "");
            return ResponseEntity.ok(tx);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.unprocessableEntity().body(Map.of("detail", e.getMessage()));
        }
    }

    @GetMapping("/reservations")
    public ResponseEntity<?> listReservations(@PathVariable Long workOrderId) {
        if (!workOrderExists(workOrderId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "工单不存在"));
        }
        List<StockReservation> reservations = reservationService.getWorkOrderReservations(workOrderId);
        return ResponseEntity.ok(reservations);
    }

    @DeleteMapping("/reservations/{reservationId}")
    public ResponseEntity<?> cancelReservation(@PathVariable Long workOrderId,
                                                @PathVariable Long reservationId) {
        if (!workOrderExists(workOrderId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "工单不存在"));
        }
        try {
            reservationService.cancelReservation(reservationId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", e.getMessage()));
        }
    }
}
