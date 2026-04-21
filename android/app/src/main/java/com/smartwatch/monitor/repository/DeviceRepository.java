package com.smartwatch.monitor.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.smartwatch.monitor.api.ApiClient;
import com.smartwatch.monitor.api.DeviceService;
import com.smartwatch.monitor.data.AppDatabase;
import com.smartwatch.monitor.data.DeviceDao;
import com.smartwatch.monitor.model.Device;
import com.smartwatch.monitor.model.DeviceInfoDto;
import com.smartwatch.monitor.model.PageResponse;
import com.smartwatch.monitor.utils.Constants;
import java.util.List;
import java.util.concurrent.Executors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 设备数据仓库，负责处理设备相关数据操作，支持Room离线缓存
 */
public class DeviceRepository {

    private final DeviceService deviceService;
    private final DeviceDao deviceDao;

    /**
     * 构造函数，初始化设备服务
     */
    public DeviceRepository() {
        deviceService = ApiClient.getInstance().createService(DeviceService.class);
        deviceDao = null;
    }

    /**
     * 构造函数，初始化设备服务和Room DAO（带数据库支持）
     * @param application 应用实例
     */
    public DeviceRepository(Application application) {
        deviceService = ApiClient.getInstance().createService(DeviceService.class);
        deviceDao = AppDatabase.getInstance(application).deviceDao();
    }

    /**
     * 获取设备列表，优先从网络获取，失败时返回缓存数据
     * @param search 搜索关键词
     * @param page 页码
     * @return 设备分页列表LiveData
     */
    public LiveData<PageResponse<DeviceInfoDto>> getDevices(String search, int page) {
        MutableLiveData<PageResponse<DeviceInfoDto>> result = new MutableLiveData<>();
        deviceService.list(page, Constants.DEFAULT_PAGE_SIZE, search).enqueue(new Callback<PageResponse<DeviceInfoDto>>() {
            @Override
            public void onResponse(Call<PageResponse<DeviceInfoDto>> call, Response<PageResponse<DeviceInfoDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.setValue(response.body());
                }
            }

            @Override
            public void onFailure(Call<PageResponse<DeviceInfoDto>> call, Throwable t) {
                result.setValue(null);
            }
        });
        return result;
    }

    /**
     * 获取设备详情，优先从网络获取，失败时返回缓存数据
     * @param id 设备ID
     * @return 设备详情LiveData
     */
    public LiveData<DeviceInfoDto> getDevice(long id) {
        MutableLiveData<DeviceInfoDto> result = new MutableLiveData<>();
        deviceService.get(id).enqueue(new Callback<DeviceInfoDto>() {
            @Override
            public void onResponse(Call<DeviceInfoDto> call, Response<DeviceInfoDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.setValue(response.body());
                    cacheDeviceFromDto(response.body());
                }
            }

            @Override
            public void onFailure(Call<DeviceInfoDto> call, Throwable t) {
                loadCachedDeviceAsDto(result, id);
            }
        });
        return result;
    }

    /**
     * 获取未关联设备列表
     * @return 可用设备列表LiveData
     */
    public LiveData<List<DeviceInfoDto>> getAvailableDevices() {
        MutableLiveData<List<DeviceInfoDto>> result = new MutableLiveData<>();
        deviceService.getAvailableDevices().enqueue(new Callback<List<DeviceInfoDto>>() {
            @Override
            public void onResponse(Call<List<DeviceInfoDto>> call, Response<List<DeviceInfoDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.setValue(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<DeviceInfoDto>> call, Throwable t) {
                result.setValue(null);
            }
        });
        return result;
    }

    /**
     * 将DeviceInfoDto转换为Device并缓存到Room（后台线程执行）
     * @param dto 设备信息DTO
     */
    private void cacheDeviceFromDto(DeviceInfoDto dto) {
        if (deviceDao != null && dto != null && dto.getDevice() != null) {
            Executors.newSingleThreadExecutor().execute(() -> deviceDao.insert(dto.getDevice()));
        }
    }

    /**
     * 从Room缓存加载设备详情作为DTO（离线回退）
     * @param result 结果LiveData
     * @param id 设备ID
     */
    private void loadCachedDeviceAsDto(MutableLiveData<DeviceInfoDto> result, long id) {
        if (deviceDao != null) {
            deviceDao.getDeviceById(id).observeForever(device -> {
                if (device != null) {
                    DeviceInfoDto dto = new DeviceInfoDto();
                    dto.setDevice(device);
                    result.setValue(dto);
                } else {
                    result.setValue(null);
                }
            });
        } else {
            result.setValue(null);
        }
    }
}
