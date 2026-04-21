package com.smartwatch.monitor.model;

import com.google.gson.annotations.SerializedName;

/**
 * 报警统计数据DTO
 */
public class AlarmStatsDto {

    /** 总报警数 */
    private long total;

    /** 待处理数 */
    private long pending;

    /** 已处理数 */
    private long handled;

    /** 误报数 */
    @SerializedName("falseAlarm")
    private long falseAlarm;

    /** 未读数 */
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
