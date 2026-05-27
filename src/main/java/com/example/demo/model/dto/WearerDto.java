package com.example.demo.model.dto;

import com.example.demo.model.Wearer;

public class WearerDto {
    private Long id;
    private String name;
    private String phone;
    private Long deviceId;

    public WearerDto() {}

    public WearerDto(Long id, String name, String phone, Long deviceId) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.deviceId = deviceId;
    }

    public static WearerDto from(Wearer w) {
        return new WearerDto(w.getId(), w.getName(), w.getPhone(), w.getDeviceId());
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }
}
