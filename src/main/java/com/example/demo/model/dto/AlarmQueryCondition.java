package com.example.demo.model.dto;

import java.util.Date;

/**
 * 报警查询条件DTO，替代AlarmService中7个单独的查询方法
 */
public class AlarmQueryCondition {
    private Long deviceId;
    private Long patientId;
    private String status;
    private Date startTime;
    private Date endTime;
    private int page;
    private int size;

    /**
     * 判断是否有过滤条件
     * @return 是否存在至少一个过滤条件
     */
    public boolean hasFilter() {
        return deviceId != null || patientId != null || status != null || startTime != null;
    }

    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }
    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
}
