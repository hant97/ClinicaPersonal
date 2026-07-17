package com.clinica.backend.repository;

import com.clinica.backend.model.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {
    List<InventoryTransaction> findBySupplyIdOrderByTransactionDateDesc(Long supplyId);
}
