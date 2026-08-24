package com.clinica.backend.repository;

import com.clinica.backend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Collection;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    @Query("SELECT u FROM User u WHERE " +
           "(cast(:query as string) IS NULL OR cast(:query as string) = '' OR " +
           " LOWER(u.username) LIKE LOWER(CONCAT('%', cast(:query as string), '%')) OR " +
           " LOWER(u.firstName) LIKE LOWER(CONCAT('%', cast(:query as string), '%')) OR " +
           " LOWER(u.lastName) LIKE LOWER(CONCAT('%', cast(:query as string), '%')) OR " +
           " LOWER(u.email) LIKE LOWER(CONCAT('%', cast(:query as string), '%'))) AND " +
           "(cast(:specialty as string) IS NULL OR cast(:specialty as string) = '' OR u.specialty = :specialty) AND " +
           "(:enabled IS NULL OR u.enabled = :enabled)")
    Page<User> searchUsers(@Param("query") String query,
                           @Param("specialty") String specialty,
                           @Param("enabled") Boolean enabled,
                           Pageable pageable);

    java.util.List<User> findBySpecialtyAndEnabledTrueOrderByFirstNameAscLastNameAsc(String specialty);

    List<User> findByIdIn(Collection<Long> ids);
}
