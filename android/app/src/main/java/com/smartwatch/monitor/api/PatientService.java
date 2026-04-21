package com.smartwatch.monitor.api;

import com.smartwatch.monitor.model.PageResponse;
import com.smartwatch.monitor.model.Patient;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * 病人服务接口，提供病人CRUD操作API
 */
public interface PatientService {

    /**
     * 创建病人
     * @param patient 病人信息
     * @return 创建的病人信息
     */
    @POST("patients")
    Call<Patient> create(@Body Patient patient);

    /**
     * 获取病人列表（分页）
     * @param search 搜索关键词
     * @param page 页码
     * @param size 每页数量
     * @return 分页病人列表
     */
    @GET("patients")
    Call<PageResponse<Patient>> list(
            @Query("search") String search,
            @Query("page") Integer page,
            @Query("size") Integer size);

    /**
     * 获取病人详情
     * @param id 病人ID
     * @return 病人详情
     */
    @GET("patients/{id}")
    Call<Patient> get(@Path("id") long id);

    /**
     * 更新病人信息
     * @param id 病人ID
     * @param patient 更新的病人信息
     * @return 更新后的病人信息
     */
    @PUT("patients/{id}")
    Call<Patient> update(@Path("id") long id, @Body Patient patient);

    /**
     * 删除病人
     * @param id 病人ID
     * @return 删除结果
     */
    @DELETE("patients/{id}")
    Call<Void> delete(@Path("id") long id);
}
