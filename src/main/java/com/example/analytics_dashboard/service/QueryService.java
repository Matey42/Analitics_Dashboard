package com.example.analytics_dashboard.service;

import com.example.analytics_dashboard.dto.QueryListResponse;
import com.example.analytics_dashboard.dto.QueryResponse;
import com.example.analytics_dashboard.model.StoredQuery;
import com.example.analytics_dashboard.repository.QueryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class QueryService {
    private final QueryRepository queryRepository;

    public QueryService(QueryRepository queryRepository) {
        this.queryRepository = queryRepository;
    }

    @Transactional
    public QueryResponse addQuery(String queryText){
        if(queryText == null || queryText.trim().isEmpty()){
            throw new IllegalArgumentException("Query text cannot be null or empty");
        }

        StoredQuery storedQuery = new StoredQuery(queryText.trim());
        StoredQuery savedQuery = queryRepository.save(storedQuery);

        return new QueryResponse(savedQuery.getId());
    }

    @Transactional(readOnly = true)
    public List<QueryListResponse> getAllQueries(){
        return queryRepository.findAll()
                .stream()
                .map(q -> new QueryListResponse(q.getId(), q.getQueryText()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StoredQuery getQueryById(Long id){
        return queryRepository.findById(id)
                .orElseThrow( () -> new IllegalArgumentException("Query with id " + id + " not found"));
    }


}
