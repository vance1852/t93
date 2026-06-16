package com.admin.equipment.service;

import com.admin.equipment.model.*;
import com.admin.equipment.repo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class StockCountService {

    private final StockCountRepository stockCountRepo;
    private final StockCountItemRepository stockCountItemRepo;
    private final SparePartStockRepository stockRepo;
    private final StockService stockService;
    private final AtomicInteger countCounter = new AtomicInteger(0);

    public StockCountService(StockCountRepository stockCountRepo,
                             StockCountItemRepository stockCountItemRepo,
                             SparePartStockRepository stockRepo,
                             StockService stockService) {
        this.stockCountRepo = stockCountRepo;
        this.stockCountItemRepo = stockCountItemRepo;
        this.stockRepo = stockRepo;
        this.stockService = stockService;
    }

    private String generateCountCode() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int seq = countCounter.incrementAndGet();
        return String.format("SC%s%04d", dateStr, seq);
    }

    @Transactional
    public StockCount createStockCount(Long warehouseId, String remark, String createdBy) {
        StockCount count = new StockCount();
        count.setCode(generateCountCode());
        count.setWarehouseId(warehouseId);
        count.setStatus("draft");
        count.setRemark(remark != null ? remark : "");
        count.setCreatedBy(createdBy != null ? createdBy : "");
        return stockCountRepo.save(count);
    }

    @Transactional
    public StockCount generateCountItems(Long countId) {
        StockCount count = stockCountRepo.findById(countId)
                .orElseThrow(() -> new IllegalArgumentException("盘点单不存在"));

        if (!"draft".equals(count.getStatus())) {
            throw new IllegalStateException("只有草稿状态可以生成盘点明细");
        }

        stockCountItemRepo.deleteByStockCountId(countId);

        List<SparePartStock> stocks = stockRepo.findByWarehouseId(count.getWarehouseId());
        for (SparePartStock stock : stocks) {
            StockCountItem item = new StockCountItem();
            item.setStockCountId(countId);
            item.setSparePartId(stock.getSparePartId());
            item.setSystemQuantity(stock.getQuantity());
            item.setActualQuantity(null);
            item.setDiffQuantity(0);
            stockCountItemRepo.save(item);
        }

        return count;
    }

    @Transactional
    public StockCountItem updateCountItem(Long itemId, Integer actualQuantity, String remark) {
        StockCountItem item = stockCountItemRepo.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("盘点明细不存在"));

        StockCount count = stockCountRepo.findById(item.getStockCountId())
                .orElseThrow(() -> new IllegalArgumentException("盘点单不存在"));

        if (!"draft".equals(count.getStatus())) {
            throw new IllegalStateException("只有草稿状态可以修改盘点明细");
        }

        item.setActualQuantity(actualQuantity);
        if (actualQuantity != null && item.getSystemQuantity() != null) {
            item.setDiffQuantity(actualQuantity - item.getSystemQuantity());
        }
        if (remark != null) {
            item.setRemark(remark);
        }
        return stockCountItemRepo.save(item);
    }

    @Transactional
    public StockCount submitCount(Long countId, String countedBy) {
        StockCount count = stockCountRepo.findById(countId)
                .orElseThrow(() -> new IllegalArgumentException("盘点单不存在"));

        if (!"draft".equals(count.getStatus())) {
            throw new IllegalStateException("只有草稿状态可以提交");
        }

        List<StockCountItem> items = stockCountItemRepo.findByStockCountId(countId);
        for (StockCountItem item : items) {
            if (item.getActualQuantity() == null) {
                throw new IllegalStateException("存在未录入实盘数量的明细，请先完成盘点");
            }
        }

        count.setStatus("counted");
        count.setCountedBy(countedBy != null ? countedBy : "");
        count.setCountedAt(LocalDateTime.now());
        return stockCountRepo.save(count);
    }

    @Transactional
    public StockCount adjustStock(Long countId, String operator) {
        StockCount count = stockCountRepo.findById(countId)
                .orElseThrow(() -> new IllegalArgumentException("盘点单不存在"));

        if (!"counted".equals(count.getStatus())) {
            throw new IllegalStateException("只有已盘点状态可以调整库存");
        }

        List<StockCountItem> items = stockCountItemRepo.findByStockCountId(countId);
        for (StockCountItem item : items) {
            if (item.getDiffQuantity() != null && item.getDiffQuantity() != 0) {
                stockService.adjustStock(
                        item.getSparePartId(),
                        count.getWarehouseId(),
                        item.getDiffQuantity(),
                        countId,
                        operator,
                        "盘点调整：" + item.getRemark()
                );
            }
        }

        count.setStatus("adjusted");
        count.setAdjustedAt(LocalDateTime.now());
        return stockCountRepo.save(count);
    }

    public List<StockCountItem> getCountItems(Long countId) {
        return stockCountItemRepo.findByStockCountId(countId);
    }
}
