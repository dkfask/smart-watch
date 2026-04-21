package com.smartwatch.monitor.utils;

/**
 * 全局常量定义类
 * 包含API基础地址、分页默认值、键名等常量
 */
public class Constants {

    /** API基础地址 */
    public static final String BASE_URL = "http://8.156.83.206:8080/api/";

    /** SharedPreferences文件名 */
    public static final String PREF_NAME = "smartwatch_prefs";

    /** 默认分页大小 */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /** 默认起始页码 */
    public static final int DEFAULT_PAGE = 0;

    /** 会话Cookie键名 */
    public static final String KEY_COOKIES = "cookies";

    /** 用户名键名 */
    public static final String KEY_USERNAME = "username";

    /** 用户角色键名 */
    public static final String KEY_ROLE = "role";

    /** 登录状态键名 */
    public static final String KEY_IS_LOGGED_IN = "is_logged_in";

    /** 是否需要修改密码键名 */
    public static final String KEY_MUST_CHANGE_PASSWORD = "must_change_password";

    /** WebSocket路径 */
    public static final String WS_PATH = "/ws";

    /** 报警通知渠道ID */
    public static final String ALARM_CHANNEL_ID = "alarm_channel";

    private Constants() {
    }
}
