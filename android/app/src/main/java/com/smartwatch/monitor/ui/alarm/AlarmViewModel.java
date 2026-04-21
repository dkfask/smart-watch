package com.smartwatch.monitor.ui.alarm;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.smartwatch.monitor.model.AlarmStatsDto;
import com.smartwatch.monitor.model.PageResponse;
import com.smartwatch.monitor.repository.AlarmRepository;
import com.smartwatch.monitor.utils.Constants;
import java.util.Map;

/**
 * 报警ViewModel，处理报警列表、详情和操作数据逻辑
 */
public class AlarmViewModel extends AndroidViewModel {

    private final AlarmRepository alarmRepository;
    private final MutableLiveData<PageResponse<Map<String, Object>>> alarms = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Object>> selectedAlarm = new MutableLiveData<>();
    private final MutableLiveData<AlarmStatsDto> alarmStats = new MutableLiveData<>();
    private final MutableLiveData<Boolean> operationResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private int currentPage = 0;
    private String currentStatus = null;

    /**
     * 构造函数，初始化报警仓库
     * @param application 应用实例
     */
    public AlarmViewModel(@NonNull Application application) {
        super(application);
        alarmRepository = new AlarmRepository();
    }

    /**
     * 加载报警列表
     * @param page 页码
     * @param size 每页数量
     * @param status 报警状态过滤
     */
    public void loadAlarms(int page, int size, String status) {
        isLoading.setValue(true);
        currentStatus = status;
        currentPage = page;
        alarmRepository.getAlarms(page, status).observeForever(result -> {
            isLoading.setValue(false);
            if (result != null) {
                alarms.setValue(result);
            } else {
                errorMessage.setValue("加载报警列表失败");
            }
        });
    }

    /**
     * 加载报警列表（使用默认分页参数）
     * @param status 报警状态过滤
     */
    public void loadAlarms(String status) {
        loadAlarms(0, Constants.DEFAULT_PAGE_SIZE, status);
    }

    /**
     * 加载报警详情
     * @param id 报警ID
     */
    public void loadAlarmDetail(long id) {
        isLoading.setValue(true);
        alarmRepository.getAlarm(id).observeForever(result -> {
            isLoading.setValue(false);
            if (result != null) {
                selectedAlarm.setValue(result);
            } else {
                errorMessage.setValue("加载报警详情失败");
            }
        });
    }

    /**
     * 标记报警为已读
     * @param id 报警ID
     */
    public void markAsRead(long id) {
        isLoading.setValue(true);
        alarmRepository.markRead(id).observeForever(result -> {
            isLoading.setValue(false);
            if (result != null && result) {
                operationResult.setValue(true);
                loadAlarmDetail(id);
            } else {
                errorMessage.setValue("标记已读失败");
                operationResult.setValue(false);
            }
        });
    }

    /**
     * 处理报警
     * @param id 报警ID
     * @param status 处理状态
     * @param resultStr 处理结果
     * @param remark 处理备注
     */
    public void handleAlarm(long id, String status, String resultStr, String remark) {
        isLoading.setValue(true);
        alarmRepository.handleAlarm(id, status, resultStr, remark).observeForever(result -> {
            isLoading.setValue(false);
            if (result != null && result) {
                operationResult.setValue(true);
                loadAlarmDetail(id);
            } else {
                errorMessage.setValue("处理报警失败");
                operationResult.setValue(false);
            }
        });
    }

    /**
     * 加载报警统计数据
     */
    public void loadAlarmStats() {
        alarmRepository.getAlarmStats().observeForever(result -> {
            if (result != null) {
                alarmStats.setValue(result);
            }
        });
    }

    /**
     * 刷新当前列表
     */
    public void refresh() {
        currentPage = 0;
        loadAlarms(currentStatus);
    }

    /**
     * 加载下一页
     */
    public void loadNextPage() {
        currentPage++;
        loadAlarms(currentStatus);
    }

    public LiveData<PageResponse<Map<String, Object>>> getAlarms() { return alarms; }
    public LiveData<Map<String, Object>> getSelectedAlarm() { return selectedAlarm; }
    public LiveData<AlarmStatsDto> getAlarmStats() { return alarmStats; }
    public LiveData<Boolean> getOperationResult() { return operationResult; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public int getCurrentPage() { return currentPage; }
}
