package com.smartwatch.monitor.ui.alarm;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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
import com.google.android.material.tabs.TabLayout;
import com.smartwatch.monitor.R;
import com.smartwatch.monitor.common.UiState;
import com.smartwatch.monitor.model.PageResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AlarmListFragment extends Fragment {

    private AlarmViewModel viewModel;
    private TabLayout tabLayout;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private LinearLayout emptyStateLayout;
    private TextView emptyStateText;
    private AlarmAdapter adapter;
    private String currentStatus = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_alarm_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        initViewModel();
        loadData();
    }

    private void initViews(View view) {
        tabLayout = view.findViewById(R.id.tab_layout);
        recyclerView = view.findViewById(R.id.recycler_alarms);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyStateLayout = view.findViewById(R.id.layout_empty_state);
        emptyStateText = view.findViewById(R.id.tv_empty_message);

        tabLayout.addTab(tabLayout.newTab().setText("All"));
        tabLayout.addTab(tabLayout.newTab().setText("Pending"));
        tabLayout.addTab(tabLayout.newTab().setText("Handled"));
        tabLayout.addTab(tabLayout.newTab().setText("False alarm"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 1:
                        currentStatus = "pending";
                        break;
                    case 2:
                        currentStatus = "handled";
                        break;
                    case 3:
                        currentStatus = "false_alarm";
                        break;
                    default:
                        currentStatus = null;
                        break;
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

        swipeRefresh.setOnRefreshListener(() -> viewModel.refresh());

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (lm != null && lm.findLastCompletelyVisibleItemPosition() == adapter.getItemCount() - 1) {
                    viewModel.loadNextPage();
                }
            }
        });
    }

    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(AlarmViewModel.class);
        viewModel.getAlarms().observe(getViewLifecycleOwner(), this::updateAlarmList);
        viewModel.getAlarmsState().observe(getViewLifecycleOwner(), this::renderState);
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderState(UiState<PageResponse<Map<String, Object>>> state) {
        if (state == null) {
            return;
        }
        swipeRefresh.setRefreshing(false);
        switch (state.getStatus()) {
            case LOADING:
                progressBar.setVisibility(View.VISIBLE);
                emptyStateLayout.setVisibility(View.GONE);
                break;
            case SUCCESS:
                progressBar.setVisibility(View.GONE);
                emptyStateLayout.setVisibility(View.GONE);
                break;
            case EMPTY:
                progressBar.setVisibility(View.GONE);
                emptyStateLayout.setVisibility(View.VISIBLE);
                emptyStateText.setText(state.getMessage() != null ? state.getMessage() : "No alarms");
                break;
            case ERROR:
                progressBar.setVisibility(View.GONE);
                emptyStateLayout.setVisibility(View.VISIBLE);
                emptyStateText.setText(state.getMessage() != null ? state.getMessage() : "Failed to load data");
                break;
        }
    }

    private void loadData() {
        viewModel.loadAlarms(null);
    }

    private void updateAlarmList(PageResponse<Map<String, Object>> pageResponse) {
        if (pageResponse != null && pageResponse.getContent() != null) {
            adapter.setAlarms(pageResponse.getContent());
        } else {
            adapter.setAlarms(new ArrayList<>());
        }
    }

    private static class AlarmAdapter extends RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder> {
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
            String alarmType = String.valueOf(alarm.getOrDefault("alarmType", ""));
            String alarmLevel = String.valueOf(alarm.getOrDefault("alarmLevel", ""));
            String status = String.valueOf(alarm.getOrDefault("status", ""));
            String time = String.valueOf(alarm.getOrDefault("triggeredTime", ""));
            String patientName = String.valueOf(alarm.getOrDefault("patientName", ""));
            boolean isRead = Boolean.TRUE.equals(alarm.get("isRead"));

            holder.tvAlarmType.setText(alarmType);
            holder.tvAlarmLevel.setText(alarmLevel);
            holder.tvAlarmStatus.setText(status);
            holder.tvAlarmTime.setText(time);
            holder.tvPatientName.setText(patientName);

            int levelColor = getLevelColor(holder.itemView, alarmLevel);
            holder.tvAlarmLevel.setTextColor(levelColor);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(levelColor);
            bg.setCornerRadius(10f);
            holder.tvAlarmLevel.setBackground(bg);
            holder.tvUnreadBadge.setVisibility(isRead ? View.GONE : View.VISIBLE);

            holder.itemView.setOnClickListener(v -> {
                Object idObj = alarm.get("id");
                if (idObj instanceof Number) {
                    Bundle args = new Bundle();
                    args.putLong("alarmId", ((Number) idObj).longValue());
                    Navigation.findNavController(holder.itemView).navigate(R.id.action_alarmList_to_alarmDetail, args);
                }
            });
        }

        private int getLevelColor(View view, String level) {
            if ("critical".equals(level)) {
                return ContextCompat.getColor(view.getContext(), R.color.status_critical);
            }
            if ("warning".equals(level)) {
                return ContextCompat.getColor(view.getContext(), R.color.status_warning);
            }
            return ContextCompat.getColor(view.getContext(), R.color.status_info);
        }

        @Override
        public int getItemCount() {
            return alarms.size();
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
