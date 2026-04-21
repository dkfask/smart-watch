package com.smartwatch.monitor.ui.patient;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.smartwatch.monitor.repository.HealthRepository;
import java.util.Map;

/**
 * 健康数据ViewModel，处理健康数据的加载、统计、历史记录等数据逻辑
 */
public class HealthViewModel extends AndroidViewModel {

    private final HealthRepository healthRepository;
    private final MutableLiveData<Map<String, Object>> latestHealthData = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Object>> healthStats = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Object>> healthHistory = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    /**
     * 构造函数，初始化健康数据仓库
     * @param application 应用实例
     */
    public HealthViewModel(@NonNull Application application) {
        super(application);
        healthRepository = new HealthRepository();
    }

    /**
     * 加载病人最新的各类健康数据
     * @param patientId 病人ID
     */
    public void loadLatestHealthData(long patientId) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        healthRepository.getLatestHealthRecords(patientId).observeForever(result -> {
            isLoading.setValue(false);
            if (result != null) {
                latestHealthData.setValue(result);
            } else {
                errorMessage.setValue("加载最新健康数据失败");
            }
        });
    }

    /**
     * 加载病人健康数据统计
     * @param patientId 病人ID
     * @param dataType 数据类型
     * @param start 开始时间戳
     * @param end 结束时间戳
     */
    public void loadHealthStats(long patientId, String dataType, Long start, Long end) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        healthRepository.getHealthStats(patientId, dataType, start, end).observeForever(result -> {
            isLoading.setValue(false);
            if (result != null) {
                healthStats.setValue(result);
            } else {
                errorMessage.setValue("加载健康统计数据失败");
            }
        });
    }

    /**
     * 加载病人健康数据历史记录
     * @param patientId 病人ID
     * @param dataType 数据类型
     * @param limit 获取数量限制
     */
    public void loadHealthHistory(long patientId, String dataType, int limit) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        healthRepository.getHealthHistory(patientId, dataType, limit).observeForever(result -> {
            isLoading.setValue(false);
            if (result != null) {
                healthHistory.setValue(result);
            } else {
                errorMessage.setValue("加载健康历史数据失败");
            }
        });
    }

    /**
     * 获取最新健康数据LiveData
     * @return 最新健康数据Map，包含temperature、heart_rate、blood_pressure、spo2
     */
    public LiveData<Map<String, Object>> getLatestHealthData() { return latestHealthData; }

    /**
     * 获取健康统计数据LiveData
     * @return 健康统计数据Map
     */
    public LiveData<Map<String, Object>> getHealthStats() { return healthStats; }

    /**
     * 获取健康历史数据LiveData
     * @return 健康历史数据Map
     */
    public LiveData<Map<String, Object>> getHealthHistory() { return healthHistory; }

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
