package com.clinica.backend.repository;

import com.clinica.backend.model.InventoryTransaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {
    List<InventoryTransaction> findBySupplyIdOrderByTransactionDateDesc(Long supplyId);

    @Query("SELECT t FROM InventoryTransaction t JOIN FETCH t.supply s WHERE s.deleted = false AND s.specialty = :specialty ORDER BY t.transactionDate DESC")
    List<InventoryTransaction> findRecentBySpecialty(@Param("specialty") String specialty, Pageable pageable);
}
