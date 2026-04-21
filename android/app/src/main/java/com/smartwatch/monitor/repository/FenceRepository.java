package com.smartwatch.monitor.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.smartwatch.monitor.api.ApiClient;
import com.smartwatch.monitor.api.FenceService;
import com.smartwatch.monitor.model.FenceCreateRequest;
import com.smartwatch.monitor.model.FenceDto;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 围栏数据仓库，负责处理围栏相关数据操作
 */
public class FenceRepository {

    private final FenceService fenceService;

    /**
     * 构造函数，初始化围栏服务
     */
    public FenceRepository() {
        fenceService = ApiClient.getInstance().createService(FenceService.class);
    }

    /**
     * 获取围栏列表
     * @return 围栏列表LiveData
     */
    public LiveData<List<FenceDto>> getFences() {
        MutableLiveData<List<FenceDto>> result = new MutableLiveData<>();
        fenceService.list(20, 0).enqueue(new Callback<List<FenceDto>>() {
            @Override
            public void onResponse(Call<List<FenceDto>> call, Response<List<FenceDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.setValue(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<FenceDto>> call, Throwable t) {
                result.setValue(null);
            }
        });
        return result;
    }

    /**
     * 获取活跃围栏列表
     * @return 活跃围栏列表LiveData
     */
    public LiveData<List<FenceDto>> getActiveFences() {
        MutableLiveData<List<FenceDto>> result = new MutableLiveData<>();
        fenceService.getActiveFences().enqueue(new Callback<List<FenceDto>>() {
            @Override
            public void onResponse(Call<List<FenceDto>> call, Response<List<FenceDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.setValue(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<FenceDto>> call, Throwable t) {
                result.setValue(null);
            }
        });
        return result;
    }

    /**
     * 获取围栏详情
     * @param id 围栏ID
     * @return 围栏详情LiveData
     */
    public LiveData<FenceDto> getFence(long id) {
        MutableLiveData<FenceDto> result = new MutableLiveData<>();
        fenceService.get(id).enqueue(new Callback<FenceDto>() {
            @Override
            public void onResponse(Call<FenceDto> call, Response<FenceDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.setValue(response.body());
                }
            }

            @Override
            public void onFailure(Call<FenceDto> call, Throwable t) {
                result.setValue(null);
            }
        });
        return result;
    }

    /**
     * 创建围栏
     * @param request 围栏创建请求
     * @return 创建结果LiveData
     */
    public LiveData<Boolean> createFence(FenceCreateRequest request) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        fenceService.create(request).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                result.setValue(response.isSuccessful());
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                result.setValue(false);
            }
        });
        return result;
    }

    /**
     * 更新围栏
     * @param id 围栏ID
     * @param request 围栏更新请求
     * @return 更新结果LiveData
     */
    public LiveData<Boolean> updateFence(long id, FenceCreateRequest request) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        fenceService.update(id, request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                result.setValue(response.isSuccessful());
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                result.setValue(false);
            }
        });
        return result;
    }

    /**
     * 删除围栏
     * @param id 围栏ID
     * @return 删除结果LiveData
     */
    public LiveData<Boolean> deleteFence(long id) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        fenceService.delete(id).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                result.setValue(response.isSuccessful());
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                result.setValue(false);
            }
        });
        return result;
    }

    /**
     * 获取活跃围栏数量
     * @return 活跃围栏数量LiveData
     */
    public LiveData<Integer> getActiveFenceCount() {
        MutableLiveData<Integer> result = new MutableLiveData<>();
        fenceService.getActiveFences().enqueue(new Callback<List<FenceDto>>() {
            @Override
            public void onResponse(Call<List<FenceDto>> call, Response<List<FenceDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.setValue(response.body().size());
                } else {
                    result.setValue(0);
                }
            }

            @Override
            public void onFailure(Call<List<FenceDto>> call, Throwable t) {
                result.setValue(0);
            }
        });
        return result;
    }
}
