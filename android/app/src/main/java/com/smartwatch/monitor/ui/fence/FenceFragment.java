package com.smartwatch.monitor.ui.fence;

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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.CircleOptions;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.MarkerOptions;
import com.amap.api.maps2d.model.PolygonOptions;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.smartwatch.monitor.R;
import com.smartwatch.monitor.model.FenceCreateRequest;
import com.smartwatch.monitor.model.FenceDto;
import com.smartwatch.monitor.utils.MapHelper;
import java.util.ArrayList;
import java.util.List;

/**
 * 围栏Fragment，显示地图和围栏列表，支持创建和删除操作
 */
public class FenceFragment extends Fragment {

    private FenceViewModel viewModel;
    private MapView mapView;
    private AMap aMap;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private FloatingActionButton fabAdd;
    private FenceAdapter adapter;
    private List<FenceDto> fenceList = new ArrayList<>();
    private long selectedFenceId = -1;
    private int drawingMode = DRAW_MODE_NONE;

    private static final int DRAW_MODE_NONE = 0;
    private static final int DRAW_MODE_CIRCLE = 1;
    private static final int DRAW_MODE_POLYGON = 2;
    private static final int DRAW_MODE_RECTANGLE = 3;

    private List<LatLng> tempPolygonPoints = new ArrayList<>();
    private LatLng tempCircleCenter = null;

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
        return inflater.inflate(R.layout.fragment_fence, container, false);
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
        loadData();
    }

    /**
     * 初始化视图组件
     * @param view 根视图
     * @param savedInstanceState 保存的实例状态
     */
    private void initViews(View view, Bundle savedInstanceState) {
        mapView = view.findViewById(R.id.map_view);
        mapView.onCreate(savedInstanceState);
        initMap();

        recyclerView = view.findViewById(R.id.recycler_fences);
        progressBar = view.findViewById(R.id.progress_bar);
        fabAdd = view.findViewById(R.id.fab_add_fence);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new FenceAdapter();
        recyclerView.setAdapter(adapter);

        fabAdd.setOnClickListener(v -> showDrawingOptions());
    }

    /**
     * 初始化高德地图
     */
    private void initMap() {
        if (aMap == null) {
            aMap = mapView.getMap();
        }
        MapHelper.setupMap(aMap);
        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(39.9042, 116.4074), 10));

        aMap.setOnMapClickListener(latLng -> {
            if (drawingMode == DRAW_MODE_CIRCLE) {
                tempCircleCenter = latLng;
                showCreateFenceDialog("circle", latLng, 500);
            } else if (drawingMode == DRAW_MODE_POLYGON) {
                tempPolygonPoints.add(latLng);
                aMap.addMarker(new MarkerOptions().position(latLng).title("顶点" + tempPolygonPoints.size()));
                if (tempPolygonPoints.size() >= 3) {
                    showCreateFenceDialog("polygon", null, 0);
                }
            } else if (drawingMode == DRAW_MODE_RECTANGLE) {
                tempPolygonPoints.add(latLng);
                aMap.addMarker(new MarkerOptions().position(latLng).title("角点" + tempPolygonPoints.size()));
                if (tempPolygonPoints.size() >= 2) {
                    LatLng p1 = tempPolygonPoints.get(0);
                    LatLng p2 = tempPolygonPoints.get(1);
                    tempPolygonPoints.clear();
                    tempPolygonPoints.add(p1);
                    tempPolygonPoints.add(new LatLng(p1.latitude, p2.longitude));
                    tempPolygonPoints.add(p2);
                    tempPolygonPoints.add(new LatLng(p2.latitude, p1.longitude));
                    showCreateFenceDialog("rectangle", null, 0);
                }
            }
        });

        aMap.setOnMapLongClickListener(latLng -> {
            drawingMode = DRAW_MODE_NONE;
            tempPolygonPoints.clear();
            tempCircleCenter = null;
            Toast.makeText(getContext(), "已退出绘制模式", Toast.LENGTH_SHORT).show();
        });

        aMap.setOnMarkerClickListener(marker -> {
            if (marker.getObject() instanceof FenceDto) {
                FenceDto fence = (FenceDto) marker.getObject();
                selectFence(fence.getId());
            }
            return false;
        });
    }

    /**
     * 显示绘制工具选项对话框
     */
    private void showDrawingOptions() {
        String[] options = {"圆形围栏", "多边形围栏", "矩形围栏"};
        new AlertDialog.Builder(getContext())
                .setTitle("选择围栏类型")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            drawingMode = DRAW_MODE_CIRCLE;
                            Toast.makeText(getContext(), "点击地图设置圆心", Toast.LENGTH_SHORT).show();
                            break;
                        case 1:
                            drawingMode = DRAW_MODE_POLYGON;
                            tempPolygonPoints.clear();
                            Toast.makeText(getContext(), "点击地图添加顶点（至少3个）", Toast.LENGTH_SHORT).show();
                            break;
                        case 2:
                            drawingMode = DRAW_MODE_RECTANGLE;
                            tempPolygonPoints.clear();
                            Toast.makeText(getContext(), "点击地图设置对角点（2个）", Toast.LENGTH_SHORT).show();
                            break;
                    }
                })
                .show();
    }

    /**
     * 显示创建围栏对话框
     * @param type 围栏类型
     * @param center 圆心坐标（圆形围栏时使用）
     * @param radius 半径（圆形围栏时使用）
     */
    private void showCreateFenceDialog(String type, LatLng center, int radius) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_create_fence, null);
        EditText etName = dialogView.findViewById(R.id.et_fence_name);
        EditText etRadius = dialogView.findViewById(R.id.et_fence_radius);
        TextView tvType = dialogView.findViewById(R.id.tv_fence_type_label);
        TextView tvCoords = dialogView.findViewById(R.id.tv_fence_coords);

        tvType.setText(type.equals("circle") ? "圆形" : type.equals("polygon") ? "多边形" : "矩形");

        if (type.equals("circle") && center != null) {
            etRadius.setText(String.valueOf(radius));
            etRadius.setVisibility(View.VISIBLE);
            tvCoords.setText(String.format("中心: %.6f, %.6f", center.latitude, center.longitude));
        } else {
            etRadius.setVisibility(View.GONE);
            StringBuilder sb = new StringBuilder("顶点: ");
            for (LatLng p : tempPolygonPoints) {
                sb.append(String.format("(%.6f,%.6f) ", p.latitude, p.longitude));
            }
            tvCoords.setText(sb.toString());
        }

        new AlertDialog.Builder(getContext())
                .setTitle("创建围栏")
                .setView(dialogView)
                .setPositiveButton("创建", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(getContext(), "请输入围栏名称", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    FenceCreateRequest request = new FenceCreateRequest();
                    request.setName(name);
                    request.setType(type);
                    request.setStatus("active");

                    if (type.equals("circle") && center != null) {
                        request.setCenterLat(center.latitude);
                        request.setCenterLng(center.longitude);
                        String radiusStr = etRadius.getText().toString().trim();
                        request.setRadius(radiusStr.isEmpty() ? 500 : Integer.parseInt(radiusStr));
                    } else {
                        StringBuilder coordsBuilder = new StringBuilder("[");
                        for (int i = 0; i < tempPolygonPoints.size(); i++) {
                            LatLng p = tempPolygonPoints.get(i);
                            if (i > 0) coordsBuilder.append(",");
                            coordsBuilder.append(String.format("[%.6f,%.6f]", p.longitude, p.latitude));
                        }
                        coordsBuilder.append("]");
                        request.setCoordinates(coordsBuilder.toString());
                        if (!tempPolygonPoints.isEmpty()) {
                            double centerLat = 0, centerLng = 0;
                            for (LatLng p : tempPolygonPoints) {
                                centerLat += p.latitude;
                                centerLng += p.longitude;
                            }
                            request.setCenterLat(centerLat / tempPolygonPoints.size());
                            request.setCenterLng(centerLng / tempPolygonPoints.size());
                        }
                    }

                    viewModel.createFence(request);
                    resetDrawingState();
                })
                .setNegativeButton("取消", (dialog, which) -> resetDrawingState())
                .setOnCancelListener(dialog -> resetDrawingState())
                .show();
    }

    /**
     * 重置绘制状态
     */
    private void resetDrawingState() {
        drawingMode = DRAW_MODE_NONE;
        tempPolygonPoints.clear();
        tempCircleCenter = null;
        aMap.clear();
        drawAllFencesOnMap();
    }

    /**
     * 初始化ViewModel并观察数据
     */
    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(FenceViewModel.class);
        viewModel.getFences().observe(getViewLifecycleOwner(), this::updateFenceList);
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
        viewModel.getOperationResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null && result) {
                Toast.makeText(getContext(), "操作成功", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 加载围栏列表数据
     */
    private void loadData() {
        viewModel.loadFences();
    }

    /**
     * 更新围栏列表显示
     * @param fences 围栏列表
     */
    private void updateFenceList(List<FenceDto> fences) {
        progressBar.setVisibility(View.GONE);
        if (fences != null) {
            fenceList = fences;
            adapter.setFences(fences);
            drawAllFencesOnMap();
        }
    }

    /**
     * 在地图上绘制所有围栏
     */
    private void drawAllFencesOnMap() {
        if (aMap == null) return;
        aMap.clear();
        for (FenceDto fence : fenceList) {
            drawFenceOnMap(fence);
        }
    }

    /**
     * 在地图上绘制单个围栏
     * @param fence 围栏数据
     */
    private void drawFenceOnMap(FenceDto fence) {
        boolean isSelected = fence.getId() != null && fence.getId() == selectedFenceId;
        int color = MapHelper.getFenceColor(fence.getStatus(), isSelected);

        if ("circle".equals(fence.getType()) && fence.getCenterLat() != null && fence.getCenterLng() != null) {
            LatLng center = new LatLng(fence.getCenterLat(), fence.getCenterLng());
            int radius = fence.getRadius() != null ? fence.getRadius() : 500;
            aMap.addCircle(new CircleOptions()
                    .center(center)
                    .radius(radius)
                    .fillColor(color)
                    .strokeColor(MapHelper.getDarkerColor(color))
                    .strokeWidth(2));
            MarkerOptions marker = new MarkerOptions()
                    .position(center)
                    .title(fence.getName());
            aMap.addMarker(marker).setObject(fence);
        } else if (fence.getCoordinates() != null && !fence.getCoordinates().isEmpty()) {
            List<LatLng> points = parseCoordinates(fence.getCoordinates());
            if (points.size() >= 3) {
                aMap.addPolygon(new PolygonOptions()
                        .addAll(points)
                        .fillColor(color)
                        .strokeColor(MapHelper.getDarkerColor(color))
                        .strokeWidth(2));
            }
            if (fence.getCenterLat() != null && fence.getCenterLng() != null) {
                LatLng center = new LatLng(fence.getCenterLat(), fence.getCenterLng());
                MarkerOptions marker = new MarkerOptions()
                        .position(center)
                        .title(fence.getName());
                aMap.addMarker(marker).setObject(fence);
            }
        }
    }

    /**
     * 解析坐标JSON字符串为LatLng列表
     * @param coordinatesJson 坐标JSON字符串
     * @return LatLng列表
     */
    private List<LatLng> parseCoordinates(String coordinatesJson) {
        List<LatLng> points = new ArrayList<>();
        try {
            String json = coordinatesJson.replace("[", "").replace("]", "").trim();
            if (json.isEmpty()) return points;
            String[] pairs = json.split(",");
            for (int i = 0; i < pairs.length - 1; i += 2) {
                double lng = Double.parseDouble(pairs[i].trim());
                double lat = Double.parseDouble(pairs[i + 1].trim());
                points.add(new LatLng(lat, lng));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return points;
    }

    /**
     * 选中围栏，在地图上高亮并移动相机
     * @param fenceId 围栏ID
     */
    private void selectFence(long fenceId) {
        selectedFenceId = fenceId;
        drawAllFencesOnMap();
        for (FenceDto fence : fenceList) {
            if (fence.getId() != null && fence.getId() == fenceId) {
                if (fence.getCenterLat() != null && fence.getCenterLng() != null) {
                    MapHelper.moveCamera(aMap, fence.getCenterLat(), fence.getCenterLng(), 15);
                }
                int position = fenceList.indexOf(fence);
                recyclerView.scrollToPosition(position);
                break;
            }
        }
        adapter.setSelectedFenceId(fenceId);
    }

    /**
     * 处理地图生命周期 - Activity创建时调用
     * @param savedInstanceState 保存的实例状态
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

    /**
     * 围栏列表适配器内部类
     */
    private class FenceAdapter extends RecyclerView.Adapter<FenceAdapter.FenceViewHolder> {
        private List<FenceDto> fences = new ArrayList<>();
        private long selectedId = -1;

        /**
         * 设置围栏数据列表
         * @param fences 围栏列表
         */
        public void setFences(List<FenceDto> fences) {
            this.fences = fences != null ? fences : new ArrayList<>();
            notifyDataSetChanged();
        }

        /**
         * 设置选中的围栏ID
         * @param fenceId 围栏ID
         */
        public void setSelectedFenceId(long fenceId) {
            this.selectedId = fenceId;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public FenceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_fence, parent, false);
            return new FenceViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull FenceViewHolder holder, int position) {
            FenceDto fence = fences.get(position);
            holder.tvName.setText(fence.getName());
            holder.chipType.setText(getTypeLabel(fence.getType()));
            holder.tvStatus.setText(getStatusLabel(fence.getStatus()));

            int statusColor = getStatusColor(fence.getStatus());
            holder.tvStatus.setTextColor(statusColor);
            holder.chipType.setChipBackgroundColor(
                    android.content.res.ColorStateList.valueOf(
                            ContextCompat.getColor(holder.itemView.getContext(),
                                    "circle".equals(fence.getType()) ? R.color.alarm_info : R.color.alarm_warning)));

            if (fence.getCenterLat() != null && fence.getCenterLng() != null) {
                holder.tvCenter.setText(String.format("(%.4f, %.4f)", fence.getCenterLat(), fence.getCenterLng()));
            } else {
                holder.tvCenter.setText("");
            }

            boolean isSelected = fence.getId() != null && fence.getId() == selectedId;
            holder.itemView.setBackgroundColor(
                    ContextCompat.getColor(holder.itemView.getContext(),
                            isSelected ? R.color.primary_blue_light : R.color.surface_color));

            holder.itemView.setOnClickListener(v -> {
                if (fence.getId() != null) {
                    selectFence(fence.getId());
                }
            });

            holder.itemView.setOnLongClickListener(v -> {
                if (fence.getId() != null) {
                    showDeleteConfirmation(fence);
                }
                return true;
            });
        }

        @Override
        public int getItemCount() { return fences.size(); }

        /**
         * 获取围栏类型显示标签
         * @param type 类型字符串
         * @return 中文标签
         */
        private String getTypeLabel(String type) {
            if ("circle".equals(type)) return "圆形";
            if ("polygon".equals(type)) return "多边形";
            if ("rectangle".equals(type)) return "矩形";
            return type != null ? type : "";
        }

        /**
         * 获取围栏状态显示标签
         * @param status 状态字符串
         * @return 中文标签
         */
        private String getStatusLabel(String status) {
            if ("active".equals(status)) return "启用";
            if ("inactive".equals(status)) return "停用";
            return status != null ? status : "";
        }

        /**
         * 获取状态对应的颜色
         * @param status 状态字符串
         * @return 颜色资源ID
         */
        private int getStatusColor(String status) {
            if ("active".equals(status)) {
                return ContextCompat.getColor(requireContext(), R.color.status_active);
            }
            return ContextCompat.getColor(requireContext(), R.color.status_inactive);
        }

        /**
         * 显示删除确认对话框
         * @param fence 要删除的围栏
         */
        private void showDeleteConfirmation(FenceDto fence) {
            new AlertDialog.Builder(getContext())
                    .setTitle("删除围栏")
                    .setMessage("确定要删除围栏\"" + fence.getName() + "\"吗？")
                    .setPositiveButton("删除", (dialog, which) -> {
                        if (fence.getId() != null) {
                            viewModel.deleteFence(fence.getId());
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
        }

        class FenceViewHolder extends RecyclerView.ViewHolder {
            TextView tvName;
            Chip chipType;
            TextView tvStatus;
            TextView tvCenter;

            FenceViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_fence_name);
                chipType = itemView.findViewById(R.id.chip_fence_type);
                tvStatus = itemView.findViewById(R.id.tv_fence_status);
                tvCenter = itemView.findViewById(R.id.tv_fence_center);
            }
        }
    }
}
