package com.admin.equipment.repo;

import com.admin.equipment.model.SparePartStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface SparePartStockRepository extends JpaRepository<SparePartStock, Long> {
    Optional<SparePartStock> findBySparePartIdAndWarehouseId(Long sparePartId, Long warehouseId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SparePartStock s WHERE s.sparePartId = :sparePartId AND s.warehouseId = :warehouseId")
    Optional<SparePartStock> findBySparePartIdAndWarehouseIdWithLock(Long sparePartId, Long warehouseId);

    List<SparePartStock> findByWarehouseId(Long warehouseId);

    List<SparePartStock> findBySparePartId(Long sparePartId);

    @Query("SELECT s FROM SparePartStock s WHERE s.quantity < s.sparePartId")
    List<SparePartStock> findLowStockItems();
}
