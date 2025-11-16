package com.example.analytics_dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class QueryListResponse {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("query")
    private String query;

    public QueryListResponse() {}

    public QueryListResponse(Long id, String query) {
        this.id = id;
        this.query = query;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }


}
