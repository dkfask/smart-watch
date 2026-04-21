package com.smartwatch.monitor.ui.dashboard;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.smartwatch.monitor.R;
import com.smartwatch.monitor.model.AlarmStatsDto;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 仪表盘Fragment，显示系统概览统计信息和最近报警列表
 */
public class DashboardFragment extends Fragment {

    private DashboardViewModel viewModel;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private TextView tvGreeting;
    private TextView tvDate;
    private TextView tvPatientCount;
    private TextView tvDeviceCount;
    private TextView tvDeviceOnline;
    private TextView tvFenceCount;
    private TextView tvPendingAlarms;
    private TextView tvViewAllAlarms;
    private RecyclerView recyclerRecentAlarms;
    private LinearLayout layoutEmptyAlarms;
    private FrameLayout btnNotification;
    private View viewUnreadBadge;
    private ImageView ivUserAvatar;
    private PieChart chartAlarmStats;
    private RecentAlarmAdapter alarmAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        initViewModel();
        loadData();
    }

    private void initViews(View view) {
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        progressBar = view.findViewById(R.id.progress_bar);
        tvGreeting = view.findViewById(R.id.tv_greeting);
        tvDate = view.findViewById(R.id.tv_date);
        tvPatientCount = view.findViewById(R.id.tv_patient_count);
        tvDeviceCount = view.findViewById(R.id.tv_device_count);
        tvDeviceOnline = view.findViewById(R.id.tv_device_online);
        tvFenceCount = view.findViewById(R.id.tv_fence_count);
        tvPendingAlarms = view.findViewById(R.id.tv_pending_alarms);
        tvViewAllAlarms = view.findViewById(R.id.tv_view_all_alarms);
        recyclerRecentAlarms = view.findViewById(R.id.recycler_recent_alarms);
        layoutEmptyAlarms = view.findViewById(R.id.layout_empty_alarms);
        btnNotification = view.findViewById(R.id.btn_notification);
        viewUnreadBadge = view.findViewById(R.id.view_unread_badge);
        ivUserAvatar = view.findViewById(R.id.iv_user_avatar);
        chartAlarmStats = view.findViewById(R.id.chart_alarm_stats);

        recyclerRecentAlarms.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerRecentAlarms.setNestedScrollingEnabled(false);
        alarmAdapter = new RecentAlarmAdapter();
        recyclerRecentAlarms.setAdapter(alarmAdapter);

        // 设置问候语
        updateGreeting();

        // 初始化饼图
        setupPieChart();

        // 下拉刷新
        swipeRefresh.setOnRefreshListener(() -> {
            viewModel.loadDashboardData();
            swipeRefresh.setRefreshing(false);
        });

        // 查看全部报警
        tvViewAllAlarms.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.alarmListFragment);
        });

        // 通知铃铛
        btnNotification.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.alarmListFragment);
        });

        // 头像点击跳转设置
        ivUserAvatar.setOnClickListener(v -> {
            // 跳转设置页
        });
    }

    /**
     * 根据时间设置问候语
     */
    private void updateGreeting() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        String greeting;
        if (hour < 12) {
            greeting = getString(R.string.greeting_morning);
        } else if (hour < 18) {
            greeting = getString(R.string.greeting_afternoon);
        } else {
            greeting = getString(R.string.greeting_evening);
        }

        tvGreeting.setText(greeting);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy年M月d日 EEEE", Locale.CHINESE);
        tvDate.setText(dateFormat.format(calendar.getTime()));
    }

    /**
     * 初始化报警统计饼图
     */
    private void setupPieChart() {
        chartAlarmStats.setUsePercentValues(true);
        chartAlarmStats.getDescription().setEnabled(false);
        chartAlarmStats.setDrawHoleEnabled(true);
        chartAlarmStats.setHoleRadius(55f);
        chartAlarmStats.setTransparentCircleRadius(60f);
        chartAlarmStats.setCenterTextColor(Color.parseColor("#1F2937"));
        chartAlarmStats.setCenterTextSize(14f);
        chartAlarmStats.getLegend().setEnabled(false);
    }

    /**
     * 更新饼图数据
     */
    private void updatePieChart(AlarmStatsDto stats) {
        if (stats == null || chartAlarmStats == null) return;

        List<PieEntry> entries = new ArrayList<>();
        int[] colors = new int[]{
            Color.parseColor("#EF4444"),  // 紧急
            Color.parseColor("#F59E0B"),  // 警告
            Color.parseColor("#3B82F6")   // 信息
        };

        long total = stats.getTotal();
        if (total > 0) {
            // 使用已处理/未处理/误报分类
            if (stats.getPending() > 0) entries.add(new PieEntry(stats.getPending(), "待处理"));
            if (stats.getHandled() > 0) entries.add(new PieEntry(stats.getHandled(), "已处理"));
            if (stats.getFalseAlarm() > 0) entries.add(new PieEntry(stats.getFalseAlarm(), "误报"));
        }

        if (entries.isEmpty()) {
            entries.add(new PieEntry(1, ""));
            colors = new int[]{Color.parseColor("#E5E7EB")};
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setDrawValues(false);

        PieData data = new PieData(dataSet);
        chartAlarmStats.setData(data);
        chartAlarmStats.setCenterText(String.valueOf(total));
        chartAlarmStats.invalidate();
    }

    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        viewModel.getPatientCount().observe(getViewLifecycleOwner(), count -> {
            if (count != null) {
                tvPatientCount.setText(String.valueOf(count));
            }
        });

        viewModel.getDeviceCount().observe(getViewLifecycleOwner(), count -> {
            if (count != null) {
                tvDeviceCount.setText(String.valueOf(count));
            }
        });

        viewModel.getFenceCount().observe(getViewLifecycleOwner(), count -> {
            if (count != null) {
                tvFenceCount.setText(String.valueOf(count));
            }
        });

        viewModel.getAlarmStats().observe(getViewLifecycleOwner(), stats -> {
            if (stats != null) {
                tvPendingAlarms.setText(String.valueOf(stats.getPending()));
                updatePieChart(stats);

                // 显示/隐藏未读徽章
                if (stats.getUnread() > 0) {
                    viewUnreadBadge.setVisibility(View.VISIBLE);
                } else {
                    viewUnreadBadge.setVisibility(View.GONE);
                }
            }
        });

        viewModel.getRecentAlarms().observe(getViewLifecycleOwner(), alarms -> {
            if (alarms != null && !alarms.isEmpty()) {
                alarmAdapter.setAlarms(alarms);
                recyclerRecentAlarms.setVisibility(View.VISIBLE);
                layoutEmptyAlarms.setVisibility(View.GONE);
            } else {
                recyclerRecentAlarms.setVisibility(View.GONE);
                layoutEmptyAlarms.setVisibility(View.VISIBLE);
            }
        });

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

    private void loadData() {
        viewModel.loadDashboardData();
    }

    /**
     * 最近报警列表适配器
     */
    private class RecentAlarmAdapter extends RecyclerView.Adapter<RecentAlarmAdapter.AlarmViewHolder> {

        private List<Map<String, Object>> alarms = new ArrayList<>();

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

            Object alarmType = alarm.get("alarmType");
            holder.tvAlarmType.setText(alarmType != null ? alarmType.toString() : "未知报警");

            Object alarmLevel = alarm.get("alarmLevel");
            String levelText = alarmLevel != null ? alarmLevel.toString() : "";
            holder.tvAlarmLevel.setText(formatAlarmLevel(levelText));

            // 设置报警级别颜色
            int levelColor;
            switch (levelText) {
                case "critical": levelColor = getResources().getColor(R.color.alarm_critical, null); break;
                case "warning": levelColor = getResources().getColor(R.color.alarm_warning, null); break;
                case "info": levelColor = getResources().getColor(R.color.alarm_info, null); break;
                default: levelColor = getResources().getColor(R.color.text_secondary, null); break;
            }
            holder.viewLevelStripe.setBackgroundColor(levelColor);
            holder.tvAlarmLevel.setTextColor(levelColor);

            Object status = alarm.get("status");
            holder.tvAlarmStatus.setText(formatAlarmStatus(status != null ? status.toString() : ""));

            Object triggeredTime = alarm.get("triggeredTime");
            holder.tvAlarmTime.setText(triggeredTime != null ? triggeredTime.toString() : "");

            // 未读指示
            Object unread = alarm.get("unread");
            boolean isUnread = unread != null && Boolean.TRUE.equals(unread);
            holder.viewUnreadBadge.setVisibility(isUnread ? View.VISIBLE : View.GONE);

            holder.itemView.setOnClickListener(v -> {
                Object id = alarm.get("id");
                if (id != null) {
                    try {
                        long alarmId = ((Number) id).longValue();
                        Bundle args = new Bundle();
                        args.putLong("alarmId", alarmId);
                        Navigation.findNavController(v)
                                .navigate(R.id.action_alarmList_to_alarmDetail, args);
                    } catch (Exception ignored) {
                    }
                }
            });
        }

        @Override
        public int getItemCount() { return alarms.size(); }

        private String formatAlarmLevel(String level) {
            if (level == null) return "";
            switch (level) {
                case "critical": return "紧急";
                case "warning": return "警告";
                case "info": return "信息";
                default: return level;
            }
        }

        private String formatAlarmStatus(String status) {
            if (status == null) return "";
            switch (status) {
                case "pending": return "待处理";
                case "handled": return "已处理";
                case "false_alarm": return "误报";
                default: return status;
            }
        }

        class AlarmViewHolder extends RecyclerView.ViewHolder {
            View viewLevelStripe;
            View viewUnreadBadge;
            TextView tvAlarmType;
            TextView tvAlarmLevel;
            TextView tvAlarmStatus;
            TextView tvAlarmTime;

            AlarmViewHolder(View itemView) {
                super(itemView);
                viewLevelStripe = itemView.findViewById(R.id.view_level_stripe);
                viewUnreadBadge = itemView.findViewById(R.id.tv_unread_badge);
                tvAlarmType = itemView.findViewById(R.id.tv_alarm_type);
                tvAlarmLevel = itemView.findViewById(R.id.tv_alarm_level);
                tvAlarmStatus = itemView.findViewById(R.id.tv_alarm_status);
                tvAlarmTime = itemView.findViewById(R.id.tv_alarm_time);
            }
        }
    }
}
