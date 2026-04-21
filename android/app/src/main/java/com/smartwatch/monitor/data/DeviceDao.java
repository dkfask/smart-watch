package com.smartwatch.monitor.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.smartwatch.monitor.model.Device;
import java.util.List;

/**
 * 设备数据访问对象，提供设备表的CRUD操作
 */
@Dao
public interface DeviceDao {

    /**
     * 插入设备数据，冲突时替换
     * @param devices 设备列表
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Device> devices);

    /**
     * 插入单条设备数据，冲突时替换
     * @param device 设备对象
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Device device);

    /**
     * 获取所有设备列表
     * @return 设备列表LiveData
     */
    @Query("SELECT * FROM devices")
    LiveData<List<Device>> getAllDevices();

    /**
     * 根据ID获取设备
     * @param id 设备ID
     * @return 设备对象LiveData
     */
    @Query("SELECT * FROM devices WHERE id = :id")
    LiveData<Device> getDeviceById(long id);

    /**
     * 删除所有设备数据
     */
    @Query("DELETE FROM devices")
    void deleteAll();

    /**
     * 删除指定设备
     * @param device 设备对象
     */
    @Delete
    void delete(Device device);
}
