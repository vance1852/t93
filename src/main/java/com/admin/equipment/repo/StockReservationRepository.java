package com.admin.equipment.repo;

import com.admin.equipment.model.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {
    List<StockReservation> findByWorkOrderId(Long workOrderId);
    List<StockReservation> findBySparePartIdAndWarehouseIdAndStatus(Long sparePartId, Long warehouseId, String status);
    List<StockReservation> findByStatusAndExpiresAtBefore(String status, LocalDateTime expiresAt);
    List<StockReservation> findByWorkOrderIdAndSparePartIdAndWarehouseId(Long workOrderId, Long sparePartId, Long warehouseId);
}
