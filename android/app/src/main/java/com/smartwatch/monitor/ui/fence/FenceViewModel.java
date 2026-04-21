package com.smartwatch.monitor.ui.fence;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.smartwatch.monitor.model.FenceCreateRequest;
import com.smartwatch.monitor.model.FenceDto;
import com.smartwatch.monitor.repository.FenceRepository;
import java.util.List;

/**
 * 围栏ViewModel，处理围栏列表和操作数据逻辑
 */
public class FenceViewModel extends AndroidViewModel {

    private final FenceRepository fenceRepository;
    private final MutableLiveData<List<FenceDto>> fences = new MutableLiveData<>();
    private final MutableLiveData<List<FenceDto>> activeFences = new MutableLiveData<>();
    private final MutableLiveData<FenceDto> selectedFence = new MutableLiveData<>();
    private final MutableLiveData<Boolean> operationResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    /**
     * 构造函数，初始化围栏仓库
     * @param application 应用实例
     */
    public FenceViewModel(@NonNull Application application) {
        super(application);
        fenceRepository = new FenceRepository();
    }

    /**
     * 加载所有围栏列表
     */
    public void loadFences() {
        isLoading.setValue(true);
        fenceRepository.getFences().observeForever(result -> {
            isLoading.setValue(false);
            if (result != null) {
                fences.setValue(result);
            } else {
                errorMessage.setValue("加载围栏列表失败");
            }
        });
    }

    /**
     * 加载活跃围栏列表
     */
    public void loadActiveFences() {
        isLoading.setValue(true);
        fenceRepository.getActiveFences().observeForever(result -> {
            isLoading.setValue(false);
            if (result != null) {
                activeFences.setValue(result);
            } else {
                errorMessage.setValue("加载活跃围栏失败");
            }
        });
    }

    /**
     * 创建围栏
     * @param request 围栏创建请求
     */
    public void createFence(FenceCreateRequest request) {
        isLoading.setValue(true);
        fenceRepository.createFence(request).observeForever(result -> {
            isLoading.setValue(false);
            if (result != null && result) {
                operationResult.setValue(true);
                loadFences();
            } else {
                errorMessage.setValue("创建围栏失败");
                operationResult.setValue(false);
            }
        });
    }

    /**
     * 更新围栏
     * @param id 围栏ID
     * @param request 围栏更新请求
     */
    public void updateFence(long id, FenceCreateRequest request) {
        isLoading.setValue(true);
        fenceRepository.updateFence(id, request).observeForever(result -> {
            isLoading.setValue(false);
            if (result != null && result) {
                operationResult.setValue(true);
                loadFences();
            } else {
                errorMessage.setValue("更新围栏失败");
                operationResult.setValue(false);
            }
        });
    }

    /**
     * 删除围栏
     * @param id 围栏ID
     */
    public void deleteFence(long id) {
        isLoading.setValue(true);
        fenceRepository.deleteFence(id).observeForever(result -> {
            isLoading.setValue(false);
            if (result != null && result) {
                operationResult.setValue(true);
                loadFences();
            } else {
                errorMessage.setValue("删除围栏失败");
                operationResult.setValue(false);
            }
        });
    }

    /**
     * 加载围栏详情
     * @param id 围栏ID
     */
    public void loadFenceDetail(long id) {
        fenceRepository.getFence(id).observeForever(result -> {
            selectedFence.setValue(result);
        });
    }

    public LiveData<List<FenceDto>> getFences() { return fences; }
    public LiveData<List<FenceDto>> getActiveFences() { return activeFences; }
    public LiveData<FenceDto> getSelectedFence() { return selectedFence; }
    public LiveData<Boolean> getOperationResult() { return operationResult; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
}
