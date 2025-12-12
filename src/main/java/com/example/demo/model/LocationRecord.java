package com.example.demo.model;

import jakarta.persistence.*;
import java.util.Date;

/**
 * LocationRecord - 存储设备上行的定位数据（对应上行协议 AP01）
 *
 * 字段说明：
 * - device: 可选的 Device 关联，便于按设备查询（ON DELETE SET NULL）
 * - imei: 上行原始 IMEI，冗余存储以便快速查询
 * - recvTime: 服务器接收该记录的时间
 * - gpsRaw / extraRaw: 原始分段字符串，便于调试与二次解析
 * - latitude / longitude: 解析后的十进制度坐标
 * - speed / direction / address: 常见展示字段
 */
@Entity
@Table(name = "location_records")
public class LocationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 可选的设备外键关联
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Device device;

    @Column(name = "patient_id")
    private Long patientId;

    @Column(length = 15)
    private String imei;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "recv_time", nullable = false)
    private Date recvTime;

    @Column(name = "gps_raw", length = 1024)
    private String gpsRaw;

    @Lob
    @Column(name = "extra_raw")
    private String extraRaw;

    private Double latitude;
    private Double longitude;

    private Double speed;

    @Column(name = "direction")
    private Double direction;

    @Column(length = 255)
    private String address;

    @Column(length = 50)
    private String source;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "datetime default current_timestamp")
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at", nullable = false, columnDefinition = "datetime default current_timestamp on update current_timestamp")
    private Date updatedAt;

    public LocationRecord() {
        this.recvTime = new Date();
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    // getters & setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Device getDevice() { return device; }
    public void setDevice(Device device) { this.device = device; }

    public String getImei() { return imei; }
    public void setImei(String imei) { this.imei = imei; }

    public Date getRecvTime() { return recvTime; }
    public void setRecvTime(Date recvTime) { this.recvTime = recvTime; }

    public String getGpsRaw() { return gpsRaw; }
    public void setGpsRaw(String gpsRaw) { this.gpsRaw = gpsRaw; }

    public String getExtraRaw() { return extraRaw; }
    public void setExtraRaw(String extraRaw) { this.extraRaw = extraRaw; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Double getSpeed() { return speed; }
    public void setSpeed(Double speed) { this.speed = speed; }

    public Double getDirection() { return direction; }
    public void setDirection(Double direction) { this.direction = direction; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}

