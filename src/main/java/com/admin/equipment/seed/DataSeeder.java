package com.admin.equipment.seed;

import com.admin.equipment.model.*;
import com.admin.equipment.repo.*;
import com.admin.equipment.security.PasswordUtil;
import com.admin.equipment.service.StockService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final AppUserRepository userRepo;
    private final EquipmentRepository equipmentRepo;
    private final WorkOrderRepository workOrderRepo;
    private final WarehouseRepository warehouseRepo;
    private final SparePartRepository sparePartRepo;
    private final SparePartStockRepository stockRepo;
    private final SparePartApplicabilityRepository applicabilityRepo;
    private final StockService stockService;

    @Value("${app.admin-username}")
    private String adminUsername;

    @Value("${app.admin-password}")
    private String adminPassword;

    public DataSeeder(AppUserRepository userRepo,
                      EquipmentRepository equipmentRepo,
                      WorkOrderRepository workOrderRepo,
                      WarehouseRepository warehouseRepo,
                      SparePartRepository sparePartRepo,
                      SparePartStockRepository stockRepo,
                      SparePartApplicabilityRepository applicabilityRepo,
                      StockService stockService) {
        this.userRepo = userRepo;
        this.equipmentRepo = equipmentRepo;
        this.workOrderRepo = workOrderRepo;
        this.warehouseRepo = warehouseRepo;
        this.sparePartRepo = sparePartRepo;
        this.stockRepo = stockRepo;
        this.applicabilityRepo = applicabilityRepo;
        this.stockService = stockService;
    }

    @Override
    public void run(String... args) {
        seedAdmin();
        seedEquipmentsAndOrders();
        seedWarehouses();
        seedSparePartsAndStocks();
    }

    private void seedAdmin() {
        if (!userRepo.existsByUsername(adminUsername)) {
            AppUser admin = new AppUser();
            admin.setUsername(adminUsername);
            admin.setPasswordHash(PasswordUtil.hash(adminPassword));
            admin.setDisplayName("平台管理员");
            userRepo.save(admin);
            System.out.println("已创建管理员账号");
        }
    }

    private void seedEquipmentsAndOrders() {
        if (equipmentRepo.count() > 0) {
            return;
        }
        Equipment e1 = newEquip("EQ-1001", "一号注塑机", "注塑车间A区", "robot", "normal");
        Equipment e2 = newEquip("EQ-1002", "二号空压机", "动力站", "pump", "warning");
        Equipment e3 = newEquip("EQ-1003", "主输送带", "包装车间", "conveyor", "fault");
        Equipment e4 = newEquip("EQ-1004", "冷却循环水泵", "动力站", "pump", "maintenance");
        equipmentRepo.saveAll(List.of(e1, e2, e3, e4));

        workOrderRepo.saveAll(List.of(
                newOrder(e2.getId(), "空压机压力异常巡检", "inspection", "high", "巡检发现排气压力波动，需排查", "王工", "open"),
                newOrder(e3.getId(), "输送带断带抢修", "repair", "urgent", "包装线输送带断裂，停机抢修", "李工", "in_progress"),
                newOrder(e4.getId(), "循环水泵季度保养", "maintenance", "medium", "按计划做季度保养换油", "张工", "open"),
                newOrder(e1.getId(), "注塑机模具点检", "inspection", "low", "例行模具与液压点检", "赵工", "done")
        ));
        System.out.println("已初始化设备与工单数据");
    }

    private void seedWarehouses() {
        if (warehouseRepo.count() > 0) {
            return;
        }
        Warehouse w1 = newWarehouse("WH-001", "主仓库", "厂区一楼", "张库管");
        Warehouse w2 = newWarehouse("WH-002", "动力站备件库", "动力站旁边", "刘师傅");
        warehouseRepo.saveAll(List.of(w1, w2));
        System.out.println("已初始化仓库数据");
    }

    private void seedSparePartsAndStocks() {
        if (sparePartRepo.count() > 0) {
            return;
        }

        Warehouse wh1 = warehouseRepo.findByCode("WH-001").orElse(null);
        Warehouse wh2 = warehouseRepo.findByCode("WH-002").orElse(null);
        if (wh1 == null) {
            System.out.println("仓库未初始化，跳过备件数据");
            return;
        }

        SparePart sp1 = newSparePart("SP-001", "轴承", "6205-2RS", new BigDecimal("85.00"), 20, "个", "深沟球轴承");
        SparePart sp2 = newSparePart("SP-002", "油封", "TC-30*42*7", new BigDecimal("12.50"), 50, "个", "丁腈橡胶油封");
        SparePart sp3 = newSparePart("SP-003", "输送带", "PVC-500mm宽", new BigDecimal("280.00"), 5, "米", "PVC工业输送带");
        SparePart sp4 = newSparePart("SP-004", "液压油", "46号抗磨", new BigDecimal("320.00"), 10, "桶", "20L装");
        SparePart sp5 = newSparePart("SP-005", "滤芯", "HF35010", new BigDecimal("156.00"), 15, "个", "液压油回油滤芯");
        SparePart sp6 = newSparePart("SP-006", "三角带", "B型2240", new BigDecimal("45.00"), 30, "根", "普通V带");
        SparePart sp7 = newSparePart("SP-007", "电机碳刷", "10*16*32", new BigDecimal("28.00"), 40, "副", "含铜石墨碳刷");
        SparePart sp8 = newSparePart("SP-008", "O型圈", "30*3.5", new BigDecimal("2.50"), 200, "个", "丁腈橡胶O型圈");
        sparePartRepo.saveAll(List.of(sp1, sp2, sp3, sp4, sp5, sp6, sp7, sp8));

        initStock(sp1.getId(), wh1.getId(), 30);
        initStock(sp2.getId(), wh1.getId(), 60);
        initStock(sp3.getId(), wh1.getId(), 3);
        initStock(sp4.getId(), wh1.getId(), 12);
        initStock(sp5.getId(), wh1.getId(), 18);
        initStock(sp6.getId(), wh1.getId(), 25);
        initStock(sp7.getId(), wh1.getId(), 45);
        initStock(sp8.getId(), wh1.getId(), 250);

        if (wh2 != null) {
            initStock(sp1.getId(), wh2.getId(), 10);
            initStock(sp2.getId(), wh2.getId(), 20);
            initStock(sp4.getId(), wh2.getId(), 5);
            initStock(sp5.getId(), wh2.getId(), 8);
        }

        addApplicability(sp1.getId(), null, "pump");
        addApplicability(sp1.getId(), null, "conveyor");
        addApplicability(sp1.getId(), null, "robot");
        addApplicability(sp2.getId(), null, "pump");
        addApplicability(sp3.getId(), null, "conveyor");
        addApplicability(sp4.getId(), null, "robot");
        addApplicability(sp5.getId(), null, "robot");
        addApplicability(sp6.getId(), null, "pump");
        addApplicability(sp7.getId(), null, "motor");
        addApplicability(sp8.getId(), null, "pump");

        WorkOrder repairOrder = workOrderRepo.findByStatusOrderByIdDesc("in_progress").stream()
                .filter(w -> "repair".equals(w.getType()))
                .findFirst().orElse(null);
        if (repairOrder != null) {
            stockService.issueStock(sp3.getId(), wh1.getId(), 1, repairOrder.getId(), "李工", "抢修领用输送带1米");
            stockService.issueStock(sp2.getId(), wh1.getId(), 4, repairOrder.getId(), "李工", "抢修领用密封圈");
        }

        WorkOrder maintOrder = workOrderRepo.findByStatusOrderByIdDesc("open").stream()
                .filter(w -> "maintenance".equals(w.getType()))
                .findFirst().orElse(null);
        if (maintOrder != null && wh2 != null) {
            stockService.issueStock(sp1.getId(), wh2.getId(), 2, maintOrder.getId(), "张工", "保养更换轴承");
            stockService.issueStock(sp5.getId(), wh2.getId(), 1, maintOrder.getId(), "张工", "保养更换滤芯");
        }

        WorkOrder doneOrder = workOrderRepo.findByStatusOrderByIdDesc("done").stream()
                .findFirst().orElse(null);
        if (doneOrder != null) {
            stockService.issueStock(sp1.getId(), wh1.getId(), 4, doneOrder.getId(), "赵工", "点检领用");
        }

        System.out.println("已初始化备件、库存与领用记录");
    }

    private Equipment newEquip(String code, String name, String location, String type, String status) {
        Equipment e = new Equipment();
        e.setCode(code);
        e.setName(name);
        e.setLocation(location);
        e.setType(type);
        e.setStatus(status);
        return e;
    }

    private WorkOrder newOrder(Long equipmentId, String title, String type, String priority,
                               String description, String assignee, String status) {
        WorkOrder w = new WorkOrder();
        w.setEquipmentId(equipmentId);
        w.setTitle(title);
        w.setType(type);
        w.setPriority(priority);
        w.setDescription(description);
        w.setAssignee(assignee);
        w.setStatus(status);
        return w;
    }

    private Warehouse newWarehouse(String code, String name, String location, String manager) {
        Warehouse w = new Warehouse();
        w.setCode(code);
        w.setName(name);
        w.setLocation(location);
        w.setManager(manager);
        return w;
    }

    private SparePart newSparePart(String code, String name, String specification,
                                   BigDecimal unitPrice, int safetyStock, String unit, String remark) {
        SparePart sp = new SparePart();
        sp.setCode(code);
        sp.setName(name);
        sp.setSpecification(specification);
        sp.setUnitPrice(unitPrice);
        sp.setSafetyStock(safetyStock);
        sp.setUnit(unit);
        sp.setRemark(remark);
        return sp;
    }

    private void initStock(Long sparePartId, Long warehouseId, int quantity) {
        SparePartStock stock = new SparePartStock();
        stock.setSparePartId(sparePartId);
        stock.setWarehouseId(warehouseId);
        stock.setQuantity(quantity);
        stock.setReservedQuantity(0);
        stock.setLastMovementAt(LocalDateTime.now().minusDays(7));
        stockRepo.save(stock);
    }

    private void addApplicability(Long sparePartId, Long equipmentId, String equipmentType) {
        SparePartApplicability app = new SparePartApplicability();
        app.setSparePartId(sparePartId);
        app.setEquipmentId(equipmentId);
        app.setEquipmentType(equipmentType);
        applicabilityRepo.save(app);
    }
}
