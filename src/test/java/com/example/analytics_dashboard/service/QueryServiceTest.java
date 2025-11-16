package com.example.analytics_dashboard.service;

import com.example.analytics_dashboard.dto.QueryListResponse;
import com.example.analytics_dashboard.dto.QueryResponse;
import com.example.analytics_dashboard.model.StoredQuery;
import com.example.analytics_dashboard.repository.QueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class QueryServiceTest {
    @Mock
    private QueryRepository queryRepository;

    @InjectMocks
    private QueryService queryService;

    @Test
    void addQuery_withValidQuery_shouldReturnQueryResponse() {
        // Arrange
        String queryText = "SELECT * FROM passengers";
        StoredQuery savedQuery = new StoredQuery(queryText);
        savedQuery.setId(1L);

        when(queryRepository.save(any(StoredQuery.class))).thenReturn(savedQuery);

        // Act
        QueryResponse response = queryService.addQuery(queryText);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        verify(queryRepository, times(1)).save(any(StoredQuery.class));
    }

    @Test
    void addQuery_withEmptyQuery_shouldThrowException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            queryService.addQuery("");
        });

        verify(queryRepository, never()).save(any(StoredQuery.class));
    }

    @Test
    void addQuery_withNullQuery_shouldThrowException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            queryService.addQuery(null);
        });

        verify(queryRepository, never()).save(any(StoredQuery.class));
    }

    @Test
    void getAllQueries_shouldReturnListOfQueries() {
        // Arrange
        StoredQuery query1 = new StoredQuery("SELECT * FROM passengers");
        query1.setId(1L);

        StoredQuery query2 = new StoredQuery("SELECT name FROM passengers");
        query2.setId(2L);

        when(queryRepository.findAll()).thenReturn(Arrays.asList(query1, query2));

        // Act
        List<QueryListResponse> queries = queryService.getAllQueries();

        // Assert
        assertEquals(2, queries.size());
        assertEquals(1L, queries.get(0).getId());
        assertEquals("SELECT * FROM passengers", queries.get(0).getQuery());
        assertEquals(2L, queries.get(1).getId());
        verify(queryRepository, times(1)).findAll();
    }

    @Test
    void getQueryById_withExistingId_shouldReturnQuery() {
        // Arrange
        Long queryId = 1L;
        StoredQuery query = new StoredQuery("SELECT * FROM passengers");
        query.setId(queryId);

        when(queryRepository.findById(queryId)).thenReturn(Optional.of(query));

        // Act
        StoredQuery result = queryService.getQueryById(queryId);

        // Assert
        assertNotNull(result);
        assertEquals(queryId, result.getId());
        assertEquals("SELECT * FROM passengers", result.getQueryText());
    }

    @Test
    void getQueryById_withNonExistingId_shouldThrowException() {
        // Arrange
        Long queryId = 999L;
        when(queryRepository.findById(queryId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            queryService.getQueryById(queryId);
        });
    }

}
