package com.admin.equipment.repo;

import com.admin.equipment.model.StockCountItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockCountItemRepository extends JpaRepository<StockCountItem, Long> {
    List<StockCountItem> findByStockCountId(Long stockCountId);
    void deleteByStockCountId(Long stockCountId);
}
