package com.admin.equipment.repo;

import com.admin.equipment.model.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    Optional<PurchaseOrder> findByCode(String code);
    boolean existsByCode(String code);
    List<PurchaseOrder> findByStatusOrderByIdDesc(String status);
    List<PurchaseOrder> findAllByOrderByIdDesc();
}
