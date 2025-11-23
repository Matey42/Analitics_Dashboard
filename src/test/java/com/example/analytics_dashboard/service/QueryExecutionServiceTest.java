package com.example.analytics_dashboard.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class QueryExecutionServiceTest {
    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private QueryExecutionService executionService;

    @Test
    void executeQuery_withValidQuery_shouldReturnResults() {
        // Arrange
        String sql = "SELECT * FROM passengers LIMIT 2";
        List<List<Object>> expectedResult = Arrays.asList(
                Arrays.asList(1, "John", 25),
                Arrays.asList(2, "Jane", 30)
        );

        when(jdbcTemplate.query(eq(sql), any(ResultSetExtractor.class)))
                .thenReturn(expectedResult);

        // Act
        List<List<Object>> result = executionService.executeQuery(sql);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(jdbcTemplate, times(1)).query(eq(sql), any(ResultSetExtractor.class));
    }

    @Test
    void executeQuery_withInvalidQuery_shouldThrowException() {
        // Arrange
        String sql = "SELECT * FROM nonexistent_table";

        when(jdbcTemplate.query(eq(sql), any(ResultSetExtractor.class)))
                .thenThrow(new DataAccessException("Table not found") {});

        // Act & Assert
        assertThrows(DataAccessException.class, () -> {
            executionService.executeQuery(sql);
        });
    }

    @Test
    void isReadOnlyQuery_withSelectQuery_shouldReturnTrue() {
        // Arrange
        String sql = "SELECT * FROM passengers";

        // Act
        boolean result = executionService.isReadOnlyQuery(sql);

        // Assert
        assertTrue(result);
    }

    @Test
    void isReadOnlyQuery_withSelectQueryLowerCase_shouldReturnTrue() {
        // Arrange
        String sql = "select name, age from passengers where age > 30";

        // Act
        boolean result = executionService.isReadOnlyQuery(sql);

        // Assert
        assertTrue(result);
    }

    @Test
    void isReadOnlyQuery_withInsertQuery_shouldReturnFalse() {
        // Arrange
        String sql = "INSERT INTO passengers VALUES (1, 'John')";

        // Act
        boolean result = executionService.isReadOnlyQuery(sql);

        // Assert
        assertFalse(result);
    }

    @Test
    void isReadOnlyQuery_withUpdateQuery_shouldReturnFalse() {
        // Arrange
        String sql = "UPDATE passengers SET age = 30 WHERE id = 1";

        // Act
        boolean result = executionService.isReadOnlyQuery(sql);

        // Assert
        assertFalse(result);
    }

    @Test
    void isReadOnlyQuery_withDeleteQuery_shouldReturnFalse() {
        // Arrange
        String sql = "DELETE FROM passengers WHERE id = 1";

        // Act
        boolean result = executionService.isReadOnlyQuery(sql);

        // Assert
        assertFalse(result);
    }

    @Test
    void isReadOnlyQuery_withDropQuery_shouldReturnFalse() {
        // Arrange
        String sql = "DROP TABLE passengers";

        // Act
        boolean result = executionService.isReadOnlyQuery(sql);

        // Assert
        assertFalse(result);
    }

    @Test
    void isReadOnlyQuery_withCreateQuery_shouldReturnFalse() {
        // Arrange
        String sql = "CREATE TABLE test (id INT)";

        // Act
        boolean result = executionService.isReadOnlyQuery(sql);

        // Assert
        assertFalse(result);
    }

    @Test
    void isReadOnlyQuery_withSelectAndInsertInComment_shouldDetectInsert() {
        // Arrange - tricky case where INSERT is in the query
        String sql = "SELECT * FROM passengers; INSERT INTO passengers VALUES (1, 'John')";

        // Act
        boolean result = executionService.isReadOnlyQuery(sql);

        // Assert
        assertFalse(result, "Should detect INSERT even if SELECT comes first");
    }

    @Test
    void isReadOnlyQuery_withNullQuery_shouldReturnFalse() {
        // Act
        boolean result = executionService.isReadOnlyQuery(null);

        // Assert
        assertFalse(result);
    }

    @Test
    void isReadOnlyQuery_withEmptyQuery_shouldReturnFalse() {
        // Act
        boolean result = executionService.isReadOnlyQuery("");

        // Assert
        assertFalse(result);
    }
}
