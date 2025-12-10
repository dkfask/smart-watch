package com.example.demo.socket.protocol;

/**
 * 测试主程序，用于独立验证 ProtocolParser 的解析效果（运行不依赖 Gradle）
 */
public class TestParserMain {
    public static void main(String[] args) {
        String[] samples = new String[] {
            "IWAP00353456789012345#",
            "IWAP00353456789012345,460|00|CMNET#",
            "IWAP00357653050852602,89962030221137165263,416032113716526#",
            "IWAP01 080524A2232.9806N11404.9355E000.1061830323.87,460,0,9520,3671,Home|74-DE-2B-44-88-8C|97&Home1|74-DE-2B-44-88-8C|97#".replace(" ", ""),
            "IWAP03,06000908000102,5555,30#",
            "IWAP01250723V0000.0000N00000.0000E000.0160150029.4704700008200008,310,26,65533,45912587,AP1|8e:76:3f:9b:54:fc|-70,[23.12312@114.23543]#"
        };

        for (String s : samples) {
            System.out.println("----- 原始: " + s);
            BraceletPacket p = ProtocolParser.parse(s);
            if (p == null) {
                System.out.println("解析结果: null\n");
                continue;
            }
            System.out.println("协议号: " + p.getProtocol());
            System.out.println("头: " + p.getHeader());
            System.out.println("接收时间: " + p.getReceiveTime());
            System.out.println("参数: " + p.getParams());
            if (p instanceof LoginPacket) {
                LoginPacket lp = (LoginPacket) p;
                System.out.println("-- Login IMEI: " + lp.getImei());
                System.out.println("-- MCC/MNC/APN: " + lp.getMcc() + "/" + lp.getMnc() + "/" + lp.getApn());
                System.out.println("-- ICCID/IMSI: " + lp.getIccid() + "/" + lp.getImsi());
            }
            if (p instanceof LocationPacket) {
                LocationPacket lp = (LocationPacket) p;
                System.out.println("-- date: " + lp.getDate());
                System.out.println("-- valid: " + lp.getValid());
                System.out.println("-- latRaw/lngRaw: " + lp.getLatRaw() + "/" + lp.getLngRaw());
                System.out.println("-- lat/lng: " + lp.getLat() + "/" + lp.getLng());
                System.out.println("-- extra: " + lp.getExtra());
            }
            if (p instanceof HeartbeatPacket) {
                HeartbeatPacket hp = (HeartbeatPacket) p;
                System.out.println("-- rawPayload: " + hp.getRawPayload());
            }
            System.out.println();
        }
    }
}
