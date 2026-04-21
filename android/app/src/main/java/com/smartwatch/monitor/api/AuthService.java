package com.smartwatch.monitor.api;

import com.smartwatch.monitor.model.ApiResponse;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * 认证服务接口，提供登录、获取用户信息、退出登录等API
 *
 * 注意：由于ApiClient中的ApiResponseUnwrapInterceptor会自动解包信封格式，
 * 所有接口返回类型应为data字段的实际类型，而非ApiResponse包装类型。
 */
public interface AuthService {

    /**
     * 用户登录
     * 后端返回 ApiResponse&lt;Map&gt;，解包后直接得到data字段（包含user和token）
     * @param body 登录请求体（包含username和password）
     * @return 登录数据（包含user信息）
     */
    @POST("auth/login")
    Call<Map<String, Object>> login(@Body Map<String, String> body);

    /**
     * 获取当前登录用户信息
     * @return 用户信息
     */
    @POST("auth/me")
    Call<Map<String, Object>> getMe();

    /**
     * 退出登录
     * @return 退出登录响应
     */
    @POST("auth/logout")
    Call<Object> logout();
}
