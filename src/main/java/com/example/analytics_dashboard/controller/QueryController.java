package com.example.analytics_dashboard.controller;

import com.example.analytics_dashboard.dto.QueryListResponse;
import com.example.analytics_dashboard.dto.QueryRequest;
import com.example.analytics_dashboard.dto.QueryResponse;
import com.example.analytics_dashboard.service.QueryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/queries")
public class QueryController {
    private final QueryService queryService;

    public QueryController(QueryService queryService) {
        this.queryService = queryService;
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

}
