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
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.search.SearchView;
import com.smartwatch.monitor.R;
import com.smartwatch.monitor.model.PageResponse;
import com.smartwatch.monitor.model.Patient;
import java.util.ArrayList;
import java.util.List;

/**
 * 病人列表Fragment，显示病人列表并支持搜索、下拉刷新和分页加载
 */
public class PatientListFragment extends Fragment {

    private PatientViewModel viewModel;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private FloatingActionButton fabAdd;
    private PatientAdapter adapter;
    private SearchView searchView;

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
        return inflater.inflate(R.layout.fragment_patient_list, container, false);
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
        recyclerView = view.findViewById(R.id.recycler_patients);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        progressBar = view.findViewById(R.id.progress_bar);
        fabAdd = view.findViewById(R.id.fab_add_patient);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new PatientAdapter();
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

        fabAdd.setOnClickListener(v -> {
            Toast.makeText(getContext(), "添加病人功能开发中", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * 初始化ViewModel并观察数据变化
     */
    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(PatientViewModel.class);

        viewModel.getPatients().observe(getViewLifecycleOwner(), this::updatePatientList);

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
     * 加载病人列表数据
     */
    private void loadData() {
        progressBar.setVisibility(View.VISIBLE);
        viewModel.loadPatients("", 0, 20);
    }

    /**
     * 更新病人列表显示
     * @param pageResponse 病人分页数据
     */
    private void updatePatientList(PageResponse<Patient> pageResponse) {
        progressBar.setVisibility(View.GONE);
        if (pageResponse != null && pageResponse.getContent() != null) {
            adapter.setPatients(viewModel.getAllPatients());
        }
    }

    /**
     * 病人列表适配器内部类
     */
    private class PatientAdapter extends RecyclerView.Adapter<PatientAdapter.PatientViewHolder> {
        private List<Patient> patients = new ArrayList<>();

        /**
         * 设置病人数据列表
         * @param patients 病人列表
         */
        public void setPatients(List<Patient> patients) {
            this.patients = patients != null ? patients : new ArrayList<>();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public PatientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_patient, parent, false);
            return new PatientViewHolder(view);
        }

        /**
         * 绑定病人数据到ViewHolder
         * @param holder 视图持有者
         * @param position 列表位置
         */
        @Override
        public void onBindViewHolder(@NonNull PatientViewHolder holder, int position) {
            Patient patient = patients.get(position);
            holder.tvName.setText(patient.getName());

            String wardBed = "";
            if (patient.getWard() != null) {
                wardBed = patient.getWard();
            }
            if (patient.getBed() != null) {
                wardBed = wardBed.isEmpty() ? patient.getBed() : wardBed + " / " + patient.getBed();
            }
            holder.tvWardBed.setText(wardBed.isEmpty() ? "未分配" : wardBed);

            String statusText = formatPatientStatus(patient.getStatus());
            holder.tvStatus.setText(statusText);

            holder.itemView.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putLong("patientId", patient.getId());
                Navigation.findNavController(v)
                        .navigate(R.id.action_patientList_to_patientDetail, args);
            });
        }

        @Override
        public int getItemCount() { return patients.size(); }

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

        class PatientViewHolder extends RecyclerView.ViewHolder {
            TextView tvName;
            TextView tvWardBed;
            TextView tvStatus;

            PatientViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_patient_name);
                tvWardBed = itemView.findViewById(R.id.tv_patient_ward);
                tvStatus = itemView.findViewById(R.id.tv_patient_status);
            }
        }
    }
}
