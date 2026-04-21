package com.smartwatch.monitor.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.google.gson.annotations.SerializedName;

/**
 * 病人模型类，对应后端patients表，同时作为Room数据库实体
 */
@Entity(tableName = "patients")
public class Patient {

    /** 病人ID */
    @PrimaryKey
    private Long id;

    /** 姓名 */
    @ColumnInfo(name = "name")
    private String name;

    /** 性别 */
    @ColumnInfo(name = "gender")
    private String gender;

    /** 年龄 */
    @ColumnInfo(name = "age")
    private Integer age;

    /** 身份证号 */
    @ColumnInfo(name = "id_card")
    @SerializedName("id_card")
    private String idCard;

    /** 病房 */
    @ColumnInfo(name = "ward")
    private String ward;

    /** 床位 */
    @ColumnInfo(name = "bed")
    private String bed;

    /** 联系电话 */
    @ColumnInfo(name = "phone")
    private String phone;

    /** 紧急联系人 */
    @ColumnInfo(name = "emergency_contact")
    @SerializedName("emergency_contact")
    private String emergencyContact;

    /** 紧急联系电话 */
    @ColumnInfo(name = "emergency_phone")
    @SerializedName("emergency_phone")
    private String emergencyPhone;

    /** 诊断信息 */
    @ColumnInfo(name = "diagnosis")
    private String diagnosis;

    /** 入院日期 */
    @ColumnInfo(name = "admission_date")
    @SerializedName("admission_date")
    private String admissionDate;

    /** 出院日期 */
    @ColumnInfo(name = "discharge_date")
    @SerializedName("discharge_date")
    private String dischargeDate;

    /** 状态：admitted-住院中，discharged-已出院 */
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
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public String getIdCard() { return idCard; }
    public void setIdCard(String idCard) { this.idCard = idCard; }
    public String getWard() { return ward; }
    public void setWard(String ward) { this.ward = ward; }
    public String getBed() { return bed; }
    public void setBed(String bed) { this.bed = bed; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmergencyContact() { return emergencyContact; }
    public void setEmergencyContact(String emergencyContact) { this.emergencyContact = emergencyContact; }
    public String getEmergencyPhone() { return emergencyPhone; }
    public void setEmergencyPhone(String emergencyPhone) { this.emergencyPhone = emergencyPhone; }
    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }
    public String getAdmissionDate() { return admissionDate; }
    public void setAdmissionDate(String admissionDate) { this.admissionDate = admissionDate; }
    public String getDischargeDate() { return dischargeDate; }
    public void setDischargeDate(String dischargeDate) { this.dischargeDate = dischargeDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
