package com.smartwatch.monitor.api;

import java.util.Map;
import java.util.Set;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * 下行指令服务接口，提供向设备发送各种指令的API
 */
public interface DownlinkService {

    /**
     * 获取在线设备IMEI列表
     * @return 在线IMEI集合
     */
    @GET("downlink/online")
    Call<Set<String>> online();

    /**
     * 发送BP00时间同步指令
     * @param imei 设备IMEI号
     * @param timezone 时区偏移小时数
     * @return 发送结果
     */
    @POST("downlink/bp00")
    Call<Map<String, Object>> sendBp00(@Query("imei") String imei, @Query("timezone") Integer timezone);

    /**
     * 发送BP12 SOS号码指令
     * @param body 请求体（包含imei、seq、sos列表）
     * @return 发送结果
     */
    @POST("downlink/bp12")
    Call<Map<String, Object>> sendBp12(@Body Map<String, Object> body);

    /**
     * 发送自定义指令
     * @param body 请求体（包含imei、payload）
     * @return 发送结果
     */
    @POST("downlink/custom")
    Call<Map<String, Object>> sendCustom(@Body Map<String, String> body);

    /**
     * 发送BP15 GPS定位间隔指令
     * @param imei 设备IMEI号
     * @param interval 定位间隔（秒）
     * @return 发送结果
     */
    @POST("downlink/bp15")
    Call<Map<String, Object>> sendBp15(@Query("imei") String imei, @Query("interval") int interval);

    /**
     * 发送BP16 立即定位指令
     * @param imei 设备IMEI号
     * @return 发送结果
     */
    @POST("downlink/bp16")
    Call<Map<String, Object>> sendBp16(@Query("imei") String imei);

    /**
     * 发送BP17 恢复出厂设置指令
     * @param imei 设备IMEI号
     * @return 发送结果
     */
    @POST("downlink/bp17")
    Call<Map<String, Object>> sendBp17(@Query("imei") String imei);

    /**
     * 发送BP18 重启设备指令
     * @param imei 设备IMEI号
     * @return 发送结果
     */
    @POST("downlink/bp18")
    Call<Map<String, Object>> sendBp18(@Query("imei") String imei);

    /**
     * 发送BP31 关机指令
     * @param imei 设备IMEI号
     * @return 发送结果
     */
    @POST("downlink/bp31")
    Call<Map<String, Object>> sendBp31(@Query("imei") String imei);

    /**
     * 发送BP86 健康监测间隔指令
     * @param imei 设备IMEI号
     * @param onOff 开关（0=关，1=开）
     * @param minutes 间隔分钟数
     * @return 发送结果
     */
    @POST("downlink/bp86")
    Call<Map<String, Object>> sendBp86(
            @Query("imei") String imei,
            @Query("onOff") int onOff,
            @Query("minutes") int minutes);

    /**
     * 发送BP88 寻找设备指令
     * @param imei 设备IMEI号
     * @return 发送结果
     */
    @POST("downlink/bp88")
    Call<Map<String, Object>> sendBp88(@Query("imei") String imei);

    /**
     * 发送BPXL 测心率指令
     * @param imei 设备IMEI号
     * @return 发送结果
     */
    @POST("downlink/bpxl")
    Call<Map<String, Object>> sendBpxl(@Query("imei") String imei);

    /**
     * 发送BPXY 测血压指令
     * @param imei 设备IMEI号
     * @return 发送结果
     */
    @POST("downlink/bpxy")
    Call<Map<String, Object>> sendBpxy(@Query("imei") String imei);

    /**
     * 发送BPXZ 测血氧指令
     * @param imei 设备IMEI号
     * @return 发送结果
     */
    @POST("downlink/bpxz")
    Call<Map<String, Object>> sendBpxz(@Query("imei") String imei);

    /**
     * 发送BPXX 测体温指令
     * @param imei 设备IMEI号
     * @return 发送结果
     */
    @POST("downlink/bpxx")
    Call<Map<String, Object>> sendBpxx(@Query("imei") String imei);
}
