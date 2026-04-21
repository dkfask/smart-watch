package com.smartwatch.monitor.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.google.gson.annotations.SerializedName;

/**
 * 设备模型类，对应后端devices表，同时作为Room数据库实体
 */
@Entity(tableName = "devices")
public class Device {

    /** 设备ID */
    @PrimaryKey
    private Long id;

    /** 设备IMEI号 */
    @ColumnInfo(name = "imei")
    private String imei;

    /** 设备型号 */
    @ColumnInfo(name = "device_model")
    @SerializedName("device_model")
    private String deviceModel;

    /** 移动国家代码 */
    @ColumnInfo(name = "mcc")
    private String mcc;

    /** 移动网络代码 */
    @ColumnInfo(name = "mnc")
    private String mnc;

    /** 接入点名称 */
    @ColumnInfo(name = "apn")
    private String apn;

    /** SIM卡ICCID */
    @ColumnInfo(name = "iccid")
    private String iccid;

    /** 国际移动用户识别码 */
    @ColumnInfo(name = "imsi")
    private String imsi;

    /** 固件版本 */
    @ColumnInfo(name = "firmware_version")
    @SerializedName("firmware_version")
    private String firmwareVersion;

    /** 硬件版本 */
    @ColumnInfo(name = "hardware_version")
    @SerializedName("hardware_version")
    private String hardwareVersion;

    /** 状态：inactive-未激活，active-在线，offline-离线 */
    @ColumnInfo(name = "status")
    private String status;

    /** 创建时间 */
    @ColumnInfo(name = "created_at")
    @SerializedName("created_at")
    private String createdAt;

    /** 更新时间 */
    @ColumnInfo(name = "updated_at")
    @SerializedName("updated_at")
    private String updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getImei() { return imei; }
    public void setImei(String imei) { this.imei = imei; }
    public String getDeviceModel() { return deviceModel; }
    public void setDeviceModel(String deviceModel) { this.deviceModel = deviceModel; }
    public String getMcc() { return mcc; }
    public void setMcc(String mcc) { this.mcc = mcc; }
    public String getMnc() { return mnc; }
    public void setMnc(String mnc) { this.mnc = mnc; }
    public String getApn() { return apn; }
    public void setApn(String apn) { this.apn = apn; }
    public String getIccid() { return iccid; }
    public void setIccid(String iccid) { this.iccid = iccid; }
    public String getImsi() { return imsi; }
    public void setImsi(String imsi) { this.imsi = imsi; }
    public String getFirmwareVersion() { return firmwareVersion; }
    public void setFirmwareVersion(String firmwareVersion) { this.firmwareVersion = firmwareVersion; }
    public String getHardwareVersion() { return hardwareVersion; }
    public void setHardwareVersion(String hardwareVersion) { this.hardwareVersion = hardwareVersion; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
