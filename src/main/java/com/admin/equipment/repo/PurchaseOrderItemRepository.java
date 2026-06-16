package com.admin.equipment.repo;

import com.admin.equipment.model.PurchaseOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Long> {
    List<PurchaseOrderItem> findByPurchaseOrderId(Long purchaseOrderId);
    void deleteByPurchaseOrderId(Long purchaseOrderId);
}
