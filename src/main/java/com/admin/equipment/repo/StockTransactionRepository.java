package com.admin.equipment.repo;

import com.admin.equipment.model.StockTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface StockTransactionRepository extends JpaRepository<StockTransaction, Long> {
    List<StockTransaction> findBySparePartIdOrderByCreatedAtDesc(Long sparePartId);
    List<StockTransaction> findByWarehouseIdOrderByCreatedAtDesc(Long warehouseId);
    List<StockTransaction> findBySparePartIdAndWarehouseIdOrderByCreatedAtDesc(Long sparePartId, Long warehouseId);
    List<StockTransaction> findByWorkOrderIdOrderByCreatedAtDesc(Long workOrderId);
    List<StockTransaction> findByPurchaseOrderIdOrderByCreatedAtDesc(Long purchaseOrderId);
    List<StockTransaction> findByTypeOrderByCreatedAtDesc(String type);

    @Query("SELECT t FROM StockTransaction t WHERE t.type = 'issue' AND t.createdAt >= :startDate AND t.createdAt < :endDate ORDER BY t.createdAt DESC")
    List<StockTransaction> findIssueTransactionsByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT t FROM StockTransaction t WHERE t.type = 'in' AND t.createdAt >= :startDate AND t.createdAt < :endDate ORDER BY t.createdAt DESC")
    List<StockTransaction> findInboundTransactionsByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT t.sparePartId, SUM(t.quantity) as totalQty, SUM(t.totalPrice) as totalAmount " +
           "FROM StockTransaction t WHERE t.type = 'issue' AND t.createdAt >= :startDate AND t.createdAt < :endDate " +
           "GROUP BY t.sparePartId")
    List<Object[]> getConsumptionStatsByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT t.workOrderId, COUNT(t.id), SUM(t.totalPrice) " +
           "FROM StockTransaction t WHERE t.type = 'issue' AND t.createdAt >= :startDate AND t.createdAt < :endDate " +
           "GROUP BY t.workOrderId")
    List<Object[]> getWorkOrderCostStatsByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
