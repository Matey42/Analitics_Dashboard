package com.example.analytics_dashboard.integration;

import com.example.analytics_dashboard.dto.AsyncExecutionResponse;
import com.example.analytics_dashboard.dto.QueryRequest;
import com.example.analytics_dashboard.dto.QueryResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class QueryExecutorIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fullFlow_addListAndExecuteQuery_shouldWork() throws Exception {
        // Step 1: Add a query
        QueryRequest request = new QueryRequest("SELECT * FROM passengers LIMIT 3");
        String requestJson = objectMapper.writeValueAsString(request);

        MvcResult addResult = mockMvc.perform(post("/queries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn();

        String responseJson = addResult.getResponse().getContentAsString();
        QueryResponse queryResponse = objectMapper.readValue(responseJson, QueryResponse.class);
        Long queryId = queryResponse.getId();

        // Step 2: List all queries and verify it's there
        mockMvc.perform(get("/queries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + queryId + ")].query").value("SELECT * FROM passengers LIMIT 3"));

        // Step 3: Execute the query
        mockMvc.perform(get("/queries/execute")
                        .param("query", queryId.toString()))
                .andExpect(status().isOk())
                // Pamiętaj: teraz wynik jest w obiekcie { "result": [...] }
                .andExpect(jsonPath("$.result").isArray())
                .andExpect(jsonPath("$.result", hasSize(3)));
    }

    @Test
    void executeQuery_withWhereClause_shouldReturnFilteredResults() throws Exception {
        // Add a query with WHERE clause
        QueryRequest request = new QueryRequest("SELECT PassengerId, Name FROM passengers WHERE Survived = 1 LIMIT 2");

        MvcResult addResult = mockMvc.perform(post("/queries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        QueryResponse queryResponse = objectMapper.readValue(addResult.getResponse().getContentAsString(), QueryResponse.class);
        Long queryId = queryResponse.getId();

        // Execute and verify results
        mockMvc.perform(get("/queries/execute")
                        .param("query", queryId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").isArray())
                .andExpect(jsonPath("$.result", hasSize(2)));
    }

    @Test
    void executeQuery_withNonSelectQuery_shouldReturnBadRequest() throws Exception {
        // Add a DELETE query
        QueryRequest request = new QueryRequest("DELETE FROM passengers WHERE PassengerId = 1");

        MvcResult addResult = mockMvc.perform(post("/queries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        QueryResponse queryResponse = objectMapper.readValue(addResult.getResponse().getContentAsString(), QueryResponse.class);
        Long queryId = queryResponse.getId();

        // Try to execute - should fail
        mockMvc.perform(get("/queries/execute")
                        .param("query", queryId.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Only SELECT queries are allowed"));
    }

    @Test
    void executeQuery_withInvalidQueryId_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/queries/execute")
                        .param("query", "99999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Query with id 99999 not found"));
    }

    @Test
    void executeQuery_cachingBehavior_shouldCacheResults() throws Exception {
        // Add a query
        QueryRequest request = new QueryRequest("SELECT COUNT(*) FROM passengers");

        MvcResult addResult = mockMvc.perform(post("/queries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        QueryResponse queryResponse = objectMapper.readValue(addResult.getResponse().getContentAsString(), QueryResponse.class);
        Long queryId = queryResponse.getId();

        // Execute first time (Warm up cache)
        long startTime1 = System.currentTimeMillis();
        mockMvc.perform(get("/queries/execute")
                        .param("query", queryId.toString()))
                .andExpect(status().isOk());
        long duration1 = System.currentTimeMillis() - startTime1;

        // Execute second time (Should be cached)
        long startTime2 = System.currentTimeMillis();
        mockMvc.perform(get("/queries/execute")
                        .param("query", queryId.toString()))
                .andExpect(status().isOk());
        long duration2 = System.currentTimeMillis() - startTime2;

        // Verify speedup (Allowing small margin of error for test env jitter)
        assertTrue(duration2 <= duration1, "Cached query should be faster");
    }

    @Test
    void asyncExecution_fullFlow_shouldWork() throws Exception {
        // Add a query
        QueryRequest request = new QueryRequest("SELECT * FROM passengers LIMIT 5");

        MvcResult addResult = mockMvc.perform(post("/queries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        QueryResponse queryResponse = objectMapper.readValue(addResult.getResponse().getContentAsString(), QueryResponse.class);
        Long queryId = queryResponse.getId();

        // Start async execution
        MvcResult asyncResult = mockMvc.perform(post("/queries/execute/async")
                        .param("query", queryId.toString()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.executionId").exists())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        AsyncExecutionResponse asyncResponse = objectMapper.readValue(asyncResult.getResponse().getContentAsString(), AsyncExecutionResponse.class);
        String executionId = asyncResponse.getExecutionId();

        // Wait a bit for execution to complete (Polling logic in test)
        int attempts = 0;
        String status = "PENDING";
        while(attempts < 10 && !status.equals("COMPLETED") && !status.equals("FAILED")) {
            Thread.sleep(200); // Check every 200ms
            MvcResult statusResult = mockMvc.perform(get("/queries/execute/async/" + executionId))
                    .andReturn();
            status = com.jayway.jsonpath.JsonPath.read(statusResult.getResponse().getContentAsString(), "$.status");
            attempts++;
        }

        // Check final status
        mockMvc.perform(get("/queries/execute/async/" + executionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionId").value(executionId))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.result").isArray())
                .andExpect(jsonPath("$.result", hasSize(5)));
    }
}