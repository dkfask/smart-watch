package com.smartwatch.monitor.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.smartwatch.monitor.api.ApiClient;
import com.smartwatch.monitor.api.AuthService;
import com.smartwatch.monitor.utils.SessionManager;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 认证数据仓库，负责处理登录、退出等认证相关数据操作
 */
public class AuthRepository {

    private final AuthService authService;
    private final SessionManager sessionManager;

    /**
     * 构造函数，初始化认证服务和会话管理器
     */
    public AuthRepository() {
        authService = ApiClient.getInstance().createService(AuthService.class);
        sessionManager = ApiClient.getInstance().getSessionManager();
    }

    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码
     * @return 登录结果LiveData
     */
    public LiveData<Result<Map<String, Object>>> login(String username, String password) {
        MutableLiveData<Result<Map<String, Object>>> result = new MutableLiveData<>();
        Map<String, String> body = new HashMap<>();
        body.put("username", username);
        body.put("password", password);

        authService.login(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = response.body();
                    // 从响应中提取用户信息
                    String role = "";
                    boolean mustChangePassword = false;
                    if (data.containsKey("user") && data.get("user") instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> user = (Map<String, Object>) data.get("user");
                        if (user.containsKey("role")) {
                            role = String.valueOf(user.get("role"));
                        }
                        if (user.containsKey("mustChangePassword")) {
                            mustChangePassword = Boolean.TRUE.equals(user.get("mustChangePassword"));
                        }
                    }
                    sessionManager.saveUserInfo(username, role, mustChangePassword);
                    result.setValue(Result.success(data));
                } else {
                    result.setValue(Result.failure(new Exception("登录失败")));
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                result.setValue(Result.failure(new Exception(t.getMessage(), t)));
            }
        });
        return result;
    }

    /**
     * 退出登录
     * @return 退出结果LiveData
     */
    public LiveData<Result<Boolean>> logout() {
        MutableLiveData<Result<Boolean>> result = new MutableLiveData<>();
        authService.logout().enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                sessionManager.clearSession();
                result.setValue(Result.success(true));
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                sessionManager.clearSession();
                result.setValue(Result.success(true));
            }
        });
        return result;
    }

    /**
     * 判断用户是否已登录
     * @return 是否已登录
     */
    public boolean isLoggedIn() {
        return sessionManager.isLoggedIn();
    }

    /**
     * 结果封装类
     * @param <T> 数据类型
     */
    public static class Result<T> {
        private final T data;
        private final Exception error;
        private final boolean success;

        private Result(T data, Exception error, boolean success) {
            this.data = data;
            this.error = error;
            this.success = success;
        }

        public static <T> Result<T> success(T data) { return new Result<>(data, null, true); }
        public static <T> Result<T> failure(Exception error) { return new Result<>(null, error, false); }

        public T getData() { return data; }
        public Exception getError() { return error; }
        public boolean isSuccess() { return success; }
    }
}
