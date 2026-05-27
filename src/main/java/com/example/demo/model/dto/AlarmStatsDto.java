package com.example.demo.model.dto;

/**
 * 报警统计数据DTO，替代AlarmService.getAlarmStats()返回的Map<String, Object>
 */
public class AlarmStatsDto {
    private long total;
    private long pending;
    private long handled;
    private long falseAlarm;
    private long unread;

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public long getPending() { return pending; }
    public void setPending(long pending) { this.pending = pending; }
    public long getHandled() { return handled; }
    public void setHandled(long handled) { this.handled = handled; }
    public long getFalseAlarm() { return falseAlarm; }
    public void setFalseAlarm(long falseAlarm) { this.falseAlarm = falseAlarm; }
    public long getUnread() { return unread; }
    public void setUnread(long unread) { this.unread = unread; }
}
