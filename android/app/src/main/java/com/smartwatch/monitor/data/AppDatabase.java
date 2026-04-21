package com.smartwatch.monitor.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.smartwatch.monitor.model.Alert;
import com.smartwatch.monitor.model.Device;
import com.smartwatch.monitor.model.Patient;

/**
 * Room数据库抽象类，定义数据库实例和DAO访问入口
 */
@Database(entities = {Patient.class, Device.class, Alert.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    /**
     * 获取病人DAO
     * @return PatientDao实例
     */
    public abstract PatientDao patientDao();

    /**
     * 获取设备DAO
     * @return DeviceDao实例
     */
    public abstract DeviceDao deviceDao();

    /**
     * 获取报警DAO
     * @return AlertDao实例
     */
    public abstract AlertDao alertDao();

    /**
     * 获取AppDatabase单例，使用双重检查锁定保证线程安全
     * @param context 应用上下文
     * @return AppDatabase实例
     */
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "smartwatch_db"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}
