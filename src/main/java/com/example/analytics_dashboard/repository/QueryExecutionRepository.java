package com.example.analytics_dashboard.repository;

import java.util.UUID;
import com.example.analytics_dashboard.model.QueryExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QueryExecutionRepository extends JpaRepository<QueryExecution, UUID> {
}
