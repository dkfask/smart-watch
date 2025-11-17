package com.example.demo.socket.protocol;

/**
 * LocationPacket - 对应 AP01 报文的解析结果模型（简化字段）
 *
 * 字段说明（按常见示例抽取，保留字符串类型以避免解析异常）：
 * - date: 报文中的日期部分（如 080524 -> 2008-05-24）
 * - valid: GPS 有效性标志，A 或 V
 * - latRaw / lngRaw: 原始纬度/经度字符串
 * - lat / lng: 解析后的十进制度或 null（如解析失败）
 * - speed: 速度字符串
 * - gpsTime: 报文中产生定位数据的时间
 * - direction: 方向角
 * - extra: 其它参数原始串
 */
public class LocationPacket extends BraceletPacket {
    private String date;
    private String valid;
    private String latRaw;
    private String lngRaw;
    private Double lat;
    private Double lng;
    private String speed;
    private String gpsTime;
    private String direction;
    private String extra;

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getValid() { return valid; }
    public void setValid(String valid) { this.valid = valid; }

    public String getLatRaw() { return latRaw; }
    public void setLatRaw(String latRaw) { this.latRaw = latRaw; }

    public String getLngRaw() { return lngRaw; }
    public void setLngRaw(String lngRaw) { this.lngRaw = lngRaw; }

    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }

    public Double getLng() { return lng; }
    public void setLng(Double lng) { this.lng = lng; }

    public String getSpeed() { return speed; }
    public void setSpeed(String speed) { this.speed = speed; }

    public String getGpsTime() { return gpsTime; }
    public void setGpsTime(String gpsTime) { this.gpsTime = gpsTime; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public String getExtra() { return extra; }
    public void setExtra(String extra) { this.extra = extra; }
}

