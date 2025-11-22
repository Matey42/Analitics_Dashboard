package com.example.analytics_dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AsyncExecutionResponse {
    @JsonProperty("executionId")
    private String executionId;

    @JsonProperty("status")
    private String status;

    public AsyncExecutionResponse() {}

    public AsyncExecutionResponse(String executionId, String status) {
        this.executionId = executionId;
        this.status = status;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
