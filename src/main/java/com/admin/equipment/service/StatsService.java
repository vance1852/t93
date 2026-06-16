package com.admin.equipment.service;

import com.admin.equipment.model.*;
import com.admin.equipment.repo.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class StatsService {

    private final StockTransactionRepository transactionRepo;
    private final SparePartRepository sparePartRepo;
    private final SparePartStockRepository stockRepo;

    public StatsService(StockTransactionRepository transactionRepo,
                        SparePartRepository sparePartRepo,
                        SparePartStockRepository stockRepo) {
        this.transactionRepo = transactionRepo;
        this.sparePartRepo = sparePartRepo;
        this.stockRepo = stockRepo;
    }

    public static class ConsumptionStats {
        public Long sparePartId;
        public String sparePartCode;
        public String sparePartName;
        public int totalQuantity;
        public BigDecimal totalAmount;

        public ConsumptionStats(Long sparePartId, String sparePartCode, String sparePartName,
                                int totalQuantity, BigDecimal totalAmount) {
            this.sparePartId = sparePartId;
            this.sparePartCode = sparePartCode;
            this.sparePartName = sparePartName;
            this.totalQuantity = totalQuantity;
            this.totalAmount = totalAmount;
        }
    }

    public List<ConsumptionStats> getConsumptionStats(LocalDateTime startDate, LocalDateTime endDate) {
        List<Object[]> results = transactionRepo.getConsumptionStatsByDateRange(startDate, endDate);
        List<ConsumptionStats> stats = new ArrayList<>();

        for (Object[] row : results) {
            Long sparePartId = (Long) row[0];
            Long totalQty = (Long) row[1];
            BigDecimal totalAmount = (BigDecimal) row[2];

            SparePart part = sparePartRepo.findById(sparePartId).orElse(null);
            String code = part != null ? part.getCode() : "";
            String name = part != null ? part.getName() : "";

            stats.add(new ConsumptionStats(
                    sparePartId, code, name,
                    totalQty != null ? totalQty.intValue() : 0,
                    totalAmount != null ? totalAmount : BigDecimal.ZERO));
        }

        stats.sort((a, b) -> Integer.compare(b.totalQuantity, a.totalQuantity));
        return stats;
    }

    public static class WorkOrderCostStats {
        public Long workOrderId;
        public int itemCount;
        public BigDecimal totalCost;

        public WorkOrderCostStats(Long workOrderId, int itemCount, BigDecimal totalCost) {
            this.workOrderId = workOrderId;
            this.itemCount = itemCount;
            this.totalCost = totalCost;
        }
    }

    public List<WorkOrderCostStats> getWorkOrderCostStats(LocalDateTime startDate, LocalDateTime endDate) {
        List<Object[]> results = transactionRepo.getWorkOrderCostStatsByDateRange(startDate, endDate);
        List<WorkOrderCostStats> stats = new ArrayList<>();

        for (Object[] row : results) {
            Long workOrderId = (Long) row[0];
            Long itemCount = (Long) row[1];
            BigDecimal totalCost = (BigDecimal) row[2];

            stats.add(new WorkOrderCostStats(
                    workOrderId,
                    itemCount != null ? itemCount.intValue() : 0,
                    totalCost != null ? totalCost : BigDecimal.ZERO));
        }
        return stats;
    }

    public static class LowStockWarning {
        public Long sparePartId;
        public String sparePartCode;
        public String sparePartName;
        public Long warehouseId;
        public String warehouseName;
        public int currentStock;
        public int safetyStock;
        public int shortage;

        public LowStockWarning(Long sparePartId, String sparePartCode, String sparePartName,
                               Long warehouseId, String warehouseName,
                               int currentStock, int safetyStock, int shortage) {
            this.sparePartId = sparePartId;
            this.sparePartCode = sparePartCode;
            this.sparePartName = sparePartName;
            this.warehouseId = warehouseId;
            this.warehouseName = warehouseName;
            this.currentStock = currentStock;
            this.safetyStock = safetyStock;
            this.shortage = shortage;
        }
    }

    public List<LowStockWarning> getLowStockWarnings() {
        List<LowStockWarning> warnings = new ArrayList<>();
        List<SparePart> allParts = sparePartRepo.findAll();

        for (SparePart part : allParts) {
            if (part.getSafetyStock() == null || part.getSafetyStock() <= 0) {
                continue;
            }
            List<SparePartStock> stocks = stockRepo.findBySparePartId(part.getId());
            for (SparePartStock stock : stocks) {
                if (stock.getQuantity() < part.getSafetyStock()) {
                    warnings.add(new LowStockWarning(
                            part.getId(), part.getCode(), part.getName(),
                            stock.getWarehouseId(), "",
                            stock.getQuantity(), part.getSafetyStock(),
                            part.getSafetyStock() - stock.getQuantity()));
                }
            }
        }
        warnings.sort((a, b) -> Integer.compare(b.shortage, a.shortage));
        return warnings;
    }

    public static class DeadStockItem {
        public Long sparePartId;
        public String sparePartCode;
        public String sparePartName;
        public Long warehouseId;
        public int quantity;
        public BigDecimal stockValue;
        public LocalDateTime lastMovementAt;
        public long daysIdle;

        public DeadStockItem(Long sparePartId, String sparePartCode, String sparePartName,
                             Long warehouseId, int quantity, BigDecimal stockValue,
                             LocalDateTime lastMovementAt, long daysIdle) {
            this.sparePartId = sparePartId;
            this.sparePartCode = sparePartCode;
            this.sparePartName = sparePartName;
            this.warehouseId = warehouseId;
            this.quantity = quantity;
            this.stockValue = stockValue;
            this.lastMovementAt = lastMovementAt;
            this.daysIdle = daysIdle;
        }
    }

    public List<DeadStockItem> getDeadStockItems(int idleDaysThreshold) {
        List<DeadStockItem> deadStocks = new ArrayList<>();
        LocalDateTime thresholdDate = LocalDateTime.now().minusDays(idleDaysThreshold);
        List<SparePartStock> allStocks = stockRepo.findAll();

        for (SparePartStock stock : allStocks) {
            if (stock.getQuantity() <= 0) {
                continue;
            }
            LocalDateTime lastMove = stock.getLastMovementAt();
            if (lastMove == null) {
                lastMove = stock.getCreatedAt();
            }
            if (lastMove.isBefore(thresholdDate)) {
                SparePart part = sparePartRepo.findById(stock.getSparePartId()).orElse(null);
                if (part != null) {
                    long daysIdle = java.time.Duration.between(lastMove, LocalDateTime.now()).toDays();
                    BigDecimal stockValue = part.getUnitPrice().multiply(BigDecimal.valueOf(stock.getQuantity()));
                    deadStocks.add(new DeadStockItem(
                            part.getId(), part.getCode(), part.getName(),
                            stock.getWarehouseId(), stock.getQuantity(),
                            stockValue, lastMove, daysIdle));
                }
            }
        }
        deadStocks.sort((a, b) -> Long.compare(b.daysIdle, a.daysIdle));
        return deadStocks;
    }

    public static class CostAnalysis {
        public BigDecimal totalIssueAmount;
        public BigDecimal totalInAmount;
        public int totalIssueQty;
        public int totalInQty;
        public BigDecimal avgUnitPrice;

        public CostAnalysis(BigDecimal totalIssueAmount, BigDecimal totalInAmount,
                            int totalIssueQty, int totalInQty, BigDecimal avgUnitPrice) {
            this.totalIssueAmount = totalIssueAmount;
            this.totalInAmount = totalInAmount;
            this.totalIssueQty = totalIssueQty;
            this.totalInQty = totalInQty;
            this.avgUnitPrice = avgUnitPrice;
        }
    }

    public CostAnalysis getCostAnalysis(LocalDateTime startDate, LocalDateTime endDate) {
        List<StockTransaction> issueTxns = transactionRepo.findIssueTransactionsByDateRange(startDate, endDate);

        BigDecimal totalIssueAmount = BigDecimal.ZERO;
        int totalIssueQty = 0;

        for (StockTransaction tx : issueTxns) {
            if (tx.getTotalPrice() != null) {
                totalIssueAmount = totalIssueAmount.add(tx.getTotalPrice());
            }
            totalIssueQty += tx.getQuantity();
        }

        BigDecimal avgPrice = totalIssueQty > 0
                ? totalIssueAmount.divide(BigDecimal.valueOf(totalIssueQty), 2, BigDecimal.ROUND_HALF_UP)
                : BigDecimal.ZERO;

        return new CostAnalysis(totalIssueAmount, BigDecimal.ZERO, totalIssueQty, 0, avgPrice);
    }
}
