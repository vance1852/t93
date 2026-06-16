package com.admin.equipment.web;

import com.admin.equipment.service.StatsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/consumption")
    public List<StatsService.ConsumptionStats> getConsumptionStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return statsService.getConsumptionStats(startDate, endDate);
    }

    @GetMapping("/work-order-cost")
    public List<StatsService.WorkOrderCostStats> getWorkOrderCostStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return statsService.getWorkOrderCostStats(startDate, endDate);
    }

    @GetMapping("/cost-analysis")
    public StatsService.CostAnalysis getCostAnalysis(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return statsService.getCostAnalysis(startDate, endDate);
    }

    @GetMapping("/dead-stock")
    public List<StatsService.DeadStockItem> getDeadStock(
            @RequestParam(defaultValue = "90") int idleDays) {
        return statsService.getDeadStockItems(idleDays);
    }
}
