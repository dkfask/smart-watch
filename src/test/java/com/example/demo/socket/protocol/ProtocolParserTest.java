package com.example.demo.socket.protocol;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ProtocolParserTest - 协议解析器单元测试
 */
public class ProtocolParserTest {
    
    @Test
    public void testParseLoginPacket() throws ProtocolException {
        // 测试AP00登录包解析
        String raw = "IWAP00353456789012345#";
        BraceletPacket packet = ProtocolParser.parse(raw);
        assertNotNull(packet);
        assertEquals("AP00", packet.getProtocol());
        assertEquals("353456789012345", packet.getImei());
        assertEquals("1.0", packet.getVersion());
    }
    
    @Test
    public void testParseLocationPacket() throws ProtocolException {
        // 测试AP01定位包解析
        String raw = "IWAP01080524A2234.5678N11402.3456E000.00123456123.45012345678901234#";
        BraceletPacket packet = ProtocolParser.parse(raw);
        assertNotNull(packet);
        assertEquals("AP01", packet.getProtocol());
        assertEquals("1.0", packet.getVersion());
        assertNotNull(packet.getParams());
    }
    
    @Test
    public void testParseHeartbeatPacket() throws ProtocolException {
        // 测试AP03心跳包解析
        String raw = "IWAP0306000908000102,5555,30#";
        BraceletPacket packet = ProtocolParser.parse(raw);
        assertNotNull(packet);
        assertEquals("AP03", packet.getProtocol());
        assertEquals("1.0", packet.getVersion());
        assertNotNull(packet.getParams());
    }
    
    @Test
    public void testParseWithVersion() throws ProtocolException {
        // 测试带版本号的报文解析
        String raw = "IWAP00353456789012345&V=2.0#";
        BraceletPacket packet = ProtocolParser.parse(raw);
        assertNotNull(packet);
        assertEquals("AP00", packet.getProtocol());
        assertEquals("2.0", packet.getVersion());
        assertEquals("353456789012345", packet.getImei());
    }
    
    @Test
    public void testParseWithChecksum() throws ProtocolException {
        // 测试带校验和的报文解析
        // 计算正确的校验和
        String data = "IWAP00353456789012345";
        BaseProtocolHandler handler = new LoginProtocolHandler();
        int checksum = handler.calculateChecksum(data);
        String checksumStr = Integer.toHexString(checksum).toUpperCase();
        String raw = "IWAP00353456789012345&CS=" + checksumStr + "#";
        BraceletPacket packet = ProtocolParser.parse(raw);
        assertNotNull(packet);
        assertEquals("AP00", packet.getProtocol());
        assertEquals("1.0", packet.getVersion());
        assertEquals("353456789012345", packet.getImei());
    }
    
    @Test
    public void testParseInvalidPacket() {
        // 测试无效报文解析
        String raw = "InvalidPacket#";
        assertThrows(ProtocolException.class, () -> ProtocolParser.parse(raw));
    }
    
    @Test
    public void testParseNullPacket() {
        // 测试空报文解析
        assertThrows(ProtocolException.class, () -> ProtocolParser.parse(null));
    }
    
    @Test
    public void testParseMissingEndMarker() {
        // 测试缺少结束符的报文解析
        String raw = "IWAP00353456789012345";
        assertThrows(ProtocolException.class, () -> ProtocolParser.parse(raw));
    }
    
    @Test
    public void testParseMissingHeader() {
        // 测试缺少包头的报文解析
        String raw = "AP00353456789012345#";
        assertThrows(ProtocolException.class, () -> ProtocolParser.parse(raw));
    }
    
    @Test
    public void testParseShortPacket() {
        // 测试长度不足的报文解析
        String raw = "IWAP#";
        assertThrows(ProtocolException.class, () -> ProtocolParser.parse(raw));
    }
    
    @Test
    public void testProtocolHandlerFactory() {
        // 测试协议处理器工厂
        ProtocolHandler handler = ProtocolHandlerFactory.getHandler("AP00");
        assertNotNull(handler);
        assertEquals("AP00", handler.getSupportedProtocol());
        
        // 测试获取默认处理器
        ProtocolHandler defaultHandler = ProtocolHandlerFactory.getHandler("UNKNOWN");
        assertNotNull(defaultHandler);
        
        // 测试按版本获取处理器
        ProtocolHandler v1Handler = ProtocolHandlerFactory.getHandler("AP00", "1.0");
        assertNotNull(v1Handler);
        assertEquals("AP00", v1Handler.getSupportedProtocol());
    }
    
    @Test
    public void testChecksumCalculation() {
        // 测试校验和计算
        BaseProtocolHandler handler = new LoginProtocolHandler();
        String data = "IWAP00353456789012345";
        int checksum = handler.calculateChecksum(data);
        assertTrue(checksum >= 0 && checksum <= 255);
        
        // 测试校验和验证
        String checksumStr = Integer.toHexString(checksum).toUpperCase();
        boolean valid = handler.validateChecksum(data, checksumStr);
        assertTrue(valid);
        
        // 测试无效校验和
        boolean invalid = handler.validateChecksum(data, "FF");
        assertFalse(invalid);
    }
}
