package com.smartwatch.monitor.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.smartwatch.monitor.R;
import com.smartwatch.monitor.model.Alert;
import com.smartwatch.monitor.ui.MainActivity;

/**
 * 报警通知工具类，负责创建通知渠道和发送报警通知
 * 支持不同报警级别的通知样式和点击跳转
 */
public class AlarmNotificationHelper {

    /** 报警通知渠道ID */
    private static final String CHANNEL_ID = "alarm_channel";

    /** 报警通知渠道名称 */
    private static final String CHANNEL_NAME = "报警通知";

    /** 报警通知渠道描述 */
    private static final String CHANNEL_DESCRIPTION = "接收设备报警实时通知";

    /** 通知ID计数器 */
    private static int notificationId = 1000;

    /**
     * 创建报警通知渠道（Android 8.0+必须）
     * @param context 应用上下文
     */
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(CHANNEL_DESCRIPTION);
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setShowBadge(true);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * 显示报警通知
     * @param context 应用上下文
     * @param alert 报警数据
     */
    public static void showAlarmNotification(Context context, Alert alert) {
        createNotificationChannel(context);

        Intent intent = new Intent(context, MainActivity.class);
        intent.setAction("ACTION_ALARM_DETAIL");
        intent.putExtra("alarmId", alert.getId() != null ? alert.getId() : -1);
        intent.putExtra("navigateTo", "alarmDetail");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                alert.getId() != null ? alert.getId().intValue() : 0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String typeLabel = getAlarmTypeLabel(alert.getAlertType());
        String levelLabel = getAlarmLevelLabel(alert.getAlertLevel());
        String title = typeLabel + " - " + levelLabel;
        String content = buildNotificationContent(alert);

        int priority = getNotificationPriority(alert.getAlertLevel());
        int color = getAlarmColor(alert.getAlertLevel());

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_alarm)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(priority)
                .setColor(color)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(notificationId++, builder.build());
    }

    /**
     * 构建通知内容文本
     * @param alert 报警数据
     * @return 通知内容
     */
    private static String buildNotificationContent(Alert alert) {
        StringBuilder sb = new StringBuilder();
        if (alert.getAddress() != null && !alert.getAddress().isEmpty()) {
            sb.append("位置: ").append(alert.getAddress());
        }
        if (alert.getImei() != null && !alert.getImei().isEmpty()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("设备: ").append(alert.getImei());
        }
        if (alert.getAlertTime() != null && !alert.getAlertTime().isEmpty()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("时间: ").append(alert.getAlertTime());
        }
        return sb.length() > 0 ? sb.toString() : "收到新的报警信息";
    }

    /**
     * 获取报警类型中文标签
     * @param type 报警类型
     * @return 中文标签
     */
    private static String getAlarmTypeLabel(String type) {
        if (type == null) return "未知报警";
        switch (type) {
            case "fence_breach": return "围栏越界";
            case "low_battery": return "低电量";
            case "sos": return "SOS求救";
            case "fall": return "跌倒报警";
            case "heart_rate": return "心率异常";
            default: return type;
        }
    }

    /**
     * 获取报警级别中文标签
     * @param level 报警级别
     * @return 中文标签
     */
    private static String getAlarmLevelLabel(String level) {
        if (level == null) return "提示";
        switch (level) {
            case "critical": return "紧急";
            case "warning": return "警告";
            case "info": return "提示";
            default: return level;
        }
    }

    /**
     * 获取通知优先级
     * @param level 报警级别
     * @return 通知优先级
     */
    private static int getNotificationPriority(String level) {
        if ("critical".equals(level)) {
            return NotificationCompat.PRIORITY_HIGH;
        } else if ("warning".equals(level)) {
            return NotificationCompat.PRIORITY_DEFAULT;
        }
        return NotificationCompat.PRIORITY_LOW;
    }

    /**
     * 获取报警级别对应颜色
     * @param level 报警级别
     * @return 颜色值
     */
    private static int getAlarmColor(String level) {
        if ("critical".equals(level)) {
            return 0xFFEF4444;
        } else if ("warning".equals(level)) {
            return 0xFFF59E0B;
        }
        return 0xFF3B82F6;
    }

    /**
     * 取消所有报警通知
     * @param context 应用上下文
     */
    public static void cancelAllNotifications(Context context) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancelAll();
    }
}
