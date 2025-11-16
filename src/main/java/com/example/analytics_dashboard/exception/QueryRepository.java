package com.example.analytics_dashboard.exception;

import com.example.analytics_dashboard.model.StoredQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface QueryRepository extends JpaRepository<StoredQuery, Long> {
    // JPA will provice CRUD operations
}
