package com.admin.equipment.service;

import com.admin.equipment.model.*;
import com.admin.equipment.repo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class PurchaseService {

    private final PurchaseOrderRepository purchaseOrderRepo;
    private final PurchaseOrderItemRepository purchaseOrderItemRepo;
    private final SparePartRepository sparePartRepo;
    private final SparePartStockRepository stockRepo;
    private final StockService stockService;
    private final AtomicInteger poCounter = new AtomicInteger(0);

    public PurchaseService(PurchaseOrderRepository purchaseOrderRepo,
                           PurchaseOrderItemRepository purchaseOrderItemRepo,
                           SparePartRepository sparePartRepo,
                           SparePartStockRepository stockRepo,
                           StockService stockService) {
        this.purchaseOrderRepo = purchaseOrderRepo;
        this.purchaseOrderItemRepo = purchaseOrderItemRepo;
        this.sparePartRepo = sparePartRepo;
        this.stockRepo = stockRepo;
        this.stockService = stockService;
    }

    private String generatePoCode() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int seq = poCounter.incrementAndGet();
        return String.format("PO%s%04d", dateStr, seq);
    }

    public static class PurchaseSuggestion {
        public Long sparePartId;
        public String sparePartCode;
        public String sparePartName;
        public int currentStock;
        public int safetyStock;
        public int suggestedQuantity;
        public Long defaultWarehouseId;

        public PurchaseSuggestion(Long sparePartId, String sparePartCode, String sparePartName,
                                  int currentStock, int safetyStock, int suggestedQuantity,
                                  Long defaultWarehouseId) {
            this.sparePartId = sparePartId;
            this.sparePartCode = sparePartCode;
            this.sparePartName = sparePartName;
            this.currentStock = currentStock;
            this.safetyStock = safetyStock;
            this.suggestedQuantity = suggestedQuantity;
            this.defaultWarehouseId = defaultWarehouseId;
        }
    }

    public List<PurchaseSuggestion> generatePurchaseSuggestions(Long warehouseId) {
        List<PurchaseSuggestion> suggestions = new ArrayList<>();
        List<SparePart> spareParts = sparePartRepo.findAll();

        for (SparePart part : spareParts) {
            if (part.getSafetyStock() == null || part.getSafetyStock() <= 0) {
                continue;
            }

            SparePartStock stock;
            if (warehouseId != null) {
                stock = stockRepo.findBySparePartIdAndWarehouseId(part.getId(), warehouseId).orElse(null);
            } else {
                List<SparePartStock> stocks = stockRepo.findBySparePartId(part.getId());
                stock = stocks.isEmpty() ? null : stocks.get(0);
            }

            int currentQty = stock != null ? stock.getQuantity() : 0;
            if (currentQty < part.getSafetyStock()) {
                int suggestedQty = part.getSafetyStock() * 2 - currentQty;
                Long whId = stock != null ? stock.getWarehouseId() : 1L;
                suggestions.add(new PurchaseSuggestion(
                        part.getId(), part.getCode(), part.getName(),
                        currentQty, part.getSafetyStock(), suggestedQty, whId));
            }
        }
        return suggestions;
    }

    public static class ForecastRequest {
        public LocalDateTime startDate;
        public LocalDateTime endDate;
        public Long equipmentId;
        public String equipmentType;
    }

    public List<PurchaseSuggestion> generateForecastSuggestions(ForecastRequest request) {
        List<PurchaseSuggestion> suggestions = new ArrayList<>();
        return suggestions;
    }

    @Transactional
    public PurchaseOrder createPurchaseOrder(List<PurchaseOrderItem> items,
                                             String supplier, String remark, String createdBy) {
        PurchaseOrder po = new PurchaseOrder();
        po.setCode(generatePoCode());
        po.setStatus("draft");
        po.setSupplier(supplier != null ? supplier : "");
        po.setRemark(remark != null ? remark : "");
        po.setCreatedBy(createdBy != null ? createdBy : "");
        po = purchaseOrderRepo.save(po);

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (PurchaseOrderItem item : items) {
            item.setPurchaseOrderId(po.getId());
            item.setReceivedQuantity(0);
            if (item.getUnitPrice() == null) {
                item.setUnitPrice(BigDecimal.ZERO);
            }
            if (item.getTotalPrice() == null) {
                item.setTotalPrice(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            }
            purchaseOrderItemRepo.save(item);
            totalAmount = totalAmount.add(item.getTotalPrice());
        }

        po.setTotalAmount(totalAmount);
        return purchaseOrderRepo.save(po);
    }

    @Transactional
    public PurchaseOrder approvePurchaseOrder(Long poId, String approver) {
        PurchaseOrder po = purchaseOrderRepo.findById(poId)
                .orElseThrow(() -> new IllegalArgumentException("采购单不存在"));

        if (!"draft".equals(po.getStatus())) {
            throw new IllegalStateException("只有草稿状态的采购单可以审批");
        }

        po.setStatus("approved");
        po.setApprovedBy(approver != null ? approver : "");
        po.setApprovedAt(LocalDateTime.now());
        return purchaseOrderRepo.save(po);
    }

    @Transactional
    public PurchaseOrder startPurchasing(Long poId) {
        PurchaseOrder po = purchaseOrderRepo.findById(poId)
                .orElseThrow(() -> new IllegalArgumentException("采购单不存在"));

        if (!"approved".equals(po.getStatus())) {
            throw new IllegalStateException("只有已审批的采购单可以开始采购");
        }

        po.setStatus("purchasing");
        return purchaseOrderRepo.save(po);
    }

    @Transactional
    public PurchaseOrder receivePurchaseOrderItem(Long poId, Long itemId, int receivedQty,
                                                  Long warehouseId, String operator) {
        PurchaseOrder po = purchaseOrderRepo.findById(poId)
                .orElseThrow(() -> new IllegalArgumentException("采购单不存在"));

        if (!"purchasing".equals(po.getStatus())) {
            throw new IllegalStateException("只有采购中的采购单可以入库");
        }

        PurchaseOrderItem item = purchaseOrderItemRepo.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("采购明细不存在"));

        if (!item.getPurchaseOrderId().equals(poId)) {
            throw new IllegalArgumentException("采购明细不属于该采购单");
        }

        int remaining = item.getQuantity() - item.getReceivedQuantity();
        if (receivedQty > remaining) {
            throw new IllegalStateException(
                String.format("入库数量超出未收数量，剩余：%d，本次：%d", remaining, receivedQty));
        }

        Long whId = warehouseId != null ? warehouseId : item.getWarehouseId();
        if (whId == null) {
            whId = 1L;
        }

        stockService.stockIn(item.getSparePartId(), whId, receivedQty,
                poId, item.getUnitPrice(), operator, "采购入库");

        item.setReceivedQuantity(item.getReceivedQuantity() + receivedQty);
        purchaseOrderItemRepo.save(item);

        List<PurchaseOrderItem> allItems = purchaseOrderItemRepo.findByPurchaseOrderId(poId);
        boolean allReceived = allItems.stream().allMatch(i -> i.getReceivedQuantity() >= i.getQuantity());
        if (allReceived) {
            po.setStatus("received");
            po.setReceivedAt(LocalDateTime.now());
            purchaseOrderRepo.save(po);
        }

        return po;
    }

    public List<PurchaseOrderItem> getPurchaseOrderItems(Long poId) {
        return purchaseOrderItemRepo.findByPurchaseOrderId(poId);
    }
}
