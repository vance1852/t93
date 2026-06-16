package com.admin.equipment.repo;

import com.admin.equipment.model.SparePartApplicability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SparePartApplicabilityRepository extends JpaRepository<SparePartApplicability, Long> {
    List<SparePartApplicability> findBySparePartId(Long sparePartId);
    List<SparePartApplicability> findByEquipmentId(Long equipmentId);
    List<SparePartApplicability> findByEquipmentType(String equipmentType);
    void deleteBySparePartIdAndEquipmentId(Long sparePartId, Long equipmentId);
    void deleteBySparePartIdAndEquipmentType(Long sparePartId, String equipmentType);
    boolean existsBySparePartIdAndEquipmentId(Long sparePartId, Long equipmentId);
    boolean existsBySparePartIdAndEquipmentType(Long sparePartId, String equipmentType);
}
