package com.example.demo.model.dto;

import com.example.demo.model.HealthAlertThreshold;

public class HealthAlertThresholdDto {
    private String dataType;
    private Double minValue;
    private Double maxValue;
    private boolean enabled;

    public HealthAlertThresholdDto() {}

    public HealthAlertThresholdDto(String dataType, Double minValue, Double maxValue, boolean enabled) {
        this.dataType = dataType;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.enabled = enabled;
    }

    public static HealthAlertThresholdDto from(HealthAlertThreshold h) {
        return new HealthAlertThresholdDto(h.getDataType(), h.getMinValue(), h.getMaxValue(), h.isEnabled());
    }

    public String getDataType() { return dataType; }
    public void setDataType(String dataType) { this.dataType = dataType; }
    public Double getMinValue() { return minValue; }
    public void setMinValue(Double minValue) { this.minValue = minValue; }
    public Double getMaxValue() { return maxValue; }
    public void setMaxValue(Double maxValue) { this.maxValue = maxValue; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
