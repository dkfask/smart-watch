package com.example.demo.model;

import jakarta.persistence.*;
import java.util.Date;

/**
 * 围栏病人关联实体类
 * 用于实现围栏与病人的多对多关联关系
 */
@Entity
@Table(name = "fence_patients")
public class FencePatient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fence_id", nullable = false)
    private GeoFence fence;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "datetime default current_timestamp")
    private Date createdAt;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at", nullable = false, columnDefinition = "datetime default current_timestamp on update current_timestamp")
    private Date updatedAt;
    
    // Default constructor
    public FencePatient() {
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }
    
    // Parameterized constructor
    public FencePatient(GeoFence fence, Patient patient) {
        this.fence = fence;
        this.patient = patient;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public GeoFence getFence() { return fence; }
    public void setFence(GeoFence fence) { this.fence = fence; }
    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}