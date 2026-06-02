package com.smartwatch.monitor.ui.alarm;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.smartwatch.monitor.common.UiState;
import com.smartwatch.monitor.model.AlarmStatsDto;
import com.smartwatch.monitor.model.PageResponse;
import com.smartwatch.monitor.repository.AlarmRepository;
import com.smartwatch.monitor.utils.Constants;
import java.util.Map;

public class AlarmViewModel extends AndroidViewModel {

    private final AlarmRepository alarmRepository;
    private final MutableLiveData<PageResponse<Map<String, Object>>> alarms = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Object>> selectedAlarm = new MutableLiveData<>();
    private final MutableLiveData<AlarmStatsDto> alarmStats = new MutableLiveData<>();
    private final MutableLiveData<Boolean> operationResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<UiState<PageResponse<Map<String, Object>>>> alarmsState = new MutableLiveData<>();
    private int currentPage = 0;
    private String currentStatus = null;

    public AlarmViewModel(@NonNull Application application) {
        super(application);
        alarmRepository = new AlarmRepository();
    }

    public void loadAlarms(int page, int size, String status) {
        isLoading.setValue(true);
        alarmsState.setValue(UiState.loading());
        currentStatus = status;
        currentPage = page;
        alarmRepository.getAlarmsResult(page, status).observeForever(result -> {
            isLoading.setValue(false);
            if (result != null && result.isSuccess() && result.getData() != null) {
                PageResponse<Map<String, Object>> pageData = result.getData();
                alarms.setValue(pageData);
                if (pageData.getContent() == null || pageData.getContent().isEmpty()) {
                    alarmsState.setValue(UiState.empty("No alarms found"));
                } else {
                    alarmsState.setValue(UiState.success(pageData));
                }
                return;
            }
            String msg = result != null && result.getError() != null ? result.getError() : "Failed to load alarms";
            errorMessage.setValue(msg);
            alarmsState.setValue(UiState.error(msg));
        });
    }

    public void loadAlarms(String status) {
        loadAlarms(0, Constants.DEFAULT_PAGE_SIZE, status);
    }

    public void loadAlarmDetail(long id) {
        isLoading.setValue(true);
        alarmRepository.getAlarm(id).observeForever(result -> {
            isLoading.setValue(false);
            if (result != null) {
                selectedAlarm.setValue(result);
            } else {
                errorMessage.setValue("Failed to load alarm detail");
            }
        });
    }

    public void markAsRead(long id) {
        isLoading.setValue(true);
        alarmRepository.markRead(id).observeForever(result -> {
            isLoading.setValue(false);
            if (result != null && result) {
                operationResult.setValue(true);
                loadAlarmDetail(id);
            } else {
                errorMessage.setValue("Failed to mark alarm as read");
                operationResult.setValue(false);
            }
        });
    }

    public void handleAlarm(long id, String status, String resultStr, String remark) {
        isLoading.setValue(true);
        alarmRepository.handleAlarm(id, status, resultStr, remark).observeForever(result -> {
            isLoading.setValue(false);
            if (result != null && result) {
                operationResult.setValue(true);
                loadAlarmDetail(id);
            } else {
                errorMessage.setValue("Failed to handle alarm");
                operationResult.setValue(false);
            }
        });
    }

    public void loadAlarmStats() {
        alarmRepository.getAlarmStats().observeForever(result -> {
            if (result != null) {
                alarmStats.setValue(result);
            }
        });
    }

    public void refresh() {
        currentPage = 0;
        loadAlarms(currentStatus);
    }

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
    public LiveData<UiState<PageResponse<Map<String, Object>>>> getAlarmsState() { return alarmsState; }
    public int getCurrentPage() { return currentPage; }
}
