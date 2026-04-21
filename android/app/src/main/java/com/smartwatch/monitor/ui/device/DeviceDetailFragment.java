package com.smartwatch.monitor.ui.device;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.button.MaterialButton;
import com.smartwatch.monitor.R;
import com.smartwatch.monitor.model.DeviceInfoDto;
import java.util.HashMap;
import java.util.Map;

/**
 * 设备详情Fragment，显示设备详细信息、在线状态、关联病人、位置和远程控制面板
 */
public class DeviceDetailFragment extends Fragment {

    private DeviceViewModel viewModel;

    private ProgressBar progressBar;
    private TextView tvImei;
    private TextView tvStatus;
    private TextView tvBattery;
    private TextView tvLastLocation;
    private TextView tvPatientName;
    private TextView tvFirmwareVersion;
    private TextView tvDeviceModel;
    private View viewStatusDot;

    private MaterialButton btnLocate;
    private MaterialButton btnHeartRate;
    private MaterialButton btnBloodPressure;
    private MaterialButton btnBloodOxygen;
    private MaterialButton btnTemperature;
    private MaterialButton btnRestart;
    private MaterialButton btnShutdown;
    private MaterialButton btnFactoryReset;
    private MaterialButton btnViewLocation;

    private String currentImei = "";

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
        return inflater.inflate(R.layout.fragment_device_detail, container, false);
    }

    /**
     * 视图创建后回调，初始化组件和加载数据
     * @param view Fragment视图
     * @param savedInstanceState 保存的实例状态
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        initViewModel();
        loadDeviceDetail();
    }

    /**
     * 初始化视图组件
     * @param view 根视图
     */
    private void initViews(View view) {
        progressBar = view.findViewById(R.id.progress_bar);
        tvImei = view.findViewById(R.id.tv_detail_imei);
        tvStatus = view.findViewById(R.id.tv_detail_status);
        tvBattery = view.findViewById(R.id.tv_detail_battery);
        tvLastLocation = view.findViewById(R.id.tv_detail_patient_info);
        tvPatientName = view.findViewById(R.id.tv_detail_patient_name);
        tvFirmwareVersion = view.findViewById(R.id.tv_detail_firmware);
        tvDeviceModel = view.findViewById(R.id.tv_detail_model);
        viewStatusDot = view.findViewById(R.id.view_detail_status_dot);

        btnViewLocation = view.findViewById(R.id.btn_view_location);
        btnLocate = view.findViewById(R.id.btn_locate);
        btnHeartRate = view.findViewById(R.id.btn_heart_rate);
        btnBloodPressure = view.findViewById(R.id.btn_blood_pressure);
        btnBloodOxygen = view.findViewById(R.id.btn_blood_oxygen);
        btnTemperature = view.findViewById(R.id.btn_temperature);
        btnRestart = view.findViewById(R.id.btn_restart);
        btnShutdown = view.findViewById(R.id.btn_shutdown);
        btnFactoryReset = view.findViewById(R.id.btn_factory_reset);

        btnViewLocation.setOnClickListener(v -> {
            Toast.makeText(getContext(), "查看位置功能开发中", Toast.LENGTH_SHORT).show();
        });

        btnLocate.setOnClickListener(v -> sendCommand("bp16", "定位"));
        btnHeartRate.setOnClickListener(v -> sendCommand("bpxl", "心率测量"));
        btnBloodPressure.setOnClickListener(v -> sendCommand("bpxy", "血压测量"));
        btnBloodOxygen.setOnClickListener(v -> sendCommand("bpxz", "血氧测量"));
        btnTemperature.setOnClickListener(v -> sendCommand("bpxx", "体温测量"));
        btnRestart.setOnClickListener(v -> confirmAndSendCommand("bp18", "重启设备"));
        btnShutdown.setOnClickListener(v -> confirmAndSendCommand("bp31", "关机"));
        btnFactoryReset.setOnClickListener(v -> confirmAndSendCommand("bp17", "恢复出厂设置"));
    }

    /**
     * 发送指令到设备
     * @param command 指令类型
     * @param commandName 指令中文名称
     */
    private void sendCommand(String command, String commandName) {
        if (currentImei.isEmpty()) {
            Toast.makeText(getContext(), "设备IMEI不可用", Toast.LENGTH_SHORT).show();
            return;
        }
        viewModel.sendDownlinkCommand(currentImei, command, null);
        Toast.makeText(getContext(), commandName + "指令已发送", Toast.LENGTH_SHORT).show();
    }

    /**
     * 显示确认对话框后发送危险指令
     * @param command 指令类型
     * @param commandName 指令中文名称
     */
    private void confirmAndSendCommand(String command, String commandName) {
        if (currentImei.isEmpty()) {
            Toast.makeText(getContext(), "设备IMEI不可用", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("确认操作")
                .setMessage("确定要执行\"" + commandName + "\"吗？")
                .setPositiveButton("确定", (dialog, which) -> sendCommand(command, commandName))
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 初始化ViewModel并观察数据变化
     */
    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(DeviceViewModel.class);

        viewModel.getDeviceDetail().observe(getViewLifecycleOwner(), this::updateDeviceDetail);

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

        viewModel.getCommandResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                Object success = result.get("success");
                if (success != null && Boolean.TRUE.equals(success)) {
                    Toast.makeText(getContext(), "指令执行成功", Toast.LENGTH_SHORT).show();
                } else {
                    Object msg = result.get("message");
                    Toast.makeText(getContext(),
                            "指令结果: " + (msg != null ? msg.toString() : "未知"),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * 加载设备详情数据
     */
    private void loadDeviceDetail() {
        if (getArguments() != null) {
            long deviceId = getArguments().getLong("deviceId", -1);
            if (deviceId > 0) {
                viewModel.loadDeviceDetail(deviceId);
            }
        }
    }

    /**
     * 更新设备详情显示
     * @param device 设备数据
     */
    private void updateDeviceDetail(DeviceInfoDto device) {
        if (device != null) {
            currentImei = device.getImei() != null ? device.getImei() : "";
            tvImei.setText(device.getImei());

            boolean isOnline = device.getIsOnline() != null && device.getIsOnline();
            tvStatus.setText(isOnline ? "在线" : "离线");
            if (viewStatusDot != null) {
                viewStatusDot.setBackgroundColor(
                        ContextCompat.getColor(requireContext(),
                                isOnline ? R.color.status_online : R.color.status_offline));
            }

            tvBattery.setText(device.getBatteryLevel() != null ? device.getBatteryLevel() + "%" : "--");
            tvLastLocation.setText(device.getLastLocationTime() != null ? device.getLastLocationTime() : "--");
            tvFirmwareVersion.setText("--");
            tvDeviceModel.setText("--");

            if (device.getPatient() != null) {
                tvPatientName.setText(device.getPatient().getName());
            } else {
                tvPatientName.setText("未绑定");
            }
        }
    }
}
