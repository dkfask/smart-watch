package com.example.demo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * MySqlService单元测试
 */
@ExtendWith(MockitoExtension.class)
class MySqlServiceTest {

    @Mock private DataSource dataSource;
    @Mock private Connection connection;
    @Mock private Statement statement;
    @Mock private PreparedStatement preparedStatement;
    @Mock private ResultSet resultSet;
    @Mock private ResultSetMetaData metaData;

    private MySqlService mySqlService;

    @BeforeEach
    void setUp() throws Exception {
        mySqlService = new MySqlService(dataSource);
    }

    /**
     * testConnection连接成功返回true
     */
    @Test
    void testConnection_success_returnsTrue() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery("SELECT 1")).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(1);

        assertTrue(mySqlService.testConnection());
    }

    /**
     * testConnection查询结果非1返回false
     */
    @Test
    void testConnection_nonOneResult_returnsFalse() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery("SELECT 1")).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(0);

        assertFalse(mySqlService.testConnection());
    }

    /**
     * testConnection无结果返回false
     */
    @Test
    void testConnection_noResult_returnsFalse() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery("SELECT 1")).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        assertFalse(mySqlService.testConnection());
    }

    /**
     * testConnection异常返回false
     */
    @Test
    void testConnection_exception_returnsFalse() throws Exception {
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection refused"));

        assertFalse(mySqlService.testConnection());
    }

    /**
     * query执行查询返回结果列表
     */
    @Test
    void query_returnsResultList() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("SELECT * FROM devices WHERE id = ?")).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnCount()).thenReturn(2);
        when(metaData.getColumnLabel(1)).thenReturn("id");
        when(metaData.getColumnLabel(2)).thenReturn("imei");
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getObject(1)).thenReturn(1L);
        when(resultSet.getObject(2)).thenReturn("IMEI001");

        var result = mySqlService.query("SELECT * FROM devices WHERE id = ?", 1L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).get("id"));
        assertEquals("IMEI001", result.get(0).get("imei"));
        verify(preparedStatement).setObject(1, 1L);
    }

    /**
     * query无参数查询返回空列表
     */
    @Test
    void query_noArgs_returnsEmptyList() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("SELECT * FROM devices")).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnCount()).thenReturn(1);
        when(resultSet.next()).thenReturn(false);

        var result = mySqlService.query("SELECT * FROM devices");

        assertTrue(result.isEmpty());
    }

    /**
     * query异常时抛出RuntimeException
     */
    @Test
    void query_exception_throwsRuntimeException() throws Exception {
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection refused"));

        assertThrows(RuntimeException.class, () ->
                mySqlService.query("SELECT * FROM devices"));
    }
}
