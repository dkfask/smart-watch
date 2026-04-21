package com.smartwatch.monitor.ui.patient;

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
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.tabs.TabLayout;
import com.smartwatch.monitor.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 健康数据展示Fragment，显示病人最新健康数据卡片和趋势图表
 */
public class HealthDataFragment extends Fragment {

    private HealthViewModel viewModel;
    private ProgressBar progressBar;

    private TextView tvHeartRateValue;
    private TextView tvHeartRateTime;
    private TextView tvTemperatureValue;
    private TextView tvTemperatureTime;
    private TextView tvBloodPressureValue;
    private TextView tvBloodPressureTime;
    private TextView tvSpo2Value;
    private TextView tvSpo2Time;

    private TabLayout tabDataType;
    private com.github.mikephil.charting.charts.LineChart chartHealth;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh;

    private long patientId = -1;
    private String currentDataType = "heart_rate";

    private static final String[] DATA_TYPES = {"heart_rate", "temperature", "blood_pressure", "spo2"};
    private static final String[] DATA_LABELS = {"心率", "体温", "血压", "血氧"};
    private static final int[] DATA_COLORS = {0xFFEF4444, 0xFFF59E0B, 0xFF8B5CF6, 0xFF3B82F6};

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
        return inflater.inflate(R.layout.fragment_health_data, container, false);
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
        initTabLayout();
        initChart();
        loadArguments();
        loadData();
    }

    /**
     * 初始化视图组件
     * @param view 根视图
     */
    private void initViews(View view) {
        progressBar = view.findViewById(R.id.progress_bar);
        tvHeartRateValue = view.findViewById(R.id.tv_heart_rate_value);
        tvHeartRateTime = view.findViewById(R.id.tv_heart_rate_time);
        tvTemperatureValue = view.findViewById(R.id.tv_temperature_value);
        tvTemperatureTime = view.findViewById(R.id.tv_temperature_time);
        tvBloodPressureValue = view.findViewById(R.id.tv_blood_pressure_value);
        tvBloodPressureTime = view.findViewById(R.id.tv_blood_pressure_time);
        tvSpo2Value = view.findViewById(R.id.tv_spo2_value);
        tvSpo2Time = view.findViewById(R.id.tv_spo2_time);
        tabDataType = view.findViewById(R.id.tab_data_type);
        chartHealth = view.findViewById(R.id.chart_health);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);

        swipeRefresh.setColorSchemeResources(R.color.primary_blue);
        swipeRefresh.setOnRefreshListener(() -> {
            loadData();
            swipeRefresh.setRefreshing(false);
        });
    }

    /**
     * 初始化ViewModel并观察数据变化
     */
    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(HealthViewModel.class);

        viewModel.getLatestHealthData().observe(getViewLifecycleOwner(), this::updateHealthCards);

        viewModel.getHealthHistory().observe(getViewLifecycleOwner(), this::updateChart);

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
     * 初始化数据类型Tab选择器
     */
    private void initTabLayout() {
        for (String label : DATA_LABELS) {
            tabDataType.addTab(tabDataType.newTab().setText(label));
        }

        tabDataType.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                if (position >= 0 && position < DATA_TYPES.length) {
                    currentDataType = DATA_TYPES[position];
                    loadHistoryData();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    /**
     * 初始化折线图表配置
     */
    private void initChart() {
        chartHealth.setTouchEnabled(true);
        chartHealth.setPinchZoom(true);
        chartHealth.setDragEnabled(true);
        chartHealth.setScaleEnabled(true);
        chartHealth.getDescription().setEnabled(false);
        chartHealth.setDrawGridBackground(false);
        chartHealth.setNoDataText("暂无数据");

        XAxis xAxis = chartHealth.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        YAxis leftAxis = chartHealth.getAxisLeft();
        leftAxis.setDrawGridLines(true);

        chartHealth.getAxisRight().setEnabled(false);
        chartHealth.getLegend().setEnabled(true);
    }

    /**
     * 从导航参数获取patientId
     */
    private void loadArguments() {
        if (getArguments() != null) {
            patientId = getArguments().getLong("patientId", -1);
        }
    }

    /**
     * 加载所有健康数据
     */
    private void loadData() {
        if (patientId > 0) {
            viewModel.loadLatestHealthData(patientId);
            loadHistoryData();
        }
    }

    /**
     * 根据当前选中的数据类型加载历史数据
     */
    private void loadHistoryData() {
        if (patientId > 0) {
            viewModel.loadHealthHistory(patientId, currentDataType, 50);
        }
    }

    /**
     * 更新健康数据卡片显示
     * @param data 最新健康数据Map，包含heart_rate、temperature、blood_pressure、spo2
     */
    @SuppressWarnings("unchecked")
    private void updateHealthCards(Map<String, Object> data) {
        if (data == null) return;

        updateCard(data.get("heart_rate"), tvHeartRateValue, tvHeartRateTime, "bpm");
        updateCard(data.get("temperature"), tvTemperatureValue, tvTemperatureTime, "°C");
        updateCard(data.get("blood_pressure"), tvBloodPressureValue, tvBloodPressureTime, "mmHg");
        updateCard(data.get("spo2"), tvSpo2Value, tvSpo2Time, "%");
    }

    /**
     * 更新单个健康数据卡片的值和时间
     * @param dataObj 数据对象，可能是Map或String
     * @param valueView 值TextView
     * @param timeView 时间TextView
     * @param unit 数据单位
     */
    @SuppressWarnings("unchecked")
    private void updateCard(Object dataObj, TextView valueView, TextView timeView, String unit) {
        if (dataObj instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) dataObj;
            Object value = map.get("value");
            Object time = map.get("recvTime");
            if (value != null) {
                valueView.setText(value + " " + unit);
            }
            if (time != null) {
                timeView.setText(formatTime(time.toString()));
            }
        }
    }

    /**
     * 格式化时间显示
     * @param timeStr 原始时间字符串
     * @return 格式化后的短时间
     */
    private String formatTime(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) return "--";
        try {
            if (timeStr.length() >= 16) {
                return timeStr.substring(5, 16);
            }
            return timeStr;
        } catch (Exception e) {
            return timeStr;
        }
    }

    /**
     * 根据历史数据更新折线图表
     * @param data 健康历史数据Map
     */
    @SuppressWarnings("unchecked")
    private void updateChart(Map<String, Object> data) {
        if (data == null || getContext() == null) return;

        List<Entry> entries = new ArrayList<>();
        List<String> xLabels = new ArrayList<>();

        Object contentObj = data.get("content");
        if (contentObj instanceof List) {
            List<Map<String, Object>> records = (List<Map<String, Object>>) contentObj;
            for (int i = 0; i < records.size(); i++) {
                Map<String, Object> record = records.get(i);
                Object valueObj = record.get("value");
                Object timeObj = record.get("recvTime");

                float val = 0f;
                if (valueObj instanceof Number) {
                    val = ((Number) valueObj).floatValue();
                } else if (valueObj instanceof String) {
                    try {
                        val = Float.parseFloat((String) valueObj);
                    } catch (NumberFormatException ignored) {}
                }
                entries.add(new Entry(i, val));

                String label = "";
                if (timeObj instanceof String) {
                    label = formatTime((String) timeObj);
                }
                xLabels.add(label);
            }
        }

        if (entries.isEmpty()) {
            chartHealth.clear();
            return;
        }

        int colorIndex = getCurrentColorIndex();
        int lineColor = DATA_COLORS[colorIndex];

        LineDataSet dataSet = new LineDataSet(entries, DATA_LABELS[colorIndex]);
        dataSet.setColor(lineColor);
        dataSet.setCircleColor(lineColor);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineData lineData = new LineData(dataSet);
        chartHealth.setData(lineData);

        XAxis xAxis = chartHealth.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xLabels));
        xAxis.setLabelCount(Math.min(xLabels.size(), 8), true);

        chartHealth.invalidate();
    }

    /**
     * 获取当前数据类型对应的颜色索引
     * @return 颜色索引
     */
    private int getCurrentColorIndex() {
        for (int i = 0; i < DATA_TYPES.length; i++) {
            if (DATA_TYPES[i].equals(currentDataType)) {
                return i;
            }
        }
        return 0;
    }
}
