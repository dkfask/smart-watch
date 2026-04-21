package com.smartwatch.monitor.ui.patient;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.smartwatch.monitor.R;
import com.smartwatch.monitor.model.Patient;

/**
 * 病人详情Fragment，显示病人详细信息、关联设备和最新健康数据
 */
public class PatientDetailFragment extends Fragment {

    private PatientViewModel viewModel;
    private ProgressBar progressBar;

    private TextView tvName;
    private TextView tvGender;
    private TextView tvAge;
    private TextView tvIdCard;
    private TextView tvWard;
    private TextView tvBed;
    private TextView tvPhone;
    private TextView tvEmergencyContact;
    private TextView tvEmergencyPhone;
    private TextView tvDiagnosis;
    private TextView tvStatus;
    private TextView tvAdmissionDate;

    private LinearLayout layoutDevices;
    private LinearLayout layoutHealthData;
    private TextView tvNoDevices;
    private TextView tvNoHealthData;

    private long patientId = -1;

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
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.fragment_patient_detail, container, false);
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
        loadPatientDetail();
    }

    /**
     * 创建选项菜单（编辑/删除按钮）
     * @param menu 菜单
     * @param inflater 菜单填充器
     */
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_patient_detail, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    /**
     * 处理选项菜单点击事件
     * @param item 菜单项
     * @return 是否消费了事件
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_edit) {
            Toast.makeText(getContext(), "编辑病人功能开发中", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_delete) {
            showDeleteConfirmDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 显示删除确认对话框
     */
    private void showDeleteConfirmDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("确认删除")
                .setMessage("确定要删除该病人吗？此操作不可撤销。")
                .setPositiveButton("删除", (dialog, which) -> {
                    if (patientId > 0) {
                        viewModel.deletePatient(patientId);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 初始化视图组件
     * @param view 根视图
     */
    private void initViews(View view) {
        progressBar = view.findViewById(R.id.progress_bar);
        tvName = view.findViewById(R.id.tv_detail_name);
        tvGender = view.findViewById(R.id.tv_detail_gender);
        tvAge = view.findViewById(R.id.tv_detail_age);
        tvIdCard = view.findViewById(R.id.tv_detail_id_card);
        tvWard = view.findViewById(R.id.tv_detail_ward);
        tvBed = view.findViewById(R.id.tv_detail_bed);
        tvPhone = view.findViewById(R.id.tv_detail_phone);
        tvEmergencyContact = view.findViewById(R.id.tv_detail_emergency_contact);
        tvEmergencyPhone = view.findViewById(R.id.tv_detail_emergency_phone);
        tvDiagnosis = view.findViewById(R.id.tv_detail_diagnosis);
        tvStatus = view.findViewById(R.id.tv_detail_status);
        tvAdmissionDate = view.findViewById(R.id.tv_detail_admission_date);
        layoutDevices = view.findViewById(R.id.layout_devices);
        layoutHealthData = view.findViewById(R.id.layout_health_data);
        tvNoDevices = view.findViewById(R.id.tv_no_devices);
        tvNoHealthData = view.findViewById(R.id.tv_no_health_data);

        view.findViewById(R.id.btn_view_health_data).setOnClickListener(v -> navigateToHealthData());
    }

    /**
     * 导航到健康数据页面
     */
    private void navigateToHealthData() {
        if (patientId > 0) {
            Bundle args = new Bundle();
            args.putLong("patientId", patientId);
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_patientDetail_to_healthData, args);
        }
    }

    /**
     * 初始化ViewModel并观察数据变化
     */
    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(PatientViewModel.class);

        viewModel.getPatientDetail().observe(getViewLifecycleOwner(), this::updatePatientDetail);

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

        viewModel.getOperationSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success != null && success) {
                Toast.makeText(getContext(), "操作成功", Toast.LENGTH_SHORT).show();
                Navigation.findNavController(requireView()).navigateUp();
            }
        });
    }

    /**
     * 加载病人详情数据
     */
    private void loadPatientDetail() {
        if (getArguments() != null) {
            patientId = getArguments().getLong("patientId", -1);
            if (patientId > 0) {
                viewModel.loadPatientDetail(patientId);
            }
        }
    }

    /**
     * 更新病人详情显示
     * @param patient 病人数据
     */
    private void updatePatientDetail(Patient patient) {
        if (patient != null) {
            tvName.setText(patient.getName());
            tvGender.setText(formatGender(patient.getGender()));
            tvAge.setText(patient.getAge() != null ? String.valueOf(patient.getAge()) : "--");
            tvIdCard.setText(patient.getIdCard() != null ? patient.getIdCard() : "--");
            tvWard.setText(patient.getWard() != null ? patient.getWard() : "--");
            tvBed.setText(patient.getBed() != null ? patient.getBed() : "--");
            tvPhone.setText(patient.getPhone() != null ? patient.getPhone() : "--");
            tvEmergencyContact.setText(patient.getEmergencyContact() != null ? patient.getEmergencyContact() : "--");
            tvEmergencyPhone.setText(patient.getEmergencyPhone() != null ? patient.getEmergencyPhone() : "--");
            tvDiagnosis.setText(patient.getDiagnosis() != null ? patient.getDiagnosis() : "--");
            tvStatus.setText(formatPatientStatus(patient.getStatus()));
            tvAdmissionDate.setText(patient.getAdmissionDate() != null ? patient.getAdmissionDate() : "--");

            tvNoDevices.setVisibility(View.VISIBLE);
            tvNoHealthData.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 格式化性别显示文本
     * @param gender 性别值
     * @return 格式化后的文本
     */
    private String formatGender(String gender) {
        if (gender == null) return "--";
        switch (gender) {
            case "male": return "男";
            case "female": return "女";
            default: return gender;
        }
    }

    /**
     * 格式化病人状态显示文本
     * @param status 状态值
     * @return 格式化后的文本
     */
    private String formatPatientStatus(String status) {
        if (status == null) return "未知";
        switch (status) {
            case "admitted": return "住院中";
            case "discharged": return "已出院";
            default: return status;
        }
    }
}
