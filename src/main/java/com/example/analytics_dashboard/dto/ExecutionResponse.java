package com.example.analytics_dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ExecutionResponse {
    @JsonProperty("result")
    private List<List<Object>>  result;

    public ExecutionResponse() {}

    public ExecutionResponse(List<List<Object>> result) {
        this.result = result;
    }

    public List<List<Object>> getResult() {
        return result;
    }

    public void setResult(List<List<Object>> result) {
        this.result = result;
    }
}
