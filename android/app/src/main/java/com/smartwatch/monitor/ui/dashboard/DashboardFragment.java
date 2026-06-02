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

        updateGreeting();
        setupPieChart();

        swipeRefresh.setOnRefreshListener(() -> {
            viewModel.loadDashboardData();
            swipeRefresh.setRefreshing(false);
        });

        tvViewAllAlarms.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.alarmListFragment));
        btnNotification.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.alarmListFragment));
        ivUserAvatar.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.settingsFragment));
    }

    private void updateGreeting() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        if (hour < 12) {
            tvGreeting.setText("早上好");
        } else if (hour < 18) {
            tvGreeting.setText("下午好");
        } else {
            tvGreeting.setText("晚上好");
        }
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy年M月d日 EEEE", Locale.CHINESE);
            tvDate.setText(dateFormat.format(calendar.getTime()));
        } catch (Exception ex) {
            tvDate.setText("");
        }
    }

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

    private void updatePieChart(AlarmStatsDto stats) {
        if (stats == null || chartAlarmStats == null) return;

        List<PieEntry> entries = new ArrayList<>();
        int[] colors = new int[]{
                Color.parseColor("#EF4444"),
                Color.parseColor("#F59E0B"),
                Color.parseColor("#3B82F6")
        };

        long total = stats.getTotal();
        if (total > 0) {
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
        chartAlarmStats.setData(new PieData(dataSet));
        chartAlarmStats.setCenterText(String.valueOf(total));
        chartAlarmStats.invalidate();
    }

    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        viewModel.getPatientCount().observe(getViewLifecycleOwner(), count -> {
            if (count != null) tvPatientCount.setText(String.valueOf(count));
        });
        viewModel.getDeviceCount().observe(getViewLifecycleOwner(), count -> {
            if (count != null) tvDeviceCount.setText(String.valueOf(count));
        });
        viewModel.getFenceCount().observe(getViewLifecycleOwner(), count -> {
            if (count != null) tvFenceCount.setText(String.valueOf(count));
        });
        viewModel.getAlarmStats().observe(getViewLifecycleOwner(), stats -> {
            if (stats != null) {
                tvPendingAlarms.setText(String.valueOf(stats.getPending()));
                updatePieChart(stats);
                viewUnreadBadge.setVisibility(stats.getUnread() > 0 ? View.VISIBLE : View.GONE);
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
            if (loading != null) progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        });
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        });
    }

    private void loadData() {
        viewModel.loadDashboardData();
    }

    private class RecentAlarmAdapter extends RecyclerView.Adapter<RecentAlarmAdapter.AlarmViewHolder> {
        private List<Map<String, Object>> alarms = new ArrayList<>();

        public void setAlarms(List<Map<String, Object>> alarms) {
            this.alarms = alarms != null ? alarms : new ArrayList<>();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public AlarmViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_alarm, parent, false);
            return new AlarmViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull AlarmViewHolder holder, int position) {
            Map<String, Object> alarm = alarms.get(position);
            String type = String.valueOf(alarm.getOrDefault("alarmType", "未知报警"));
            String level = String.valueOf(alarm.getOrDefault("alarmLevel", ""));
            String status = String.valueOf(alarm.getOrDefault("status", ""));
            String time = String.valueOf(alarm.getOrDefault("triggeredTime", ""));
            holder.tvAlarmType.setText(type);
            holder.tvAlarmLevel.setText(level);
            holder.tvAlarmStatus.setText(status);
            holder.tvAlarmTime.setText(time);

            int levelColor;
            if ("critical".equals(level)) {
                levelColor = getResources().getColor(R.color.alarm_critical, null);
            } else if ("warning".equals(level)) {
                levelColor = getResources().getColor(R.color.alarm_warning, null);
            } else {
                levelColor = getResources().getColor(R.color.alarm_info, null);
            }
            holder.viewLevelStripe.setBackgroundColor(levelColor);
            holder.tvAlarmLevel.setTextColor(levelColor);

            boolean isUnread = Boolean.TRUE.equals(alarm.get("unread"));
            holder.viewUnreadBadge.setVisibility(isUnread ? View.VISIBLE : View.GONE);

            holder.itemView.setOnClickListener(v -> {
                Object id = alarm.get("id");
                if (id instanceof Number) {
                    Bundle args = new Bundle();
                    args.putLong("alarmId", ((Number) id).longValue());
                    Navigation.findNavController(v).navigate(R.id.action_alarmList_to_alarmDetail, args);
                }
            });
        }

        @Override
        public int getItemCount() {
            return alarms.size();
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
