package com.smartwatch.monitor.utils;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 会话管理器，基于SharedPreferences实现
 * 负责存储和读取Cookie、用户信息等会话数据
 */
public class SessionManager {

    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    /**
     * 构造函数，初始化SharedPreferences
     * @param context 应用上下文
     */
    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    /**
     * 保存Cookie集合到本地
     * @param cookies Cookie字符串集合
     */
    public void saveCookies(Set<String> cookies) {
        editor.putStringSet(Constants.KEY_COOKIES, cookies);
        editor.apply();
    }

    /**
     * 获取本地存储的Cookie集合
     * @return Cookie字符串集合
     */
    public Set<String> getCookies() {
        return prefs.getStringSet(Constants.KEY_COOKIES, new HashSet<>());
    }

    /**
     * 保存用户登录信息
     * @param username 用户名
     * @param role 用户角色
     * @param mustChangePassword 是否需要修改密码
     */
    public void saveUserInfo(String username, String role, boolean mustChangePassword) {
        editor.putString(Constants.KEY_USERNAME, username);
        editor.putString(Constants.KEY_ROLE, role);
        editor.putBoolean(Constants.KEY_MUST_CHANGE_PASSWORD, mustChangePassword);
        editor.putBoolean(Constants.KEY_IS_LOGGED_IN, true);
        editor.apply();
    }

    /**
     * 获取当前登录用户名
     * @return 用户名字符串
     */
    public String getUsername() {
        return prefs.getString(Constants.KEY_USERNAME, "");
    }

    /**
     * 获取当前用户角色
     * @return 角色字符串
     */
    public String getRole() {
        return prefs.getString(Constants.KEY_ROLE, "");
    }

    /**
     * 判断是否需要修改密码
     * @return 是否需要修改密码
     */
    public boolean mustChangePassword() {
        return prefs.getBoolean(Constants.KEY_MUST_CHANGE_PASSWORD, false);
    }

    /**
     * 判断用户是否已登录
     * @return 是否已登录
     */
    public boolean isLoggedIn() {
        return prefs.getBoolean(Constants.KEY_IS_LOGGED_IN, false);
    }

    /**
     * 清除所有会话数据，用于退出登录
     */
    public void clearSession() {
        editor.clear();
        editor.apply();
    }
}
