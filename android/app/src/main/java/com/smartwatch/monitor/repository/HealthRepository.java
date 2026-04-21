package com.smartwatch.monitor.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.smartwatch.monitor.api.ApiClient;
import com.smartwatch.monitor.api.HealthService;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 健康数据仓库，负责处理健康记录相关数据操作
 */
public class HealthRepository {

    private final HealthService healthService;

    /**
     * 构造函数，初始化健康数据服务
     */
    public HealthRepository() {
        healthService = ApiClient.getInstance().createService(HealthService.class);
    }

    /**
     * 获取健康记录列表
     * @param patientId 病人ID
     * @param dataType 数据类型
     * @return 健康记录列表LiveData
     */
    public LiveData<Map<String, Object>> getHealthRecords(Long patientId, String dataType) {
        MutableLiveData<Map<String, Object>> result = new MutableLiveData<>();
        healthService.getHealthRecords(patientId, null, dataType, 50, 0)
                .enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            result.setValue(response.body());
                        }
                    }

                    @Override
                    public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                        result.setValue(null);
                    }
                });
        return result;
    }

    /**
     * 获取病人最新的各类健康数据
     * @param patientId 病人ID
     * @return 最新健康数据LiveData
     */
    public LiveData<Map<String, Object>> getLatestHealthRecords(Long patientId) {
        MutableLiveData<Map<String, Object>> result = new MutableLiveData<>();
        healthService.getLatestHealthRecords(patientId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.setValue(response.body());
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                result.setValue(null);
            }
        });
        return result;
    }

    /**
     * 获取病人健康数据统计
     * @param patientId 病人ID
     * @param dataType 数据类型
     * @param start 开始时间戳
     * @param end 结束时间戳
     * @return 健康统计数据LiveData
     */
    public LiveData<Map<String, Object>> getHealthStats(Long patientId, String dataType, Long start, Long end) {
        MutableLiveData<Map<String, Object>> result = new MutableLiveData<>();
        healthService.getHealthStats(patientId, dataType, start, end)
                .enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            result.setValue(response.body());
                        }
                    }

                    @Override
                    public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                        result.setValue(null);
                    }
                });
        return result;
    }

    /**
     * 获取病人健康数据历史记录
     * @param patientId 病人ID
     * @param dataType 数据类型
     * @param limit 获取数量限制
     * @return 健康数据历史列表LiveData
     */
    public LiveData<Map<String, Object>> getHealthHistory(Long patientId, String dataType, int limit) {
        MutableLiveData<Map<String, Object>> result = new MutableLiveData<>();
        healthService.getHealthRecords(patientId, null, dataType, limit, 0)
                .enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            result.setValue(response.body());
                        }
                    }

                    @Override
                    public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                        result.setValue(null);
                    }
                });
        return result;
    }
}
