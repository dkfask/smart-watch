package com.smartwatch.monitor.ui.auth;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.smartwatch.monitor.repository.AuthRepository;
import java.util.Map;

/**
 * 认证ViewModel，处理登录和退出登录逻辑
 */
public class AuthViewModel extends AndroidViewModel {

    private final AuthRepository authRepository;
    private final MutableLiveData<Boolean> loginResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> logoutResult = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    /**
     * 构造函数，初始化认证仓库
     * @param application 应用实例
     */
    public AuthViewModel(@NonNull Application application) {
        super(application);
        authRepository = new AuthRepository();
    }

    /**
     * 执行登录操作
     * @param username 用户名
     * @param password 密码
     */
    public void login(String username, String password) {
        authRepository.login(username, password).observeForever(result -> {
            if (result.isSuccess()) {
                loginResult.setValue(true);
            } else {
                errorMessage.setValue(result.getError() != null ? result.getError().getMessage() : "登录失败");
                loginResult.setValue(false);
            }
        });
    }

    /**
     * 执行退出登录操作
     */
    public void logout() {
        authRepository.logout().observeForever(result -> {
            logoutResult.setValue(true);
        });
    }

    /**
     * 判断用户是否已登录
     * @return 是否已登录
     */
    public boolean isLoggedIn() {
        return authRepository.isLoggedIn();
    }

    public LiveData<Boolean> getLoginResult() { return loginResult; }
    public LiveData<Boolean> getLogoutResult() { return logoutResult; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
}
