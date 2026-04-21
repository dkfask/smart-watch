package com.smartwatch.monitor.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.smartwatch.monitor.api.ApiClient;
import com.smartwatch.monitor.api.AlarmService;
import com.smartwatch.monitor.data.AlertDao;
import com.smartwatch.monitor.data.AppDatabase;
import com.smartwatch.monitor.model.Alarm;
import com.smartwatch.monitor.model.AlarmStatsDto;
import com.smartwatch.monitor.model.Alert;
import com.smartwatch.monitor.model.PageResponse;
import com.smartwatch.monitor.utils.Constants;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 报警数据仓库，负责处理报警相关数据操作，支持Room离线缓存
 */
public class AlarmRepository {

    private final AlarmService alarmService;
    private final AlertDao alertDao;

    /**
     * 构造函数，初始化报警服务
     */
    public AlarmRepository() {
        alarmService = ApiClient.getInstance().createService(AlarmService.class);
        alertDao = null;
    }

    /**
     * 构造函数，初始化报警服务和Room DAO（带数据库支持）
     * @param application 应用实例
     */
    public AlarmRepository(Application application) {
        alarmService = ApiClient.getInstance().createService(AlarmService.class);
        alertDao = AppDatabase.getInstance(application).alertDao();
    }

    /**
     * 获取报警列表
     * @param page 页码
     * @param status 报警状态过滤
     * @return 报警分页列表LiveData
     */
    public LiveData<PageResponse<Map<String, Object>>> getAlarms(int page, String status) {
        MutableLiveData<PageResponse<Map<String, Object>>> result = new MutableLiveData<>();
        alarmService.list(page, Constants.DEFAULT_PAGE_SIZE, null, null, status)
                .enqueue(new Callback<PageResponse<Map<String, Object>>>() {
                    @Override
                    public void onResponse(Call<PageResponse<Map<String, Object>>> call,
                                           Response<PageResponse<Map<String, Object>>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            result.setValue(response.body());
                        }
                    }

                    @Override
                    public void onFailure(Call<PageResponse<Map<String, Object>>> call, Throwable t) {
                        result.setValue(null);
                    }
                });
        return result;
    }

    /**
     * 获取报警详情
     * @param id 报警ID
     * @return 报警详情LiveData
     */
    public LiveData<Map<String, Object>> getAlarm(long id) {
        MutableLiveData<Map<String, Object>> result = new MutableLiveData<>();
        alarmService.get(id).enqueue(new Callback<Map<String, Object>>() {
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
     * 获取报警统计数据
     * @return 报警统计LiveData
     */
    public LiveData<AlarmStatsDto> getAlarmStats() {
        MutableLiveData<AlarmStatsDto> result = new MutableLiveData<>();
        alarmService.stats().enqueue(new Callback<AlarmStatsDto>() {
            @Override
            public void onResponse(Call<AlarmStatsDto> call, Response<AlarmStatsDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.setValue(response.body());
                }
            }

            @Override
            public void onFailure(Call<AlarmStatsDto> call, Throwable t) {
                result.setValue(null);
            }
        });
        return result;
    }

    /**
     * 标记报警为已读
     * @param id 报警ID
     * @return 操作结果LiveData
     */
    public LiveData<Boolean> markRead(long id) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        alarmService.markRead(id, true).enqueue(new Callback<Void>() {
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
     * 处理报警
     * @param id 报警ID
     * @param status 处理状态
     * @param resultStr 处理结果
     * @param remark 处理备注
     * @return 操作结果LiveData
     */
    public LiveData<Boolean> handleAlarm(long id, String status, String resultStr, String remark) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        Map<String, String> body = new java.util.HashMap<>();
        body.put("status", status);
        body.put("result", resultStr);
        body.put("remark", remark);
        alarmService.handle(id, body).enqueue(new Callback<Void>() {
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
     * 获取未读报警数量
     * @return 未读数量LiveData
     */
    public LiveData<Long> getUnreadCount() {
        MutableLiveData<Long> result = new MutableLiveData<>();
        alarmService.unreadCount(null, null).enqueue(new Callback<Map<String, Long>>() {
            @Override
            public void onResponse(Call<Map<String, Long>> call, Response<Map<String, Long>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Long count = response.body().get("count");
                    result.setValue(count != null ? count : 0L);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Long>> call, Throwable t) {
                loadCachedUnreadCount(result);
            }
        });
        return result;
    }

    /**
     * 获取最近的报警记录列表
     * @param limit 获取数量限制
     * @return 报警列表LiveData
     */
    public LiveData<List<Map<String, Object>>> getRecentAlarms(int limit) {
        MutableLiveData<List<Map<String, Object>>> result = new MutableLiveData<>();
        alarmService.recent(limit).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        com.google.gson.Gson gson = new com.google.gson.Gson();
                        String json = gson.toJson(response.body());
                        java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<Map<String, Object>>>() {}.getType();
                        List<Map<String, Object>> alarms = gson.fromJson(json, listType);
                        result.setValue(alarms != null ? alarms : new ArrayList<>());
                        cacheAlertsFromMaps(alarms);
                    } catch (Exception e) {
                        result.setValue(new ArrayList<>());
                    }
                } else {
                    result.setValue(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                result.setValue(new ArrayList<>());
            }
        });
        return result;
    }

    /**
     * 将报警Map列表转换为Alert对象并缓存到Room（后台线程执行）
     * @param alarmMaps 报警Map列表
     */
    private void cacheAlertsFromMaps(List<Map<String, Object>> alarmMaps) {
        if (alertDao != null && alarmMaps != null) {
            Executors.newSingleThreadExecutor().execute(() -> {
                com.google.gson.Gson gson = new com.google.gson.Gson();
                List<Alert> alerts = new ArrayList<>();
                for (Map<String, Object> map : alarmMaps) {
                    try {
                        String json = gson.toJson(map);
                        Alert alert = gson.fromJson(json, Alert.class);
                        if (alert != null) {
                            alerts.add(alert);
                        }
                    } catch (Exception ignored) {}
                }
                if (!alerts.isEmpty()) {
                    alertDao.insertAll(alerts);
                }
            });
        }
    }

    /**
     * 从Room缓存获取未读报警数量（离线回退）
     * @param result 结果LiveData
     */
    private void loadCachedUnreadCount(MutableLiveData<Long> result) {
        if (alertDao != null) {
            alertDao.getUnreadAlerts().observeForever(alerts -> {
                result.setValue(alerts != null ? (long) alerts.size() : 0L);
            });
        } else {
            result.setValue(0L);
        }
    }
}
