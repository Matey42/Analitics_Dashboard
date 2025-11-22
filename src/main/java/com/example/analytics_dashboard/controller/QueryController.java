package com.example.analytics_dashboard.controller;

import com.example.analytics_dashboard.dto.*;
import com.example.analytics_dashboard.model.StoredQuery;
import com.example.analytics_dashboard.service.AsyncQueryExecutionService;
import com.example.analytics_dashboard.service.QueryExecutionService;
import com.example.analytics_dashboard.service.QueryService;
import com.example.analytics_dashboard.model.QueryExecution;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/queries")
public class QueryController {
    private final QueryService queryService;
    private final QueryExecutionService executionService;
    private final AsyncQueryExecutionService asyncExecutionService;


    public QueryController(QueryService queryService,
                           QueryExecutionService executionService,
                           AsyncQueryExecutionService asyncExecutionService) {
        this.queryService = queryService;
        this.executionService = executionService;
        this.asyncExecutionService = asyncExecutionService;
    }

    @PostMapping
    public ResponseEntity<QueryResponse> addQuery(@RequestBody QueryRequest queryRequest) {
        QueryResponse response = queryService.addQuery(queryRequest.getQuery());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<QueryListResponse>> getAllQueries() {
        List<QueryListResponse> queries = queryService.getAllQueries();
        return ResponseEntity.ok(queries);
    }

    @GetMapping("/execute")
    public ResponseEntity<ExecutionResponse> executeQuery(@RequestParam("query") Long queryId) {
        StoredQuery storedQuery = queryService.getQueryById(queryId);

        if(!executionService.isReadOnlyQuery(storedQuery.getQueryText())){
            throw new IllegalArgumentException("Only SELECT queries are allowed");
        }

        List<List<Object>> result = executionService.executeQuery(storedQuery.getQueryText());
        ExecutionResponse response = new ExecutionResponse(result);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/execute/async")
    public ResponseEntity<AsyncExecutionResponse> executeQueryAsync(@RequestParam("query") Long queryId) {
        StoredQuery storedQuery = queryService.getQueryById(queryId);

        if(!executionService.isReadOnlyQuery(storedQuery.getQueryText())){
            throw new IllegalArgumentException("Only SELECT queries are allowed");
        }

        String executionId = asyncExecutionService.startExecution((storedQuery));

        return ResponseEntity.accepted()
                .body(new AsyncExecutionResponse(executionId, "PENDING"));
    }

    @GetMapping("/execute/async/{executionId}")
    public ResponseEntity<AsyncExecutionStatusResponse> getExecutionStatus(@PathVariable("executionId") String executionId) {
        QueryExecution execution = asyncExecutionService.getExecution(executionId);

        AsyncExecutionStatusResponse response = new AsyncExecutionStatusResponse(
                execution.getId().toString(),
                execution.getStatus().toString()
        );

        if (execution.getStatus() == QueryExecution.ExecutionStatus.COMPLETED) {
            try {
                response.setResult(asyncExecutionService.parseResult(execution.getResult()));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error parsing result", e);
            }
        } else if (execution.getStatus() == QueryExecution.ExecutionStatus.FAILED) {
            response.setError(execution.getErrorMessage());
        }

        return ResponseEntity.ok(response);
    }

}
