package com.example.analytics_dashboard.repository;

import com.example.analytics_dashboard.model.StoredQuery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QueryRepository extends JpaRepository<StoredQuery, Long> {
    // JPA will provice CRUD operations
}
