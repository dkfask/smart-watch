package com.smartwatch.monitor.api;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.smartwatch.monitor.ui.auth.LoginActivity;
import com.smartwatch.monitor.utils.Constants;
import com.smartwatch.monitor.utils.SessionManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * API客户端单例类，负责初始化和提供Retrofit实例
 * 包含Cookie持久化、认证拦截器、日志拦截器等功能
 */
public class ApiClient {

    private static final String TAG = "ApiClient";
    private static ApiClient instance;
    private Retrofit retrofit;
    private SessionManager sessionManager;
    private Context appContext;
    private String currentBaseUrl;

    /**
     * 获取ApiClient单例
     * @return ApiClient实例
     */
    public static synchronized ApiClient getInstance() {
        if (instance == null) {
            instance = new ApiClient();
        }
        return instance;
    }

    /**
     * 初始化ApiClient，创建Retrofit实例
     * @param context 应用上下文
     */
    public void init(Context context) {
        init(context, null);
    }

    /**
     * 初始化ApiClient，创建Retrofit实例
     * @param context 应用上下文
     * @param baseUrl 自定义服务器地址，为null时使用Constants.BASE_URL
     */
    public void init(Context context, String baseUrl) {
        this.appContext = context.getApplicationContext();
        this.sessionManager = new SessionManager(this.appContext);
        this.currentBaseUrl = baseUrl != null ? baseUrl : Constants.BASE_URL;

        // 确保baseUrl以/结尾
        if (!this.currentBaseUrl.endsWith("/")) {
            this.currentBaseUrl += "/";
        }

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(
                message -> Log.d(TAG, message));
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .cookieJar(new PersistentCookieJar(sessionManager))
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    Request.Builder builder = original.newBuilder();
                    return chain.proceed(builder.build());
                })
                // 自动解包ApiResponse信封：提取data字段，让Retrofit直接反序列化为目标类型
                .addInterceptor(new ApiResponseUnwrapInterceptor())
                .addInterceptor(chain -> {
                    Response response = chain.proceed(chain.request());
                    if (response.code() == 401) {
                        sessionManager.clearSession();
                        Intent intent = new Intent(appContext, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        appContext.startActivity(intent);
                    }
                    return response;
                })
                .addInterceptor(loggingInterceptor)
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl(currentBaseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    /**
     * 创建API服务接口实例
     * @param serviceClass 服务接口类
     * @return 服务接口实例
     */
    public <T> T createService(Class<T> serviceClass) {
        return retrofit.create(serviceClass);
    }

    /**
     * 获取SessionManager实例
     * @return SessionManager实例
     */
    public SessionManager getSessionManager() {
        return sessionManager;
    }

    /**
     * 持久化CookieJar实现，将Cookie存储到SharedPreferences
     */
    private static class PersistentCookieJar implements CookieJar {

        private final SessionManager sessionManager;
        private final Set<String> cookies = new HashSet<>();

        /**
         * 构造函数，从SharedPreferences加载已有Cookie
         * @param sessionManager 会话管理器
         */
        PersistentCookieJar(SessionManager sessionManager) {
            this.sessionManager = sessionManager;
            cookies.addAll(sessionManager.getCookies());
        }

        /**
         * 从HTTP响应中保存Cookie
         * @param url 请求URL
         * @param cookies 响应中的Cookie列表
         */
        @Override
        public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
            Set<String> cookieStrings = new HashSet<>();
            for (Cookie cookie : cookies) {
                cookieStrings.add(cookie.toString());
            }
            this.cookies.addAll(cookieStrings);
            sessionManager.saveCookies(this.cookies);
        }

        /**
         * 为HTTP请求加载Cookie
         * @param url 请求URL
         * @return Cookie列表
         */
        @Override
        public List<Cookie> loadForRequest(HttpUrl url) {
            List<Cookie> result = new ArrayList<>();
            for (String cookieStr : cookies) {
                Cookie parsed = Cookie.parse(url, cookieStr);
                if (parsed != null) {
                    result.add(parsed);
                }
            }
            return result;
        }
    }

    /**
     * OkHttp拦截器，自动解包后端ApiResponse信封格式
     *
     * 后端GlobalResponseAdvice将所有API响应封装为：
     * {"code":200,"message":"success","data":{实际数据},"timestamp":123}
     *
     * 但Android端Retrofit服务接口期望直接反序列化实际数据类型（如PageResponse、AlarmStatsDto等），
     * 因此需要在Gson解析前将"data"字段提取出来作为响应体。
     *
     * 对于AuthService等返回ApiResponse&lt;T&gt;的接口，不进行解包（保持信封）。
     */
    static class ApiResponseUnwrapInterceptor implements okhttp3.Interceptor {

        private static final String TAG = "ApiUnwrap";

        @Override
        public Response intercept(Chain chain) throws IOException {
            Response response = chain.proceed(chain.request());
            ResponseBody body = response.body();
            if (body == null) return response;

            String contentType = body.contentType();
            String bodyString = body.string();

            try {
                // 尝试解析为JsonObject检查是否有ApiResponse信封
                com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(bodyString)
                        .getAsJsonObject();

                // 检查是否是ApiResponse信封格式（必须同时包含code和data字段）
                if (json.has("code") && json.has("data")) {
                    int code = json.get("code").getAsInt();
                    com.google.gson.JsonElement dataElement = json.get("data");

                    // 如果code不是200，构造错误响应
                    if (code != 200) {
                        String message = json.has("message") ? json.get("message").getAsString() : "请求失败";
                        Log.w(TAG, "API error: code=" + code + " message=" + message);
                        // 仍然返回原始响应，让Retrofit回调处理
                        ResponseBody newBody = ResponseBody.create(
                                bodyString, contentType != null ? contentType : MediaType.parse("application/json"));
                        return response.newBuilder().body(newBody).build();
                    }

                    // 提取data字段作为新的响应体
                    String dataJson = dataElement.isJsonNull() ? "null" : new com.google.gson.Gson().toJson(dataElement);
                    Log.d(TAG, "Unwrapped ApiResponse, data: " + (dataJson.length() > 200 ? dataJson.substring(0, 200) + "..." : dataJson));

                    ResponseBody newBody = ResponseBody.create(
                            dataJson, contentType != null ? contentType : MediaType.parse("application/json"));
                    return response.newBuilder().body(newBody).build();
                }
            } catch (Exception e) {
                // 不是JSON或不是Object格式（可能是JSON数组），保持原样
                Log.d(TAG, "Not an ApiResponse envelope, passing through: " + e.getMessage());
            }

            // 非信封格式，原样返回
            ResponseBody newBody = ResponseBody.create(
                    bodyString, contentType != null ? contentType : MediaType.parse("application/json"));
            return response.newBuilder().body(newBody).build();
        }
    }
}
