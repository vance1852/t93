package com.admin.equipment.repo;

import com.admin.equipment.model.StockCount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockCountRepository extends JpaRepository<StockCount, Long> {
    Optional<StockCount> findByCode(String code);
    boolean existsByCode(String code);
    List<StockCount> findByWarehouseIdOrderByIdDesc(Long warehouseId);
    List<StockCount> findByStatusOrderByIdDesc(String status);
    List<StockCount> findAllByOrderByIdDesc();
}
