package com.example.analytics_dashboard.service;

import com.example.analytics_dashboard.model.QueryExecution;
import com.example.analytics_dashboard.model.StoredQuery;
import com.example.analytics_dashboard.repository.QueryExecutionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsyncQueryExecutionServiceTest {

    @Mock
    private QueryExecutionRepository executionRepository;

    @Mock
    private QueryExecutionService queryExecutionService;

    @Mock // Self-injection
    private AsyncQueryExecutionService self;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AsyncQueryExecutionService asyncService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(asyncService, "self", self);
    }

    @Test
    void startExecution_shouldCreateExecutionAndReturnId() {
        // Arrange
        StoredQuery query = new StoredQuery("SELECT * FROM passengers");
        query.setId(1L);

        // Simulate saving in db (setting UUID)
        UUID generatedId = UUID.randomUUID();
        when(executionRepository.save(any(QueryExecution.class))).thenAnswer(invocation -> {
            QueryExecution ex = invocation.getArgument(0);
            ex.setId(generatedId);
            return ex;
        });

        // Act
        String executionId = asyncService.startExecution(query);

        // Assert
        assertNotNull(executionId);
        assertEquals(generatedId.toString(), executionId);
        verify(self, times(1)).executeAsync(eq(generatedId.toString()), eq(query.getQueryText()));
    }

    @Test
    void getExecution_withValidId_shouldReturnExecution() {
        // Arrange
        UUID uuid = UUID.randomUUID();
        String executionIdStr = uuid.toString();
        QueryExecution execution = new QueryExecution(1L);
        execution.setId(uuid);

        when(executionRepository.findById(uuid)).thenReturn(Optional.of(execution));

        // Act
        QueryExecution result = asyncService.getExecution(executionIdStr);

        // Assert
        assertNotNull(result);
        assertEquals(uuid, result.getId());
    }

    @Test
    void parseResult_withValidJson_shouldReturnList() throws Exception {
        // Arrange
        // We only use the real mapper in a test of the method that actually uses it
        // (In this AsyncService unit test, we mock the mapper within the class,
        // so we must configure the mock)
        List<List<Object>> expectedResult = Arrays.asList(
                Arrays.asList(1, "John", 25),
                Arrays.asList(2, "Jane", 30)
        );
        String jsonInput = "dummy-json";

        when(objectMapper.readValue(eq(jsonInput), eq(List.class))).thenReturn(expectedResult);

        // Act
        List<List<Object>> result = asyncService.parseResult(jsonInput);

        // Assert
        assertEquals(2, result.size());
    }
}