package com.smartwatch.monitor.ui.alarm;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.tabs.TabLayout;
import com.smartwatch.monitor.R;
import com.smartwatch.monitor.model.PageResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 报警列表Fragment，显示报警列表并支持下拉刷新、分页和状态筛选
 */
public class AlarmListFragment extends Fragment {

    private AlarmViewModel viewModel;
    private TabLayout tabLayout;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private AlarmAdapter adapter;
    private String currentStatus = null;

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
        return inflater.inflate(R.layout.fragment_alarm_list, container, false);
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
        tabLayout = view.findViewById(R.id.tab_layout);
        recyclerView = view.findViewById(R.id.recycler_alarms);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        progressBar = view.findViewById(R.id.progress_bar);

        tabLayout.addTab(tabLayout.newTab().setText("全部"));
        tabLayout.addTab(tabLayout.newTab().setText("待处理"));
        tabLayout.addTab(tabLayout.newTab().setText("已处理"));
        tabLayout.addTab(tabLayout.newTab().setText("误报"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: currentStatus = null; break;
                    case 1: currentStatus = "pending"; break;
                    case 2: currentStatus = "handled"; break;
                    case 3: currentStatus = "false_alarm"; break;
                }
                viewModel.loadAlarms(currentStatus);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                viewModel.loadAlarms(currentStatus);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AlarmAdapter();
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
     * 初始化ViewModel并观察数据
     */
    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(AlarmViewModel.class);
        viewModel.getAlarms().observe(getViewLifecycleOwner(), this::updateAlarmList);
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
     * 加载报警列表数据
     */
    private void loadData() {
        progressBar.setVisibility(View.VISIBLE);
        viewModel.loadAlarms(null);
    }

    /**
     * 更新报警列表显示
     * @param pageResponse 报警分页数据
     */
    private void updateAlarmList(PageResponse<Map<String, Object>> pageResponse) {
        progressBar.setVisibility(View.GONE);
        if (pageResponse != null && pageResponse.getContent() != null) {
            adapter.setAlarms(pageResponse.getContent());
        }
    }

    /**
     * 报警列表适配器内部类
     */
    private static class AlarmAdapter extends RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder> {
        private List<Map<String, Object>> alarms = new ArrayList<>();

        /**
         * 设置报警数据列表
         * @param alarms 报警列表
         */
        public void setAlarms(List<Map<String, Object>> alarms) {
            this.alarms = alarms != null ? alarms : new ArrayList<>();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public AlarmViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_alarm, parent, false);
            return new AlarmViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull AlarmViewHolder holder, int position) {
            Map<String, Object> alarm = alarms.get(position);

            String alarmType = String.valueOf(alarm.getOrDefault("alarmType", ""));
            String alarmLevel = String.valueOf(alarm.getOrDefault("alarmLevel", ""));
            String status = String.valueOf(alarm.getOrDefault("status", ""));
            String time = String.valueOf(alarm.getOrDefault("triggeredTime", ""));
            String patientName = String.valueOf(alarm.getOrDefault("patientName", ""));
            boolean isRead = Boolean.TRUE.equals(alarm.get("isRead"));

            holder.tvAlarmType.setText(getAlarmTypeLabel(alarmType));
            holder.tvAlarmLevel.setText(getAlarmLevelLabel(alarmLevel));
            holder.tvAlarmStatus.setText(getStatusLabel(status));
            holder.tvAlarmTime.setText(time);
            holder.tvPatientName.setText(patientName);

            int levelColor = getLevelColor(alarmLevel);
            holder.tvAlarmLevel.setTextColor(levelColor);
            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setColor(levelColor);
            bg.setCornerRadius(4f);
            holder.tvAlarmLevel.setBackground(bg);

            if (!isRead) {
                holder.tvUnreadBadge.setVisibility(View.VISIBLE);
            } else {
                holder.tvUnreadBadge.setVisibility(View.GONE);
            }

            holder.itemView.setOnClickListener(v -> {
                Object idObj = alarm.get("id");
                if (idObj != null) {
                    long alarmId = ((Number) idObj).longValue();
                    Bundle args = new Bundle();
                    args.putLong("alarmId", alarmId);
                    Navigation.findNavController(holder.itemView)
                            .navigate(R.id.action_alarmList_to_alarmDetail, args);
                }
            });
        }

        @Override
        public int getItemCount() { return alarms.size(); }

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
         * 获取报警级别对应颜色
         * @param level 报警级别
         * @return 颜色值
         */
        private int getLevelColor(String level) {
            switch (level) {
                case "critical": return 0xFFEF4444;
                case "warning": return 0xFFF59E0B;
                case "info": return 0xFF3B82F6;
                default: return 0xFF6B7280;
            }
        }

        static class AlarmViewHolder extends RecyclerView.ViewHolder {
            TextView tvAlarmType;
            TextView tvAlarmLevel;
            TextView tvAlarmStatus;
            TextView tvAlarmTime;
            TextView tvPatientName;
            TextView tvUnreadBadge;

            AlarmViewHolder(View itemView) {
                super(itemView);
                tvAlarmType = itemView.findViewById(R.id.tv_alarm_type);
                tvAlarmLevel = itemView.findViewById(R.id.tv_alarm_level);
                tvAlarmStatus = itemView.findViewById(R.id.tv_alarm_status);
                tvAlarmTime = itemView.findViewById(R.id.tv_alarm_time);
                tvPatientName = itemView.findViewById(R.id.tv_patient_name);
                tvUnreadBadge = itemView.findViewById(R.id.tv_unread_badge);
            }
        }
    }
}
