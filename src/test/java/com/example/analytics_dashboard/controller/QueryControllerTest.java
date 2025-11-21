package com.example.analytics_dashboard.controller;

import com.example.analytics_dashboard.dto.QueryListResponse;
import com.example.analytics_dashboard.dto.QueryRequest;
import com.example.analytics_dashboard.dto.QueryResponse;
import com.example.analytics_dashboard.model.StoredQuery;
import com.example.analytics_dashboard.service.QueryExecutionService;
import com.example.analytics_dashboard.service.QueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = QueryController.class)
public class QueryControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private QueryService queryService;

    @MockitoBean
    private QueryExecutionService executionService;

    @Test
    void addQuery_withValidQuery_shouldReturnCreatedWithId() throws Exception {
        // Arrange
        QueryRequest request = new QueryRequest("SELECT * FROM passengers");
        QueryResponse expectedResponse = new QueryResponse(1L);

        when(queryService.addQuery(any(String.class))).thenReturn(expectedResponse);

        // Act & Assert
        mockMvc.perform(post("/queries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.id").value(1));

        verify(queryService, times(1)).addQuery("SELECT * FROM passengers");
    }

    @Test
    void addQuery_withEmptyQuery_shouldReturnBadRequest() throws Exception {
        // Arrange
        QueryRequest request = new QueryRequest("");

        when(queryService.addQuery(any(String.class)))
                .thenThrow(new IllegalArgumentException("Query text cannot be empty"));

        // Act & Assert
        mockMvc.perform(post("/queries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Query text cannot be empty"));
    }

    @Test
    void getAllQueries_shouldReturnListOfQueries() throws Exception {
        // Arrange
        List<QueryListResponse> queries = Arrays.asList(
                new QueryListResponse(1L, "SELECT * FROM passengers"),
                new QueryListResponse(2L, "SELECT name FROM passengers")
        );

        when(queryService.getAllQueries()).thenReturn(queries);

        // Act & Assert
        mockMvc.perform(get("/queries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].query").value("SELECT * FROM passengers"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].query").value("SELECT name FROM passengers"));

        verify(queryService, times(1)).getAllQueries();
    }

    @Test
    void getAllQueries_whenEmpty_shouldReturnEmptyArray() throws Exception {
        // Arrange
        when(queryService.getAllQueries()).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/queries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void executeQuery_withValidQuery_shouldReturnResults() throws Exception {
        // Arrange
        Long queryId = 1L;
        StoredQuery storedQuery = new StoredQuery("SELECT * FROM passengers LIMIT 2");
        storedQuery.setId(queryId);

        List<List<Object>> expectedResult = Arrays.asList(
                Arrays.asList(1, "John", 25),
                Arrays.asList(2, "Jane", 30)
        );

        when(queryService.getQueryById(queryId)).thenReturn(storedQuery);
        when(executionService.isReadOnlyQuery(storedQuery.getQueryText())).thenReturn(true);
        when(executionService.executeQuery(storedQuery.getQueryText())).thenReturn(expectedResult);

        // Act & Assert
        mockMvc.perform(get("/queries/execute")
                        .param("query", "1"))
                .andExpect(status().isOk())
                // ZMIANA: Sprawdzamy, czy istnieje pole "result"
                .andExpect(jsonPath("$.result").isArray())
                // ZMIANA: Ścieżki teraz muszą zawierać ".result"
                .andExpect(jsonPath("$.result[0][0]").value(1))
                .andExpect(jsonPath("$.result[0][1]").value("John"))
                .andExpect(jsonPath("$.result[0][2]").value(25))
                .andExpect(jsonPath("$.result[1][0]").value(2));

        verify(queryService, times(1)).getQueryById(queryId);
        verify(executionService, times(1)).isReadOnlyQuery(storedQuery.getQueryText());
        verify(executionService, times(1)).executeQuery(storedQuery.getQueryText());
    }

    @Test
    void executeQuery_withNonSelectQuery_shouldReturnBadRequest() throws Exception {
        // Arrange
        Long queryId = 1L;
        StoredQuery storedQuery = new StoredQuery("DELETE FROM passengers");
        storedQuery.setId(queryId);

        when(queryService.getQueryById(queryId)).thenReturn(storedQuery);
        when(executionService.isReadOnlyQuery(storedQuery.getQueryText())).thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/queries/execute")
                        .param("query", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Only SELECT queries are allowed"));

        verify(queryService, times(1)).getQueryById(queryId);
        verify(executionService, times(1)).isReadOnlyQuery(storedQuery.getQueryText());
        verify(executionService, never()).executeQuery(any());
    }

    @Test
    void executeQuery_withNonExistentId_shouldReturnBadRequest() throws Exception {
        // Arrange
        Long queryId = 999L;

        when(queryService.getQueryById(queryId))
                .thenThrow(new IllegalArgumentException("Query with id 999 not found"));

        // Act & Assert
        mockMvc.perform(get("/queries/execute")
                        .param("query", "999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Query with id 999 not found"));

        verify(queryService, times(1)).getQueryById(queryId);
        verify(executionService, never()).executeQuery(any());
    }

    @Test
    void executeQuery_withEmptyResult_shouldReturnEmptyArray() throws Exception {
        // Arrange
        Long queryId = 1L;
        StoredQuery storedQuery = new StoredQuery("SELECT * FROM passengers WHERE age > 1000");
        storedQuery.setId(queryId);

        List<List<Object>> emptyResult = Collections.emptyList();

        when(queryService.getQueryById(queryId)).thenReturn(storedQuery);
        when(executionService.isReadOnlyQuery(storedQuery.getQueryText())).thenReturn(true);
        when(executionService.executeQuery(storedQuery.getQueryText())).thenReturn(emptyResult);

        // Act & Assert
        mockMvc.perform(get("/queries/execute")
                        .param("query", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").isArray())
                .andExpect(jsonPath("$.result.length()").value(0));
    }

}
