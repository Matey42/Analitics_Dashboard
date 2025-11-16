package com.example.analytics_dashboard.controller;

import com.example.analytics_dashboard.dto.ExecutionResponse;
import com.example.analytics_dashboard.dto.QueryListResponse;
import com.example.analytics_dashboard.dto.QueryRequest;
import com.example.analytics_dashboard.dto.QueryResponse;
import com.example.analytics_dashboard.model.StoredQuery;
import com.example.analytics_dashboard.service.QueryExecutionService;
import com.example.analytics_dashboard.service.QueryService;
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

    public QueryController(QueryService queryService, QueryExecutionService executionService) {
        this.queryService = queryService;
        this.executionService = executionService;
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
    public ResponseEntity<List<List<Object>>> executeQuery(@RequestParam("query") Long queryId) {
        StoredQuery storedQuery = queryService.getQueryById(queryId);

        if(!executionService.isReadOnlyQuery(storedQuery.getQueryText())){
            throw new IllegalArgumentException("Only Select queries are allowed");
        }

        List<List<Object>> result = executionService.executeQuery(storedQuery.getQueryText());

        // ExecutionResponse response = new ExecutionResponse(result);
        return ResponseEntity.ok(result);
    }

}
