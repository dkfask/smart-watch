package com.smartwatch.monitor.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.smartwatch.monitor.model.Patient;
import java.util.List;

/**
 * 病人数据访问对象，提供病人表的CRUD操作
 */
@Dao
public interface PatientDao {

    /**
     * 插入病人数据，冲突时替换
     * @param patients 病人列表
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Patient> patients);

    /**
     * 插入单条病人数据，冲突时替换
     * @param patient 病人对象
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Patient patient);

    /**
     * 获取所有病人列表
     * @return 病人列表LiveData
     */
    @Query("SELECT * FROM patients")
    LiveData<List<Patient>> getAllPatients();

    /**
     * 根据ID获取病人
     * @param id 病人ID
     * @return 病人对象LiveData
     */
    @Query("SELECT * FROM patients WHERE id = :id")
    LiveData<Patient> getPatientById(long id);

    /**
     * 删除所有病人数据
     */
    @Query("DELETE FROM patients")
    void deleteAll();

    /**
     * 删除指定病人
     * @param patient 病人对象
     */
    @Delete
    void delete(Patient patient);
}
