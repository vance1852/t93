package com.admin.equipment.service;

import com.admin.equipment.model.*;
import com.admin.equipment.repo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class StockService {

    private final SparePartStockRepository stockRepo;
    private final SparePartRepository sparePartRepo;
    private final WarehouseRepository warehouseRepo;
    private final StockTransactionRepository transactionRepo;

    public StockService(SparePartStockRepository stockRepo,
                        SparePartRepository sparePartRepo,
                        WarehouseRepository warehouseRepo,
                        StockTransactionRepository transactionRepo) {
        this.stockRepo = stockRepo;
        this.sparePartRepo = sparePartRepo;
        this.warehouseRepo = warehouseRepo;
        this.transactionRepo = transactionRepo;
    }

    @Transactional
    public SparePartStock getOrCreateStock(Long sparePartId, Long warehouseId) {
        return stockRepo.findBySparePartIdAndWarehouseId(sparePartId, warehouseId)
                .orElseGet(() -> {
                    SparePartStock stock = new SparePartStock();
                    stock.setSparePartId(sparePartId);
                    stock.setWarehouseId(warehouseId);
                    stock.setQuantity(0);
                    stock.setReservedQuantity(0);
                    return stockRepo.save(stock);
                });
    }

    @Transactional
    public StockTransaction issueStock(Long sparePartId, Long warehouseId, int quantity,
                                       Long workOrderId, String operator, String remark) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("领用数量必须大于0");
        }

        SparePartStock stock = stockRepo.findBySparePartIdAndWarehouseIdWithLock(sparePartId, warehouseId)
                .orElseThrow(() -> new IllegalArgumentException("库存记录不存在"));

        int availableQty = stock.getQuantity() - stock.getReservedQuantity();
        if (availableQty < quantity) {
            throw new IllegalStateException(
                String.format("库存不足，可用库存：%d，需要：%d", availableQty, quantity));
        }

        int beforeQty = stock.getQuantity();
        stock.setQuantity(beforeQty - quantity);
        stock.setLastMovementAt(LocalDateTime.now());
        stockRepo.save(stock);

        SparePart part = sparePartRepo.findById(sparePartId).orElse(null);
        BigDecimal unitPrice = part != null ? part.getUnitPrice() : BigDecimal.ZERO;

        StockTransaction tx = new StockTransaction();
        tx.setSparePartId(sparePartId);
        tx.setWarehouseId(warehouseId);
        tx.setType("issue");
        tx.setQuantity(quantity);
        tx.setBeforeQuantity(beforeQty);
        tx.setAfterQuantity(stock.getQuantity());
        tx.setUnitPrice(unitPrice);
        tx.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(quantity)));
        tx.setWorkOrderId(workOrderId);
        tx.setOperator(operator);
        tx.setRemark(remark != null ? remark : "工单领用");
        return transactionRepo.save(tx);
    }

    @Transactional
    public StockTransaction returnStock(Long sparePartId, Long warehouseId, int quantity,
                                        Long workOrderId, String operator, String remark) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("退库数量必须大于0");
        }

        SparePartStock stock = stockRepo.findBySparePartIdAndWarehouseIdWithLock(sparePartId, warehouseId)
                .orElseGet(() -> {
                    SparePartStock s = new SparePartStock();
                    s.setSparePartId(sparePartId);
                    s.setWarehouseId(warehouseId);
                    s.setQuantity(0);
                    s.setReservedQuantity(0);
                    return stockRepo.save(s);
                });

        int beforeQty = stock.getQuantity();
        stock.setQuantity(beforeQty + quantity);
        stock.setLastMovementAt(LocalDateTime.now());
        stockRepo.save(stock);

        SparePart part = sparePartRepo.findById(sparePartId).orElse(null);
        BigDecimal unitPrice = part != null ? part.getUnitPrice() : BigDecimal.ZERO;

        StockTransaction tx = new StockTransaction();
        tx.setSparePartId(sparePartId);
        tx.setWarehouseId(warehouseId);
        tx.setType("return");
        tx.setQuantity(quantity);
        tx.setBeforeQuantity(beforeQty);
        tx.setAfterQuantity(stock.getQuantity());
        tx.setUnitPrice(unitPrice);
        tx.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(quantity)));
        tx.setWorkOrderId(workOrderId);
        tx.setOperator(operator);
        tx.setRemark(remark != null ? remark : "工单退库");
        return transactionRepo.save(tx);
    }

    @Transactional
    public StockTransaction stockIn(Long sparePartId, Long warehouseId, int quantity,
                                    Long purchaseOrderId, BigDecimal unitPrice,
                                    String operator, String remark) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("入库数量必须大于0");
        }

        SparePartStock stock = stockRepo.findBySparePartIdAndWarehouseIdWithLock(sparePartId, warehouseId)
                .orElseGet(() -> {
                    SparePartStock s = new SparePartStock();
                    s.setSparePartId(sparePartId);
                    s.setWarehouseId(warehouseId);
                    s.setQuantity(0);
                    s.setReservedQuantity(0);
                    return stockRepo.save(s);
                });

        int beforeQty = stock.getQuantity();
        stock.setQuantity(beforeQty + quantity);
        stock.setLastMovementAt(LocalDateTime.now());
        stockRepo.save(stock);

        BigDecimal price = unitPrice != null ? unitPrice : BigDecimal.ZERO;

        StockTransaction tx = new StockTransaction();
        tx.setSparePartId(sparePartId);
        tx.setWarehouseId(warehouseId);
        tx.setType("in");
        tx.setQuantity(quantity);
        tx.setBeforeQuantity(beforeQty);
        tx.setAfterQuantity(stock.getQuantity());
        tx.setUnitPrice(price);
        tx.setTotalPrice(price.multiply(BigDecimal.valueOf(quantity)));
        tx.setPurchaseOrderId(purchaseOrderId);
        tx.setOperator(operator);
        tx.setRemark(remark != null ? remark : "采购入库");
        return transactionRepo.save(tx);
    }

    @Transactional
    public StockTransaction adjustStock(Long sparePartId, Long warehouseId, int diffQuantity,
                                        Long stockCountId, String operator, String remark) {
        SparePartStock stock = stockRepo.findBySparePartIdAndWarehouseIdWithLock(sparePartId, warehouseId)
                .orElseThrow(() -> new IllegalArgumentException("库存记录不存在"));

        int beforeQty = stock.getQuantity();
        int afterQty = beforeQty + diffQuantity;
        if (afterQty < 0) {
            throw new IllegalStateException("调整后库存不能为负");
        }

        stock.setQuantity(afterQty);
        stock.setLastMovementAt(LocalDateTime.now());
        stockRepo.save(stock);

        SparePart part = sparePartRepo.findById(sparePartId).orElse(null);
        BigDecimal unitPrice = part != null ? part.getUnitPrice() : BigDecimal.ZERO;

        StockTransaction tx = new StockTransaction();
        tx.setSparePartId(sparePartId);
        tx.setWarehouseId(warehouseId);
        tx.setType("adjust");
        tx.setQuantity(Math.abs(diffQuantity));
        tx.setBeforeQuantity(beforeQty);
        tx.setAfterQuantity(afterQty);
        tx.setUnitPrice(unitPrice);
        tx.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(Math.abs(diffQuantity))));
        tx.setStockCountId(stockCountId);
        tx.setOperator(operator);
        tx.setRemark(remark != null ? remark : "盘点调整");
        return transactionRepo.save(tx);
    }

    public int getAvailableQuantity(Long sparePartId, Long warehouseId) {
        Optional<SparePartStock> stockOpt = stockRepo.findBySparePartIdAndWarehouseId(sparePartId, warehouseId);
        if (stockOpt.isEmpty()) {
            return 0;
        }
        SparePartStock stock = stockOpt.get();
        return stock.getQuantity() - stock.getReservedQuantity();
    }
}
