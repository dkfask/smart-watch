package com.smartwatch.monitor.api;

import com.smartwatch.monitor.model.DeviceLocation;
import com.smartwatch.monitor.model.PageResponse;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * 位置服务接口，提供设备位置上报、查询、搜索等API
 */
public interface LocationService {

    /**
     * 上报设备位置
     * @param body 位置上报请求体
     * @return 上报结果
     */
    @POST("locations/report")
    Call<Object> report(@Body Object body);

    /**
     * 获取设备最近位置记录（分页）
     * @param deviceId 设备ID
     * @param limit 每页数量
     * @param offset 偏移量
     * @return 分页位置记录
     */
    @GET("locations/device/{deviceId}")
    Call<PageResponse<DeviceLocation>> recent(
            @Path("deviceId") long deviceId,
            @Query("limit") int limit,
            @Query("offset") int offset);

    /**
     * 获取设备历史位置记录
     * @param deviceId 设备ID
     * @param start 开始时间
     * @param end 结束时间
     * @param limit 每页数量
     * @param offset 偏移量
     * @return 分页历史位置记录
     */
    @GET("locations/device/{deviceId}/history")
    Call<PageResponse<DeviceLocation>> history(
            @Path("deviceId") long deviceId,
            @Query("start") String start,
            @Query("end") String end,
            @Query("limit") int limit,
            @Query("offset") int offset);

    /**
     * 获取设备最新位置
     * @param deviceId 设备ID
     * @return 最新位置信息
     */
    @GET("locations/device/{deviceId}/latest")
    Call<DeviceLocation> getLatestLocation(@Path("deviceId") long deviceId);

    /**
     * 获取设备最新位置（含地址）
     * @param deviceId 设备ID
     * @return 含地址的最新位置信息
     */
    @GET("locations/device/{deviceId}/latest-with-address")
    Call<Object> getLatestLocationWithAddress(@Path("deviceId") long deviceId);

    /**
     * 根据地址搜索位置（地理编码）
     * @param address 地址字符串
     * @return 位置信息
     */
    @GET("locations/search")
    Call<Map<String, Object>> searchLocation(@Query("address") String address);

    /**
     * 根据关键词搜索POI
     * @param keyword 关键词
     * @param city 城市
     * @param pageSize 每页数量
     * @param page 页码
     * @return POI列表
     */
    @GET("locations/search/poi")
    Call<Object> searchPoi(
            @Query("keyword") String keyword,
            @Query("city") String city,
            @Query("pageSize") Integer pageSize,
            @Query("page") Integer page);
}
