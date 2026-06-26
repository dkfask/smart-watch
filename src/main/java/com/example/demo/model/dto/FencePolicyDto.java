package com.example.demo.model.dto;

import com.example.demo.model.FencePolicy;

/**
 * Per-fence policy payload. Mirrors {@link FencePolicy} field-for-field.
 */
public class FencePolicyDto {
    private Long fenceId;
    private Boolean enterAlert;
    private Boolean exitAlert;
    private Boolean dwellAlert;
    private Integer dwellSeconds;
    private Integer debounceCount;
    private Integer cooldownSec;

    public FencePolicyDto() {}

    public static FencePolicyDto fromEntity(FencePolicy p) {
        if (p == null) return null;
        FencePolicyDto d = new FencePolicyDto();
        d.fenceId = p.getFenceId();
        d.enterAlert = p.getEnterAlert();
        d.exitAlert = p.getExitAlert();
        d.dwellAlert = p.getDwellAlert();
        d.dwellSeconds = p.getDwellSeconds();
        d.debounceCount = p.getDebounceCount();
        d.cooldownSec = p.getCooldownSec();
        return d;
    }

    public FencePolicy toEntity() {
        FencePolicy p = new FencePolicy();
        p.setFenceId(fenceId);
        p.setEnterAlert(enterAlert);
        p.setExitAlert(exitAlert);
        p.setDwellAlert(dwellAlert);
        p.setDwellSeconds(dwellSeconds);
        p.setDebounceCount(debounceCount);
        p.setCooldownSec(cooldownSec);
        return p;
    }

    public Long getFenceId() { return fenceId; }
    public void setFenceId(Long fenceId) { this.fenceId = fenceId; }
    public Boolean getEnterAlert() { return enterAlert; }
    public void setEnterAlert(Boolean v) { this.enterAlert = v; }
    public Boolean getExitAlert() { return exitAlert; }
    public void setExitAlert(Boolean v) { this.exitAlert = v; }
    public Boolean getDwellAlert() { return dwellAlert; }
    public void setDwellAlert(Boolean v) { this.dwellAlert = v; }
    public Integer getDwellSeconds() { return dwellSeconds; }
    public void setDwellSeconds(Integer v) { this.dwellSeconds = v; }
    public Integer getDebounceCount() { return debounceCount; }
    public void setDebounceCount(Integer v) { this.debounceCount = v; }
    public Integer getCooldownSec() { return cooldownSec; }
    public void setCooldownSec(Integer v) { this.cooldownSec = v; }
}
