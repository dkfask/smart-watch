package com.example.demo.socket.protocol;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LocationProtocolTest {

    @Test
    void testGpsLocationParsing() {
        // 第一种：通过GPS定位
        String gpsData = "IWAP01080524A2232.9806N11404.9355E000.1061830323.8706000908000102,460,0,9520,3671,Home|74-DE-2B-44-88-8C|97& Home1|74-DE-2B-44-88-8C|97&Home2|74-DE-2B-44-88-8C|97& Home3|74-DE-2B-44-88-8C|97#";
        
        try {
            BraceletPacket packet = ProtocolParser.parse(gpsData);
            // 转换为LocationPacket类型
            if (packet instanceof LocationPacket) {
                LocationPacket locationPacket = (LocationPacket) packet;
                
                // 验证基本信息
                assertEquals("AP01", locationPacket.getProtocol());
                assertEquals("080524", locationPacket.getDate());
                assertEquals("A", locationPacket.getValid());
                
                // 验证GPS坐标
                assertNotNull(locationPacket.getLat());
                assertNotNull(locationPacket.getLng());
                // 22°32.9806'N 转换为十进制约为22.549677
                assertTrue(Math.abs(locationPacket.getLat() - 22.549677) < 0.001);
                // 114°04.9355'E 转换为十进制约为114.082258
                assertTrue(Math.abs(locationPacket.getLng() - 114.082258) < 0.001);
                
                // 验证速度和方向
                assertEquals("000.1", locationPacket.getSpeed());
                assertEquals("323.87", locationPacket.getDirection());
                
                // 验证定位源
                assertEquals("gps", locationPacket.getParams().get("locationSource"));
                
                System.out.println("✅ GPS定位数据解析成功");
                System.out.println("   经纬度: " + locationPacket.getLat() + ", " + locationPacket.getLng());
                System.out.println("   速度: " + locationPacket.getSpeed());
                System.out.println("   方向: " + locationPacket.getDirection());
                System.out.println("   定位源: " + locationPacket.getParams().get("locationSource"));
            } else {
                fail("Packet is not a LocationPacket");
            }
            
        } catch (ProtocolException e) {
            fail("GPS定位数据解析失败: " + e.getMessage());
        }
    }
    
    @Test
    void testNetworkLocationParsing() {
        // 第二种：通过网络定位
        String networkData = "IWAP01250723V0000.0000N00000.0000E000.0160150029.4704700008200008,310,26,65533,45912587,AP1|8e:76:3f:9b:54:fc|-70,[23.12312@114.23543]#";
        
        try {
            BraceletPacket packet = ProtocolParser.parse(networkData);
            // 转换为LocationPacket类型
            if (packet instanceof LocationPacket) {
                LocationPacket locationPacket = (LocationPacket) packet;
                
                // 验证基本信息
                assertEquals("AP01", locationPacket.getProtocol());
                assertEquals("250723", locationPacket.getDate());
                assertEquals("V", locationPacket.getValid());
                
                // 验证网络定位经纬度
                assertNotNull(locationPacket.getLat());
                assertNotNull(locationPacket.getLng());
                assertEquals(23.12312, locationPacket.getLat());
                assertEquals(114.23543, locationPacket.getLng());
                
                // 验证速度和方向
                assertEquals("000.0", locationPacket.getSpeed());
                assertEquals("29.47", locationPacket.getDirection());
                
                // 验证定位源
                assertEquals("wifi", locationPacket.getParams().get("locationSource"));
                
                System.out.println("✅ 网络定位数据解析成功");
                System.out.println("   经纬度: " + locationPacket.getLat() + ", " + locationPacket.getLng());
                System.out.println("   速度: " + locationPacket.getSpeed());
                System.out.println("   方向: " + locationPacket.getDirection());
                System.out.println("   定位源: " + locationPacket.getParams().get("locationSource"));
            } else {
                fail("Packet is not a LocationPacket");
            }
            
        } catch (ProtocolException e) {
            fail("网络定位数据解析失败: " + e.getMessage());
        }
    }
    
    @Test
    void testRawPrefixParsing() {
        // 带有raw=前缀的定位数据
        String rawPrefixData = "IWAP01raw=251212V0000.0000N00000.0000E000.0073042000.0006000008400008,460,04,32845,236380872,AP1|d2:82:3d:a7:e2:ce|-46&AP2|5c:02:14:b3:4d:56|-53&AP3|5e:02:14:a3:4d:56|-53&AP4|52:74:8d:c7:73:a6|-69&AP5|06:05:88:1e:6d:ba|-81,[30.886866@103.5945]#";
        
        try {
            BraceletPacket packet = ProtocolParser.parse(rawPrefixData);
            // 转换为LocationPacket类型
            if (packet instanceof LocationPacket) {
                LocationPacket locationPacket = (LocationPacket) packet;
                
                // 验证基本信息
                assertEquals("AP01", locationPacket.getProtocol());
                assertEquals("251212", locationPacket.getDate());
                assertEquals("V", locationPacket.getValid());
                
                // 验证网络定位经纬度
                assertNotNull(locationPacket.getLat());
                assertNotNull(locationPacket.getLng());
                assertEquals(30.886866, locationPacket.getLat());
                assertEquals(103.5945, locationPacket.getLng());
                
                // 验证定位源
                assertEquals("wifi", locationPacket.getParams().get("locationSource"));
                
                System.out.println("✅ 带raw=前缀的定位数据解析成功");
                System.out.println("   经纬度: " + locationPacket.getLat() + ", " + locationPacket.getLng());
                System.out.println("   定位源: " + locationPacket.getParams().get("locationSource"));
            } else {
                fail("Packet is not a LocationPacket");
            }
            
        } catch (ProtocolException e) {
            fail("带raw=前缀的定位数据解析失败: " + e.getMessage());
        }
    }
}