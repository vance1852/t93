package com.admin.equipment.service;

import com.admin.equipment.model.*;
import com.admin.equipment.repo.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ReservationService {

    private final StockReservationRepository reservationRepo;
    private final SparePartStockRepository stockRepo;
    private final SparePartRepository sparePartRepo;
    private final StockTransactionRepository transactionRepo;

    public ReservationService(StockReservationRepository reservationRepo,
                              SparePartStockRepository stockRepo,
                              SparePartRepository sparePartRepo,
                              StockTransactionRepository transactionRepo) {
        this.reservationRepo = reservationRepo;
        this.stockRepo = stockRepo;
        this.sparePartRepo = sparePartRepo;
        this.transactionRepo = transactionRepo;
    }

    @Transactional
    public StockReservation createReservation(Long workOrderId, Long sparePartId,
                                              Long warehouseId, int quantity,
                                              LocalDateTime expiresAt) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("预留数量必须大于0");
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

        int availableQty = stock.getQuantity() - stock.getReservedQuantity();
        if (availableQty < quantity) {
            throw new IllegalStateException(
                String.format("可用库存不足，可用：%d，需要预留：%d", availableQty, quantity));
        }

        stock.setReservedQuantity(stock.getReservedQuantity() + quantity);
        stockRepo.save(stock);

        StockReservation reservation = new StockReservation();
        reservation.setWorkOrderId(workOrderId);
        reservation.setSparePartId(sparePartId);
        reservation.setWarehouseId(warehouseId);
        reservation.setQuantity(quantity);
        reservation.setFulfilledQuantity(0);
        reservation.setStatus("active");
        reservation.setExpiresAt(expiresAt != null ? expiresAt : LocalDateTime.now().plusHours(24));
        return reservationRepo.save(reservation);
    }

    @Transactional
    public StockTransaction fulfillReservation(Long reservationId, int quantity, String operator) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("领用数量必须大于0");
        }

        StockReservation reservation = reservationRepo.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("预留记录不存在"));

        if (!"active".equals(reservation.getStatus())) {
            throw new IllegalStateException("预留状态不是活跃状态");
        }

        int remainingQty = reservation.getQuantity() - reservation.getFulfilledQuantity();
        if (remainingQty < quantity) {
            throw new IllegalStateException(
                String.format("预留剩余数量不足，剩余：%d，需要：%d", remainingQty, quantity));
        }

        SparePartStock stock = stockRepo.findBySparePartIdAndWarehouseIdWithLock(
                reservation.getSparePartId(), reservation.getWarehouseId())
                .orElseThrow(() -> new IllegalArgumentException("库存记录不存在"));

        if (stock.getQuantity() < quantity) {
            throw new IllegalStateException("实际库存不足");
        }

        int beforeQty = stock.getQuantity();
        stock.setQuantity(beforeQty - quantity);
        stock.setReservedQuantity(stock.getReservedQuantity() - quantity);
        stock.setLastMovementAt(LocalDateTime.now());
        stockRepo.save(stock);

        reservation.setFulfilledQuantity(reservation.getFulfilledQuantity() + quantity);
        if (reservation.getFulfilledQuantity() >= reservation.getQuantity()) {
            reservation.setStatus("fulfilled");
        }
        reservationRepo.save(reservation);

        SparePart part = sparePartRepo.findById(reservation.getSparePartId()).orElse(null);
        BigDecimal unitPrice = part != null ? part.getUnitPrice() : BigDecimal.ZERO;

        StockTransaction tx = new StockTransaction();
        tx.setSparePartId(reservation.getSparePartId());
        tx.setWarehouseId(reservation.getWarehouseId());
        tx.setType("issue");
        tx.setQuantity(quantity);
        tx.setBeforeQuantity(beforeQty);
        tx.setAfterQuantity(stock.getQuantity());
        tx.setUnitPrice(unitPrice);
        tx.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(quantity)));
        tx.setWorkOrderId(reservation.getWorkOrderId());
        tx.setReservationId(reservationId);
        tx.setOperator(operator);
        tx.setRemark("预留核销领用");
        return transactionRepo.save(tx);
    }

    @Transactional
    public void cancelReservation(Long reservationId) {
        StockReservation reservation = reservationRepo.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("预留记录不存在"));

        if ("cancelled".equals(reservation.getStatus()) || "fulfilled".equals(reservation.getStatus())) {
            return;
        }

        int remainingReserved = reservation.getQuantity() - reservation.getFulfilledQuantity();
        if (remainingReserved > 0) {
            Optional<SparePartStock> stockOpt = stockRepo.findBySparePartIdAndWarehouseIdWithLock(
                    reservation.getSparePartId(), reservation.getWarehouseId());
            if (stockOpt.isPresent()) {
                SparePartStock stock = stockOpt.get();
                int newReserved = stock.getReservedQuantity() - remainingReserved;
                stock.setReservedQuantity(Math.max(0, newReserved));
                stockRepo.save(stock);
            }
        }

        reservation.setStatus("cancelled");
        reservationRepo.save(reservation);
    }

    @Transactional
    public void cancelWorkOrderReservations(Long workOrderId) {
        List<StockReservation> reservations = reservationRepo.findByWorkOrderId(workOrderId);
        for (StockReservation r : reservations) {
            if ("active".equals(r.getStatus())) {
                cancelReservation(r.getId());
            }
        }
    }

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void releaseExpiredReservations() {
        LocalDateTime now = LocalDateTime.now();
        List<StockReservation> expired = reservationRepo.findByStatusAndExpiresAtBefore("active", now);
        for (StockReservation r : expired) {
            try {
                cancelReservation(r.getId());
            } catch (Exception e) {
                // log and continue
            }
        }
    }

    public List<StockReservation> getWorkOrderReservations(Long workOrderId) {
        return reservationRepo.findByWorkOrderId(workOrderId);
    }
}
