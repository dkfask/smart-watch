package com.example.demo.socket.protocol;

/**
 * LoginPacket - 对应 AP00 登录包的具体数据模型
 *
 * 本类解析并保存 AP00 上传的几种形式：
 * 1) IMEI
 * 2) IMEI,MCC|MNC|APN
 * 3) IMEI,ICCID,IMSI
 */
public class LoginPacket extends BraceletPacket {

    private String mcc;
    private String mnc;
    private String apn;
    private String iccid;
    private String imsi;

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
}

