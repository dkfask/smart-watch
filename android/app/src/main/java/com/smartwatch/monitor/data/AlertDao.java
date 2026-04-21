package com.smartwatch.monitor.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.smartwatch.monitor.model.Alert;
import java.util.List;

/**
 * 报警数据访问对象，提供报警表的CRUD操作
 */
@Dao
public interface AlertDao {

    /**
     * 插入报警数据，冲突时替换
     * @param alerts 报警列表
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Alert> alerts);

    /**
     * 插入单条报警数据，冲突时替换
     * @param alert 报警对象
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Alert alert);

    /**
     * 获取所有报警列表
     * @return 报警列表LiveData
     */
    @Query("SELECT * FROM alerts ORDER BY alert_time DESC")
    LiveData<List<Alert>> getAllAlerts();

    /**
     * 根据ID获取报警
     * @param id 报警ID
     * @return 报警对象LiveData
     */
    @Query("SELECT * FROM alerts WHERE id = :id")
    LiveData<Alert> getAlertById(long id);

    /**
     * 获取未读报警列表
     * @return 未读报警列表LiveData
     */
    @Query("SELECT * FROM alerts WHERE is_read = 0 ORDER BY alert_time DESC")
    LiveData<List<Alert>> getUnreadAlerts();

    /**
     * 删除所有报警数据
     */
    @Query("DELETE FROM alerts")
    void deleteAll();

    /**
     * 删除指定报警
     * @param alert 报警对象
     */
    @Delete
    void delete(Alert alert);
}
