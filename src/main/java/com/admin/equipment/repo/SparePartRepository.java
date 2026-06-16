package com.admin.equipment.repo;

import com.admin.equipment.model.SparePart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SparePartRepository extends JpaRepository<SparePart, Long> {
    Optional<SparePart> findByCode(String code);
    boolean existsByCode(String code);
}
