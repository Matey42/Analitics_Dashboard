package com.example.analytics_dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class QueryResponse {
    @JsonProperty("id")
    private Long id;

    public QueryResponse() {}

    public QueryResponse(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
