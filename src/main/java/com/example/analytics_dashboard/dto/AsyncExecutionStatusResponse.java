package com.example.analytics_dashboard.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AsyncExecutionStatusResponse {
    @JsonProperty("executionId")
    private String executionId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("result")
    private List<List<Object>> result;

    @JsonProperty("error")
    private String error;

    public AsyncExecutionStatusResponse(String executionId, String status) {
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

    public List<List<Object>> getResult() {
        return result;
    }
    public void setResult(List<List<Object>> result) {
        this.result = result;
    }

    public String getError() {
        return error;
    }
    public void setError(String error) {
        this.error = error;
    }

}
