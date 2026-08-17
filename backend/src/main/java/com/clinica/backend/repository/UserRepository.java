package com.clinica.backend.repository;

import com.clinica.backend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    @Query("SELECT u FROM User u WHERE " +
           "(:query IS NULL OR :query = '' OR " +
           " LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(u.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(u.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:specialty IS NULL OR :specialty = '' OR u.specialty = :specialty) AND " +
           "(:enabled IS NULL OR u.enabled = :enabled)")
    Page<User> searchUsers(@Param("query") String query,
                           @Param("specialty") String specialty,
                           @Param("enabled") Boolean enabled,
                           Pageable pageable);
}
