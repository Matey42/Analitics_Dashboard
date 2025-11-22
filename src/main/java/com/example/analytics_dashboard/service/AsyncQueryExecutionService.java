package com.example.analytics_dashboard.service;

import com.example.analytics_dashboard.model.QueryExecution;
import com.example.analytics_dashboard.model.StoredQuery;
import com.example.analytics_dashboard.repository.QueryExecutionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AsyncQueryExecutionService {
    private final QueryExecutionRepository executionRepository;
    private final QueryExecutionService queryExecutionService;
    private final ObjectMapper objectMapper;

    @Autowired
    @Lazy
    public AsyncQueryExecutionService self;

    public AsyncQueryExecutionService(
            QueryExecutionRepository executionRepository,
            QueryExecutionService queryExecutionService,
            ObjectMapper objectMapper){
        this.executionRepository = executionRepository;
        this.queryExecutionService = queryExecutionService;
        this.objectMapper = objectMapper;
    }

    public String startExecution(StoredQuery storedQuery) {
        QueryExecution execution = new QueryExecution(storedQuery.getId());
        execution = executionRepository.save(execution);

        String executionIdStr = execution.getId().toString();

        self.executeAsync(executionIdStr, storedQuery.getQueryText());

        return executionIdStr;
    }

    @Async
    @Transactional
    public void executeAsync(String executionIdStr, String queryText) {
        UUID executionId = UUID.fromString(executionIdStr);
        QueryExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new IllegalArgumentException("Execution not found"));

        try{
            execution.setStatus(QueryExecution.ExecutionStatus.RUNNING);
            executionRepository.save(execution);

            // Thread.sleep(5000) to see running status

            List<List<Object>> result = queryExecutionService.executeQuery(queryText);
            String resultJson = objectMapper.writeValueAsString(result);

            execution.setStatus(QueryExecution.ExecutionStatus.COMPLETED);
            execution.setResult(resultJson);

        }catch(Exception e){
            execution.setStatus(QueryExecution.ExecutionStatus.FAILED);
            execution.setErrorMessage(e.getMessage());
        }
        execution.setCompletedAt(LocalDateTime.now());
        executionRepository.save(execution);
    }

    @Transactional(readOnly = true)
    public QueryExecution getExecution(String executionIdStr) {
        return executionRepository.findById(UUID.fromString(executionIdStr))
                .orElseThrow(() -> new IllegalArgumentException("Execution not found"));
    }

    public List<List<Object>> parseResult(String resultJson) throws JsonProcessingException {
        if (resultJson == null) return List.of();
        return objectMapper.readValue(resultJson, List.class);
    }

}
