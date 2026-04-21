package com.smartwatch.monitor.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.smartwatch.monitor.api.ApiClient;
import com.smartwatch.monitor.api.PatientService;
import com.smartwatch.monitor.data.AppDatabase;
import com.smartwatch.monitor.data.PatientDao;
import com.smartwatch.monitor.model.PageResponse;
import com.smartwatch.monitor.model.Patient;
import com.smartwatch.monitor.utils.Constants;
import java.util.List;
import java.util.concurrent.Executors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 病人数据仓库，负责处理病人相关数据操作，支持Room离线缓存
 */
public class PatientRepository {

    private final PatientService patientService;
    private final PatientDao patientDao;

    /**
     * 构造函数，初始化病人服务和Room DAO
     */
    public PatientRepository() {
        patientService = ApiClient.getInstance().createService(PatientService.class);
        patientDao = null;
    }

    /**
     * 构造函数，初始化病人服务和Room DAO（带数据库支持）
     * @param application 应用实例
     */
    public PatientRepository(Application application) {
        patientService = ApiClient.getInstance().createService(PatientService.class);
        patientDao = AppDatabase.getInstance(application).patientDao();
    }

    /**
     * 获取病人列表，优先从网络获取，失败时返回缓存数据
     * @param search 搜索关键词
     * @param page 页码
     * @return 病人分页列表LiveData
     */
    public LiveData<PageResponse<Patient>> getPatients(String search, int page) {
        MutableLiveData<PageResponse<Patient>> result = new MutableLiveData<>();
        patientService.list(search, page, Constants.DEFAULT_PAGE_SIZE).enqueue(new Callback<PageResponse<Patient>>() {
            @Override
            public void onResponse(Call<PageResponse<Patient>> call, Response<PageResponse<Patient>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.setValue(response.body());
                    cachePatients(response.body().getContent());
                } else {
                    loadCachedPatientsAsPage(result, search, page);
                }
            }

            @Override
            public void onFailure(Call<PageResponse<Patient>> call, Throwable t) {
                loadCachedPatientsAsPage(result, search, page);
            }
        });
        return result;
    }

    /**
     * 获取病人详情，优先从网络获取，失败时返回缓存数据
     * @param id 病人ID
     * @return 病人详情LiveData
     */
    public LiveData<Patient> getPatient(long id) {
        MutableLiveData<Patient> result = new MutableLiveData<>();
        patientService.get(id).enqueue(new Callback<Patient>() {
            @Override
            public void onResponse(Call<Patient> call, Response<Patient> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.setValue(response.body());
                    cachePatient(response.body());
                } else {
                    loadCachedPatient(result, id);
                }
            }

            @Override
            public void onFailure(Call<Patient> call, Throwable t) {
                loadCachedPatient(result, id);
            }
        });
        return result;
    }

    /**
     * 创建病人
     * @param patient 病人信息
     * @return 创建结果LiveData
     */
    public LiveData<Patient> createPatient(Patient patient) {
        MutableLiveData<Patient> result = new MutableLiveData<>();
        patientService.create(patient).enqueue(new Callback<Patient>() {
            @Override
            public void onResponse(Call<Patient> call, Response<Patient> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.setValue(response.body());
                    cachePatient(response.body());
                }
            }

            @Override
            public void onFailure(Call<Patient> call, Throwable t) {
                result.setValue(null);
            }
        });
        return result;
    }

    /**
     * 更新病人信息
     * @param id 病人ID
     * @param patient 更新的病人信息
     * @return 更新结果LiveData
     */
    public LiveData<Patient> updatePatient(long id, Patient patient) {
        MutableLiveData<Patient> result = new MutableLiveData<>();
        patientService.update(id, patient).enqueue(new Callback<Patient>() {
            @Override
            public void onResponse(Call<Patient> call, Response<Patient> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.setValue(response.body());
                    cachePatient(response.body());
                }
            }

            @Override
            public void onFailure(Call<Patient> call, Throwable t) {
                result.setValue(null);
            }
        });
        return result;
    }

    /**
     * 删除病人
     * @param id 病人ID
     * @return 删除结果LiveData
     */
    public LiveData<Boolean> deletePatient(long id) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        patientService.delete(id).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                result.setValue(response.isSuccessful());
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                result.setValue(false);
            }
        });
        return result;
    }

    /**
     * 将病人列表缓存到Room数据库（后台线程执行）
     * @param patients 病人列表
     */
    private void cachePatients(List<Patient> patients) {
        if (patientDao != null && patients != null) {
            Executors.newSingleThreadExecutor().execute(() -> patientDao.insertAll(patients));
        }
    }

    /**
     * 将单个病人缓存到Room数据库（后台线程执行）
     * @param patient 病人对象
     */
    private void cachePatient(Patient patient) {
        if (patientDao != null && patient != null) {
            Executors.newSingleThreadExecutor().execute(() -> patientDao.insert(patient));
        }
    }

    /**
     * 从Room缓存加载病人列表作为分页数据（离线回退）
     * @param result 结果LiveData
     * @param search 搜索关键词
     * @param page 页码
     */
    private void loadCachedPatientsAsPage(MutableLiveData<PageResponse<Patient>> result, String search, int page) {
        if (patientDao != null) {
            patientDao.getAllPatients().observeForever(patients -> {
                if (patients != null && !patients.isEmpty()) {
                    PageResponse<Patient> pageResponse = new PageResponse<>();
                    pageResponse.setContent(patients);
                    result.setValue(pageResponse);
                } else {
                    result.setValue(null);
                }
            });
        } else {
            result.setValue(null);
        }
    }

    /**
     * 从Room缓存加载单个病人详情（离线回退）
     * @param result 结果LiveData
     * @param id 病人ID
     */
    private void loadCachedPatient(MutableLiveData<Patient> result, long id) {
        if (patientDao != null) {
            patientDao.getPatientById(id).observeForever(patient -> result.setValue(patient));
        } else {
            result.setValue(null);
        }
    }
}
