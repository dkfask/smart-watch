package com.smartwatch.monitor.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.smartwatch.monitor.api.ApiClient;
import com.smartwatch.monitor.api.LocationService;
import com.smartwatch.monitor.model.DeviceLocation;
import com.smartwatch.monitor.model.PageResponse;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 位置数据仓库，负责处理设备位置相关数据操作
 */
public class LocationRepository {

    private final LocationService locationService;

    /**
     * 构造函数，初始化位置服务
     */
    public LocationRepository() {
        locationService = ApiClient.getInstance().createService(LocationService.class);
    }

    /**
     * 获取设备最新位置
     * @param deviceId 设备ID
     * @return 最新位置LiveData
     */
    public LiveData<DeviceLocation> getLatestLocation(long deviceId) {
        MutableLiveData<DeviceLocation> result = new MutableLiveData<>();
        locationService.getLatestLocation(deviceId).enqueue(new Callback<DeviceLocation>() {
            @Override
            public void onResponse(Call<DeviceLocation> call, Response<DeviceLocation> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.setValue(response.body());
                }
            }

            @Override
            public void onFailure(Call<DeviceLocation> call, Throwable t) {
                result.setValue(null);
            }
        });
        return result;
    }

    /**
     * 获取设备最新位置（含地址）
     * @param deviceId 设备ID
     * @return 含地址的最新位置LiveData
     */
    public LiveData<Object> getLatestLocationWithAddress(long deviceId) {
        MutableLiveData<Object> result = new MutableLiveData<>();
        locationService.getLatestLocationWithAddress(deviceId).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.setValue(response.body());
                }
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                result.setValue(null);
            }
        });
        return result;
    }

    /**
     * 获取设备历史位置记录
     * @param deviceId 设备ID
     * @param limit 每页数量
     * @param offset 偏移量
     * @return 分页位置记录LiveData
     */
    public LiveData<PageResponse<DeviceLocation>> getDeviceLocations(long deviceId, int limit, int offset) {
        MutableLiveData<PageResponse<DeviceLocation>> result = new MutableLiveData<>();
        locationService.recent(deviceId, limit, offset).enqueue(new Callback<PageResponse<DeviceLocation>>() {
            @Override
            public void onResponse(Call<PageResponse<DeviceLocation>> call,
                                   Response<PageResponse<DeviceLocation>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.setValue(response.body());
                }
            }

            @Override
            public void onFailure(Call<PageResponse<DeviceLocation>> call, Throwable t) {
                result.setValue(null);
            }
        });
        return result;
    }

    /**
     * 根据地址搜索位置
     * @param address 地址字符串
     * @return 位置信息LiveData
     */
    public LiveData<Map<String, Object>> searchLocation(String address) {
        MutableLiveData<Map<String, Object>> result = new MutableLiveData<>();
        locationService.searchLocation(address).enqueue(new Callback<Map<String, Object>>() {
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
