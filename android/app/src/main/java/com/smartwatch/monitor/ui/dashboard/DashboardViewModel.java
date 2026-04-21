package com.smartwatch.monitor.ui.dashboard;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.smartwatch.monitor.model.AlarmStatsDto;
import com.smartwatch.monitor.model.PageResponse;
import com.smartwatch.monitor.model.Patient;
import com.smartwatch.monitor.model.DeviceInfoDto;
import com.smartwatch.monitor.repository.AlarmRepository;
import com.smartwatch.monitor.repository.DeviceRepository;
import com.smartwatch.monitor.repository.FenceRepository;
import com.smartwatch.monitor.repository.PatientRepository;
import java.util.List;
import java.util.Map;

/**
 * 仪表盘ViewModel，提供概览统计数据和最近报警信息
 */
public class DashboardViewModel extends AndroidViewModel {

    private final AlarmRepository alarmRepository;
    private final PatientRepository patientRepository;
    private final DeviceRepository deviceRepository;
    private final FenceRepository fenceRepository;

    private final MutableLiveData<AlarmStatsDto> alarmStats = new MutableLiveData<>();
    private final MutableLiveData<Integer> patientCount = new MutableLiveData<>();
    private final MutableLiveData<Integer> deviceCount = new MutableLiveData<>();
    private final MutableLiveData<Integer> fenceCount = new MutableLiveData<>();
    private final MutableLiveData<List<Map<String, Object>>> recentAlarms = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    /**
     * 构造函数，初始化各仓库
     * @param application 应用实例
     */
    public DashboardViewModel(@NonNull Application application) {
        super(application);
        alarmRepository = new AlarmRepository();
        patientRepository = new PatientRepository();
        deviceRepository = new DeviceRepository();
        fenceRepository = new FenceRepository();
    }

    /**
     * 加载仪表盘全部数据，包括病人数、设备数、围栏数、报警统计和最近报警
     */
    public void loadDashboardData() {
        isLoading.setValue(true);
        errorMessage.setValue(null);

        alarmRepository.getAlarmStats().observeForever(stats -> {
            alarmStats.setValue(stats);
            checkLoadingComplete();
        });

        patientRepository.getPatients("", 0).observeForever(result -> {
            if (result != null) {
                patientCount.setValue((int) result.getTotal());
            } else {
                patientCount.setValue(0);
            }
            checkLoadingComplete();
        });

        deviceRepository.getDevices("", 0).observeForever(result -> {
            if (result != null) {
                deviceCount.setValue((int) result.getTotal());
            } else {
                deviceCount.setValue(0);
            }
            checkLoadingComplete();
        });

        fenceRepository.getActiveFenceCount().observeForever(count -> {
            fenceCount.setValue(count != null ? count : 0);
            checkLoadingComplete();
        });

        loadRecentAlarms();
    }

    /**
     * 加载最近报警记录列表
     */
    public void loadRecentAlarms() {
        alarmRepository.getRecentAlarms(10).observeForever(alarms -> {
            recentAlarms.setValue(alarms);
            checkLoadingComplete();
        });
    }

    /**
     * 检查所有数据是否加载完成，更新加载状态
     */
    private void checkLoadingComplete() {
        if (alarmStats.getValue() != null || patientCount.getValue() != null
                || deviceCount.getValue() != null || fenceCount.getValue() != null
                || recentAlarms.getValue() != null) {
            isLoading.setValue(false);
        }
    }

    /**
     * 获取报警统计数据LiveData
     * @return 报警统计数据
     */
    public LiveData<AlarmStatsDto> getAlarmStats() { return alarmStats; }

    /**
     * 获取病人总数LiveData
     * @return 病人总数
     */
    public LiveData<Integer> getPatientCount() { return patientCount; }

    /**
     * 获取设备总数LiveData
     * @return 设备总数
     */
    public LiveData<Integer> getDeviceCount() { return deviceCount; }

    /**
     * 获取活跃围栏数LiveData
     * @return 活跃围栏数
     */
    public LiveData<Integer> getFenceCount() { return fenceCount; }

    /**
     * 获取最近报警列表LiveData
     * @return 最近报警列表
     */
    public LiveData<List<Map<String, Object>>> getRecentAlarms() { return recentAlarms; }

    /**
     * 获取加载状态LiveData
     * @return 是否正在加载
     */
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    /**
     * 获取错误信息LiveData
     * @return 错误信息字符串
     */
    public LiveData<String> getErrorMessage() { return errorMessage; }
}
