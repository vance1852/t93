package com.admin.equipment.model;

import jakarta.persistence.*;

@Entity
@Table(name = "stock_count_items")
public class StockCountItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_count_id", nullable = false)
    private Long stockCountId;

    @Column(name = "spare_part_id", nullable = false)
    private Long sparePartId;

    @Column(name = "system_quantity")
    private Integer systemQuantity;

    @Column(name = "actual_quantity")
    private Integer actualQuantity;

    @Column(name = "diff_quantity")
    private Integer diffQuantity = 0;

    @Column(length = 256)
    private String remark = "";

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getStockCountId() { return stockCountId; }
    public void setStockCountId(Long stockCountId) { this.stockCountId = stockCountId; }
    public Long getSparePartId() { return sparePartId; }
    public void setSparePartId(Long sparePartId) { this.sparePartId = sparePartId; }
    public Integer getSystemQuantity() { return systemQuantity; }
    public void setSystemQuantity(Integer systemQuantity) { this.systemQuantity = systemQuantity; }
    public Integer getActualQuantity() { return actualQuantity; }
    public void setActualQuantity(Integer actualQuantity) { this.actualQuantity = actualQuantity; }
    public Integer getDiffQuantity() { return diffQuantity; }
    public void setDiffQuantity(Integer diffQuantity) { this.diffQuantity = diffQuantity; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
