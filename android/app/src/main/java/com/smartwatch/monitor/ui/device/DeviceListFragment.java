package com.smartwatch.monitor.ui.device;

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
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.smartwatch.monitor.R;
import com.smartwatch.monitor.model.DeviceInfoDto;
import com.smartwatch.monitor.model.PageResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * 设备列表Fragment，显示设备列表并支持搜索、下拉刷新和分页加载
 */
public class DeviceListFragment extends Fragment {

    private DeviceViewModel viewModel;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private DeviceAdapter adapter;

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
        return inflater.inflate(R.layout.fragment_device_list, container, false);
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
        loadData();
    }

    /**
     * 初始化视图组件
     * @param view 根视图
     */
    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_devices);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        progressBar = view.findViewById(R.id.progress_bar);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new DeviceAdapter();
        recyclerView.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(() -> {
            viewModel.refresh();
            swipeRefresh.setRefreshing(false);
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && layoutManager.findLastCompletelyVisibleItemPosition() == adapter.getItemCount() - 1) {
                    viewModel.loadNextPage();
                }
            }
        });
    }

    /**
     * 初始化ViewModel并观察数据变化
     */
    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(DeviceViewModel.class);

        viewModel.getDevices().observe(getViewLifecycleOwner(), this::updateDeviceList);

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            if (loading != null) {
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
     * 加载设备列表数据
     */
    private void loadData() {
        progressBar.setVisibility(View.VISIBLE);
        viewModel.loadDevices("", 0, 20);
    }

    /**
     * 更新设备列表显示
     * @param pageResponse 设备分页数据
     */
    private void updateDeviceList(PageResponse<DeviceInfoDto> pageResponse) {
        progressBar.setVisibility(View.GONE);
        if (pageResponse != null && pageResponse.getContent() != null) {
            adapter.setDevices(viewModel.getAllDevices());
        }
    }

    /**
     * 设备列表适配器内部类
     */
    private class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {
        private List<DeviceInfoDto> devices = new ArrayList<>();

        /**
         * 设置设备数据列表
         * @param devices 设备列表
         */
        public void setDevices(List<DeviceInfoDto> devices) {
            this.devices = devices != null ? devices : new ArrayList<>();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_device, parent, false);
            return new DeviceViewHolder(view);
        }

        /**
         * 绑定设备数据到ViewHolder
         * @param holder 视图持有者
         * @param position 列表位置
         */
        @Override
        public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
            DeviceInfoDto device = devices.get(position);
            holder.tvImei.setText(device.getImei());

            boolean isOnline = device.getIsOnline() != null && device.getIsOnline();
            holder.tvStatus.setText(isOnline ? "在线" : "离线");
            holder.viewStatusDot.setBackgroundColor(
                    ContextCompat.getColor(requireContext(),
                            isOnline ? R.color.status_online : R.color.status_offline));

            holder.tvBattery.setText(device.getBatteryLevel() != null ? device.getBatteryLevel() + "%" : "--");

            if (device.getPatient() != null) {
                holder.tvPatient.setText(device.getPatient().getName());
            } else {
                holder.tvPatient.setText("未绑定");
            }

            holder.itemView.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putLong("deviceId", device.getId());
                Navigation.findNavController(v)
                        .navigate(R.id.action_deviceList_to_deviceDetail, args);
            });
        }

        @Override
        public int getItemCount() { return devices.size(); }

        class DeviceViewHolder extends RecyclerView.ViewHolder {
            TextView tvImei;
            TextView tvStatus;
            TextView tvBattery;
            TextView tvPatient;
            View viewStatusDot;

            DeviceViewHolder(View itemView) {
                super(itemView);
                tvImei = itemView.findViewById(R.id.tv_device_imei);
                tvStatus = itemView.findViewById(R.id.tv_device_status);
                tvBattery = itemView.findViewById(R.id.tv_device_battery);
                tvPatient = itemView.findViewById(R.id.tv_device_patient);
                viewStatusDot = itemView.findViewById(R.id.view_status_dot);
            }
        }
    }
}
