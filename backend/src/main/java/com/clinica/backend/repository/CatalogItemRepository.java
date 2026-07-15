package com.clinica.backend.repository;

import com.clinica.backend.model.CatalogItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CatalogItemRepository extends JpaRepository<CatalogItem, Long> {
    List<CatalogItem> findByCatalogIdOrderByOrderIndexAsc(Long catalogId);
    List<CatalogItem> findByCatalogCodeOrderByOrderIndexAsc(String catalogCode);
    List<CatalogItem> findByCatalogCodeAndIsActiveTrueOrderByOrderIndexAsc(String catalogCode);
}
