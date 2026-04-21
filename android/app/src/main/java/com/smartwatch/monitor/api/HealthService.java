package com.smartwatch.monitor.api;

import com.smartwatch.monitor.model.HealthRecord;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * 健康数据服务接口，提供健康记录查询、统计、创建等API
 */
public interface HealthService {

    /**
     * 获取健康记录列表
     * @param patientId 病人ID
     * @param imei 设备IMEI号
     * @param dataType 数据类型
     * @param limit 每页数量
     * @param offset 偏移量
     * @return 健康记录分页数据
     */
    @GET("health-records")
    Call<Map<String, Object>> getHealthRecords(
            @Query("patientId") Long patientId,
            @Query("imei") String imei,
            @Query("dataType") String dataType,
            @Query("limit") int limit,
            @Query("offset") int offset);

    /**
     * 获取病人最新的各类健康数据
     * @param patientId 病人ID
     * @return 最新健康数据Map
     */
    @GET("health-records/latest")
    Call<Map<String, Object>> getLatestHealthRecords(@Query("patientId") Long patientId);

    /**
     * 获取病人健康数据统计
     * @param patientId 病人ID
     * @param dataType 数据类型
     * @param start 开始时间戳
     * @param end 结束时间戳
     * @return 健康统计数据
     */
    @GET("health-records/stats")
    Call<Map<String, Object>> getHealthStats(
            @Query("patientId") Long patientId,
            @Query("dataType") String dataType,
            @Query("start") Long start,
            @Query("end") Long end);

    /**
     * 创建健康记录
     * @param record 健康记录
     * @return 创建结果
     */
    @POST("health-records")
    Call<Map<String, Object>> createHealthRecord(@Body HealthRecord record);
}
