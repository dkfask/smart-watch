package com.smartwatch.monitor.api;

import com.smartwatch.monitor.model.Device;
import com.smartwatch.monitor.model.DeviceInfoDto;
import com.smartwatch.monitor.model.PageResponse;
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
 * 设备服务接口，提供设备CRUD操作及查询API
 */
public interface DeviceService {

    /**
     * 创建设备
     * @param device 设备信息
     * @return 创建结果
     */
    @POST("devices")
    Call<Object> create(@Body Device device);

    /**
     * 更新设备
     * @param id 设备ID
     * @param device 更新的设备信息
     * @return 更新结果
     */
    @PUT("devices/{id}")
    Call<Void> update(@Path("id") long id, @Body Device device);

    /**
     * 根据ID获取设备详情
     * @param id 设备ID
     * @return 设备详情DTO
     */
    @GET("devices/{id}")
    Call<DeviceInfoDto> get(@Path("id") long id);

    /**
     * 根据IMEI获取设备详情
     * @param imei 设备IMEI号
     * @return 设备详情DTO
     */
    @GET("devices/by-imei/{imei}")
    Call<DeviceInfoDto> getByImei(@Path("imei") String imei);

    /**
     * 分页查询设备列表
     * @param page 页码
     * @param size 每页数量
     * @param search 搜索关键词
     * @return 分页设备列表
     */
    @GET("devices")
    Call<PageResponse<DeviceInfoDto>> list(
            @Query("page") Integer page,
            @Query("size") Integer size,
            @Query("search") String search);

    /**
     * 删除设备
     * @param id 设备ID
     * @return 删除结果
     */
    @DELETE("devices/{id}")
    Call<Void> delete(@Path("id") long id);

    /**
     * 获取未关联设备的列表
     * @return 可用设备列表
     */
    @GET("devices/available")
    Call<List<DeviceInfoDto>> getAvailableDevices();
}
