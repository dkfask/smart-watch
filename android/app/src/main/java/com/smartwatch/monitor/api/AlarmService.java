package com.smartwatch.monitor.api;

import com.smartwatch.monitor.model.Alarm;
import com.smartwatch.monitor.model.AlarmStatsDto;
import com.smartwatch.monitor.model.PageResponse;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * 报警服务接口，提供报警CRUD、标记已读、处理、统计等API
 */
public interface AlarmService {

    /**
     * 创建报警
     * @param alarm 报警信息
     * @return 创建的报警信息
     */
    @POST("alarms")
    Call<Alarm> create(@Body Alarm alarm);

    /**
     * 获取报警详情
     * @param id 报警ID
     * @return 报警详情
     */
    @GET("alarms/{id}")
    Call<Map<String, Object>> get(@Path("id") long id);

    /**
     * 获取报警列表（支持多条件过滤）
     * @param page 页码
     * @param size 每页数量
     * @param deviceId 设备ID
     * @param patientId 病人ID
     * @param status 报警状态
     * @return 分页报警列表
     */
    @GET("alarms")
    Call<PageResponse<Map<String, Object>>> list(
            @Query("page") int page,
            @Query("size") int size,
            @Query("deviceId") Long deviceId,
            @Query("patientId") Long patientId,
            @Query("status") String status);

    /**
     * 按设备ID获取报警列表
     * @param deviceId 设备ID
     * @param page 页码
     * @param size 每页数量
     * @param status 报警状态
     * @return 分页报警列表
     */
    @GET("alarms/device/{deviceId}")
    Call<PageResponse<Map<String, Object>>> byDevice(
            @Path("deviceId") long deviceId,
            @Query("page") int page,
            @Query("size") int size,
            @Query("status") String status);

    /**
     * 按病人ID获取报警列表
     * @param patientId 病人ID
     * @param page 页码
     * @param size 每页数量
     * @param status 报警状态
     * @return 分页报警列表
     */
    @GET("alarms/patient/{patientId}")
    Call<PageResponse<Map<String, Object>>> byPatient(
            @Path("patientId") long patientId,
            @Query("page") int page,
            @Query("size") int size,
            @Query("status") String status);

    /**
     * 标记报警为已读
     * @param id 报警ID
     * @param read 是否已读
     * @return 操作结果
     */
    @PUT("alarms/{id}/read")
    Call<Void> markRead(@Path("id") long id, @Query("read") boolean read);

    /**
     * 批量标记设备的报警为已读
     * @param deviceId 设备ID
     * @param read 是否已读
     * @return 操作结果
     */
    @PUT("alarms/device/{deviceId}/read")
    Call<Void> markDeviceAlarmsRead(@Path("deviceId") long deviceId, @Query("read") boolean read);

    /**
     * 批量标记病人的报警为已读
     * @param patientId 病人ID
     * @param read 是否已读
     * @return 操作结果
     */
    @PUT("alarms/patient/{patientId}/read")
    Call<Void> markPatientAlarmsRead(@Path("patientId") long patientId, @Query("read") boolean read);

    /**
     * 处理报警
     * @param id 报警ID
     * @param body 处理请求体（包含status、result、remark）
     * @return 操作结果
     */
    @PUT("alarms/{id}/handle")
    Call<Void> handle(@Path("id") long id, @Body Map<String, String> body);

    /**
     * 获取报警统计数据
     * @return 报警统计DTO
     */
    @GET("alarms/stats")
    Call<AlarmStatsDto> stats();

    /**
     * 获取未读报警数量
     * @param deviceId 设备ID（可选）
     * @param patientId 病人ID（可选）
     * @return 未读数量
     */
    @GET("alarms/unread-count")
    Call<Map<String, Long>> unreadCount(
            @Query("deviceId") Long deviceId,
            @Query("patientId") Long patientId);

    /**
     * 获取最近的报警记录
     * @param limit 数量限制
     * @return 最近的报警列表
     */
    @GET("alarms/recent")
    Call<Object> recent(@Query("limit") int limit);
}
