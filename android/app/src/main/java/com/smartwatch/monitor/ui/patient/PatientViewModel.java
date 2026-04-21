package com.smartwatch.monitor.ui.patient;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.smartwatch.monitor.model.PageResponse;
import com.smartwatch.monitor.model.Patient;
import com.smartwatch.monitor.repository.PatientRepository;
import com.smartwatch.monitor.utils.Constants;
import java.util.ArrayList;
import java.util.List;

/**
 * 病人ViewModel，处理病人列表、详情、增删改等数据逻辑
 */
public class PatientViewModel extends AndroidViewModel {

    private final PatientRepository patientRepository;
    private final MutableLiveData<PageResponse<Patient>> patients = new MutableLiveData<>();
    private final MutableLiveData<Patient> patientDetail = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> operationSuccess = new MutableLiveData<>();

    private int currentPage = 0;
    private String currentSearch = "";
    private List<Patient> allPatients = new ArrayList<>();
    private boolean hasMore = true;

    /**
     * 构造函数，初始化病人仓库
     * @param application 应用实例
     */
    public PatientViewModel(@NonNull Application application) {
        super(application);
        patientRepository = new PatientRepository();
    }

    /**
     * 加载病人列表，支持搜索和分页
     * @param search 搜索关键词
     * @param page 页码
     * @param size 每页数量
     */
    public void loadPatients(String search, int page, int size) {
        if (isLoading.getValue() != null && isLoading.getValue()) return;

        isLoading.setValue(true);
        errorMessage.setValue(null);

        if (page == 0) {
            allPatients.clear();
            hasMore = true;
        }
        currentPage = page;
        currentSearch = search;

        patientRepository.getPatients(search, page).observeForever(result -> {
            isLoading.setValue(false);
            if (result != null) {
                if (page == 0) {
                    allPatients.clear();
                }
                if (result.getContent() != null) {
                    allPatients.addAll(result.getContent());
                }
                patients.setValue(result);
                hasMore = result.getContent() != null && result.getContent().size() >= Constants.DEFAULT_PAGE_SIZE;
            } else {
                errorMessage.setValue("加载病人列表失败");
            }
        });
    }

    /**
     * 加载病人详情
     * @param id 病人ID
     */
    public void loadPatientDetail(long id) {
        isLoading.setValue(true);
        patientRepository.getPatient(id).observeForever(result -> {
            isLoading.setValue(false);
            if (result != null) {
                patientDetail.setValue(result);
            } else {
                errorMessage.setValue("加载病人详情失败");
            }
        });
    }

    /**
     * 创建新病人
     * @param patient 病人信息
     */
    public void createPatient(Patient patient) {
        isLoading.setValue(true);
        patientRepository.createPatient(patient).observeForever(result -> {
            isLoading.setValue(false);
            if (result != null) {
                operationSuccess.setValue(true);
            } else {
                errorMessage.setValue("创建病人失败");
            }
        });
    }

    /**
     * 更新病人信息
     * @param id 病人ID
     * @param patient 更新的病人信息
     */
    public void updatePatient(long id, Patient patient) {
        isLoading.setValue(true);
        patientRepository.updatePatient(id, patient).observeForever(result -> {
            isLoading.setValue(false);
            if (result != null) {
                operationSuccess.setValue(true);
                patientDetail.setValue(result);
            } else {
                errorMessage.setValue("更新病人信息失败");
            }
        });
    }

    /**
     * 删除病人
     * @param id 病人ID
     */
    public void deletePatient(long id) {
        isLoading.setValue(true);
        patientRepository.deletePatient(id).observeForever(result -> {
            isLoading.setValue(false);
            if (result != null && result) {
                operationSuccess.setValue(true);
            } else {
                errorMessage.setValue("删除病人失败");
            }
        });
    }

    /**
     * 刷新当前列表（重置到第一页）
     */
    public void refresh() {
        loadPatients(currentSearch, 0, Constants.DEFAULT_PAGE_SIZE);
    }

    /**
     * 加载下一页数据
     */
    public void loadNextPage() {
        if (hasMore) {
            loadPatients(currentSearch, currentPage + 1, Constants.DEFAULT_PAGE_SIZE);
        }
    }

    /**
     * 获取病人列表LiveData
     * @return 病人分页数据
     */
    public LiveData<PageResponse<Patient>> getPatients() { return patients; }

    /**
     * 获取病人详情LiveData
     * @return 病人详情数据
     */
    public LiveData<Patient> getPatientDetail() { return patientDetail; }

    /**
     * 获取加载状态LiveData
     * @return 是否正在加载
     */
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    /**
     * 获取错误信息LiveData
     * @return 错误信息字符串
     */
    public LiveData<String> getErrorMessage() { return errorMessage; }

    /**
     * 获取操作结果LiveData
     * @return 操作是否成功
     */
    public LiveData<Boolean> getOperationSuccess() { return operationSuccess; }

    /**
     * 是否还有更多数据可加载
     * @return 是否有更多数据
     */
    public boolean hasMore() { return hasMore; }

    /**
     * 获取当前页码
     * @return 当前页码
     */
    public int getCurrentPage() { return currentPage; }

    /**
     * 获取所有已加载的病人列表
     * @return 病人列表
     */
    public List<Patient> getAllPatients() { return allPatients; }
}
