package com.example.analytics_dashboard.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Service
public class QueryExecutionService {
    private final JdbcTemplate jdbcTemplate;

    public QueryExecutionService(@Qualifier("readOnlyJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Executes a SQL query and returns the result as a 2D array
     * @param sql The SQL query to execute
     * @return 2D array where each inner array represents a row
     * @throws SQLException if query execution fails
     **/
    public List<List<Object>> executeQuery(String sql){
        return jdbcTemplate.query(sql, this::mapResultSetTo2DArray);
    }

    private List<List<Object>> mapResultSetTo2DArray(ResultSet rs) throws SQLException {
        List<List<Object>> result = new ArrayList<>();
        ResultSetMetaData rsmd = rs.getMetaData();
        int columnCount = rsmd.getColumnCount();

        while (rs.next()) {
            List<Object> row = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                row.add(rs.getObject(i));
            }
            result.add(row);
        }

        return result;
    }

    /**
     * Validates if a query is a SELECT statement (read-only)
     * This is a basic implementation - can be improved
     */
    public boolean isReadOnlyQuery(String sql){
        if(sql == null || sql.trim().isEmpty()){
            return false;
        }

        String nSql = sql.trim().toUpperCase();
        if(!nSql.startsWith("SELECT")){
            return false;
        }

        String[] dangerousKeywords = {
                "INSERT", "UPDATE", "DELETE", "DROP", "CREATE",
                "ALTER", "TRUNCATE", "REPLACE", "MERGE"
        };

        for (String keyword : dangerousKeywords) {
            if(nSql.contains(keyword)){
                return false;
            }
        }

        return true;
    }


}
