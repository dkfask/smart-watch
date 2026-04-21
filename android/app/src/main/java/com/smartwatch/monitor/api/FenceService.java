package com.smartwatch.monitor.api;

import com.smartwatch.monitor.model.FenceCreateRequest;
import com.smartwatch.monitor.model.FenceDto;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * 围栏服务接口，提供围栏CRUD操作及查询API
 */
public interface FenceService {

    /**
     * 创建围栏
     * @param request 围栏创建请求
     * @return 创建结果
     */
    @POST("fences")
    Call<Object> create(@Body FenceCreateRequest request);

    /**
     * 更新围栏
     * @param id 围栏ID
     * @param request 围栏更新请求
     * @return 更新结果
     */
    @PUT("fences/{id}")
    Call<Void> update(@Path("id") long id, @Body FenceCreateRequest request);

    /**
     * 获取围栏详情
     * @param id 围栏ID
     * @return 围栏详情DTO
     */
    @GET("fences/{id}")
    Call<FenceDto> get(@Path("id") long id);

    /**
     * 获取围栏列表
     * @param limit 每页数量
     * @param offset 偏移量
     * @return 围栏列表
     */
    @GET("fences")
    Call<List<FenceDto>> list(
            @Query("limit") int limit,
            @Query("offset") int offset);

    /**
     * 获取所有围栏
     * @return 所有围栏列表
     */
    @GET("fences/all")
    Call<List<FenceDto>> getAllFences();

    /**
     * 获取活跃围栏
     * @return 活跃围栏列表
     */
    @GET("fences/active")
    Call<List<FenceDto>> getActiveFences();

    /**
     * 按用户获取围栏
     * @param userId 用户ID
     * @return 围栏列表
     */
    @GET("fences/by-user/{userId}")
    Call<List<FenceDto>> listByUser(@Path("userId") long userId);

    /**
     * 按用户获取活跃围栏
     * @param userId 用户ID
     * @return 活跃围栏列表
     */
    @GET("fences/by-user/{userId}/active")
    Call<List<FenceDto>> listActiveByUser(@Path("userId") long userId);

    /**
     * 删除围栏
     * @param id 围栏ID
     * @return 删除结果
     */
    @DELETE("fences/{id}")
    Call<Void> delete(@Path("id") long id);
}
