package com.smartwatch.monitor.ui.alarm;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.LatLng;
import com.google.android.material.button.MaterialButton;
import com.smartwatch.monitor.R;
import com.smartwatch.monitor.utils.MapHelper;
import java.util.Map;

/**
 * 报警详情Fragment，显示报警详细信息、地图位置和处理操作
 */
public class AlarmDetailFragment extends Fragment {

    private AlarmViewModel viewModel;
    private MapView mapView;
    private AMap aMap;
    private TextView tvAlarmType;
    private TextView tvAlarmLevel;
    private TextView tvAlarmStatus;
    private TextView tvAlarmTime;
    private TextView tvDeviceImei;
    private TextView tvPatientName;
    private TextView tvAddress;
    private TextView tvAlarmData;
    private MaterialButton btnHandle;
    private MaterialButton btnMarkRead;
    private ProgressBar progressBar;
    private long alarmId = -1;

    /**
     * 创建Fragment视图
     * @param inflater 布局填充器
     * @param container 父容器
     * @param savedInstanceState 保存的实例状态
     * @return 创建的视图
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_alarm_detail, container, false);
    }

    /**
     * 视图创建后回调，初始化组件和加载数据
     * @param view Fragment视图
     * @param savedInstanceState 保存的实例状态
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view, savedInstanceState);
        initViewModel();
        loadAlarmDetail();
    }

    /**
     * 初始化视图组件
     * @param view 根视图
     * @param savedInstanceState 保存的实例状态
     */
    private void initViews(View view, Bundle savedInstanceState) {
        mapView = view.findViewById(R.id.map_view_alarm);
        if (mapView != null) {
            mapView.onCreate(savedInstanceState);
            aMap = mapView.getMap();
            MapHelper.setupMap(aMap);
        }

        tvAlarmType = view.findViewById(R.id.tv_alarm_type);
        tvAlarmLevel = view.findViewById(R.id.tv_alarm_level);
        tvAlarmStatus = view.findViewById(R.id.tv_alarm_status);
        tvAlarmTime = view.findViewById(R.id.tv_alarm_time);
        tvDeviceImei = view.findViewById(R.id.tv_device_imei);
        tvPatientName = view.findViewById(R.id.tv_patient_name);
        tvAddress = view.findViewById(R.id.tv_address);
        tvAlarmData = view.findViewById(R.id.tv_alarm_data);
        btnHandle = view.findViewById(R.id.btn_handle);
        btnMarkRead = view.findViewById(R.id.btn_mark_read);
        progressBar = view.findViewById(R.id.progress_bar);

        btnHandle.setOnClickListener(v -> showHandleDialog());
        btnMarkRead.setOnClickListener(v -> markAsRead());
    }

    /**
     * 初始化ViewModel并观察数据
     */
    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(AlarmViewModel.class);
        viewModel.getSelectedAlarm().observe(getViewLifecycleOwner(), this::updateAlarmDetail);
        viewModel.getOperationResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null && result) {
                Toast.makeText(getContext(), "操作成功", Toast.LENGTH_SHORT).show();
            }
        });
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            if (loading != null && progressBar != null) {
                progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            }
        });
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 加载报警详情数据
     */
    private void loadAlarmDetail() {
        if (getArguments() != null) {
            alarmId = getArguments().getLong("alarmId", -1);
            if (alarmId > 0) {
                viewModel.loadAlarmDetail(alarmId);
            }
        }
    }

    /**
     * 更新报警详情显示
     * @param alarm 报警数据
     */
    private void updateAlarmDetail(Map<String, Object> alarm) {
        if (alarm == null) return;

        tvAlarmType.setText(getAlarmTypeLabel(String.valueOf(alarm.getOrDefault("alarmType", ""))));
        tvAlarmLevel.setText(getAlarmLevelLabel(String.valueOf(alarm.getOrDefault("alarmLevel", ""))));
        tvAlarmStatus.setText(getStatusLabel(String.valueOf(alarm.getOrDefault("status", ""))));
        tvAlarmTime.setText(String.valueOf(alarm.getOrDefault("triggeredTime", "")));
        tvDeviceImei.setText(String.valueOf(alarm.getOrDefault("imei", "")));
        tvPatientName.setText(String.valueOf(alarm.getOrDefault("patientName", "")));
        tvAddress.setText(String.valueOf(alarm.getOrDefault("address", "")));
        tvAlarmData.setText(String.valueOf(alarm.getOrDefault("alarmData", "")));

        Object latObj = alarm.get("latitude");
        Object lngObj = alarm.get("longitude");
        if (latObj != null && lngObj != null && aMap != null) {
            double lat = ((Number) latObj).doubleValue();
            double lng = ((Number) lngObj).doubleValue();
            MapHelper.moveCamera(aMap, lat, lng, 16);
            MapHelper.addMarker(aMap, lat, lng, "报警位置");
        }

        String status = String.valueOf(alarm.getOrDefault("status", ""));
        if ("handled".equals(status) || "false_alarm".equals(status)) {
            btnHandle.setEnabled(false);
            btnHandle.setText("已处理");
        }
    }

    /**
     * 显示处理报警对话框
     */
    private void showHandleDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_handle_alarm, null);
        String[] resultOptions = {"已确认处理", "误报", "需进一步跟进"};

        new AlertDialog.Builder(getContext())
                .setTitle("处理报警")
                .setView(dialogView)
                .setSingleChoiceItems(resultOptions, 0, null)
                .setPositiveButton("确认处理", (dialog, which) -> {
                    int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                    String resultStr;
                    String status;
                    switch (selectedPosition) {
                        case 1:
                            resultStr = "false_alarm";
                            status = "false_alarm";
                            break;
                        case 2:
                            resultStr = "follow_up";
                            status = "handled";
                            break;
                        default:
                            resultStr = "confirmed";
                            status = "handled";
                            break;
                    }
                    EditText etRemark = dialogView.findViewById(R.id.et_handle_remark);
                    String remark = etRemark != null ? etRemark.getText().toString().trim() : "";
                    if (alarmId > 0) {
                        viewModel.handleAlarm(alarmId, status, resultStr, remark);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 标记报警为已读
     */
    private void markAsRead() {
        if (alarmId > 0) {
            viewModel.markAsRead(alarmId);
        }
    }

    /**
     * 获取报警类型中文标签
     * @param type 报警类型
     * @return 中文标签
     */
    private String getAlarmTypeLabel(String type) {
        switch (type) {
            case "fence_breach": return "围栏越界";
            case "low_battery": return "低电量";
            case "sos": return "SOS求救";
            case "fall": return "跌倒报警";
            case "heart_rate": return "心率异常";
            default: return type;
        }
    }

    /**
     * 获取报警级别中文标签
     * @param level 报警级别
     * @return 中文标签
     */
    private String getAlarmLevelLabel(String level) {
        switch (level) {
            case "critical": return "紧急";
            case "warning": return "警告";
            case "info": return "提示";
            default: return level;
        }
    }

    /**
     * 获取报警状态中文标签
     * @param status 报警状态
     * @return 中文标签
     */
    private String getStatusLabel(String status) {
        switch (status) {
            case "pending": return "待处理";
            case "handled": return "已处理";
            case "false_alarm": return "误报";
            default: return status;
        }
    }

    /**
     * 处理地图生命周期 - Activity恢复时调用
     */
    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    /**
     * 处理地图生命周期 - Activity暂停时调用
     */
    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }

    /**
     * 处理地图生命周期 - Activity销毁时调用
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mapView != null) mapView.onDestroy();
    }

    /**
     * 处理地图生命周期 - 低内存时调用
     */
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) mapView.onLowMemory();
    }

    /**
     * 保存实例状态
     * @param outState 保存的状态
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) mapView.onSaveInstanceState(outState);
    }
}
