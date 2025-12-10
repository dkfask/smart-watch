package com.example.demo.socket.protocol;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * ProtocolParser - 将原始手环报文解析为具体的 BraceletPacket 子类
 *
 * 解析策略（保守方式，尽量不抛异常，仅进行必要解析）：
 * - 校验包头 IW 与结束符 #
 * - 提取协议号（位置 2-6）例如 AP00
 * - 使用 ProtocolHandlerFactory 获取相应的协议处理器
 * - 调用协议处理器进行具体解析
 */
@Component
public class ProtocolParser {
    private static final String HEADER = "IW";
    private static final String END_MARKER = "#";
    
    // 性能监控指标
    private final Counter parseSuccessCounter;
    private final Counter parseErrorCounter;
    private final Timer parseTimer;
    private final Counter checksumSuccessCounter;
    private final Counter checksumErrorCounter;
    
    // 静态实例，用于兼容原有静态调用
    private static ProtocolParser instance;
    
    @Autowired
    public ProtocolParser(MeterRegistry meterRegistry) {
        // 初始化性能监控指标
        this.parseSuccessCounter = Counter.builder("protocol.parse.success")
                .description("Number of successful protocol parsing operations")
                .register(meterRegistry);
        
        this.parseErrorCounter = Counter.builder("protocol.parse.error")
                .description("Number of failed protocol parsing operations")
                .register(meterRegistry);
        
        this.parseTimer = Timer.builder("protocol.parse.duration")
                .description("Time taken to parse protocol messages")
                .register(meterRegistry);
        
        this.checksumSuccessCounter = Counter.builder("protocol.checksum.success")
                .description("Number of successful checksum validations")
                .register(meterRegistry);
        
        this.checksumErrorCounter = Counter.builder("protocol.checksum.error")
                .description("Number of failed checksum validations")
                .register(meterRegistry);
        
        // 设置静态实例
        instance = this;
    }
    
    // 获取静态实例（用于兼容原有静态调用）
    private static ProtocolParser getInstance() {
        if (instance == null) {
            // 当没有Spring容器时，创建一个默认实例（用于测试）
            instance = new ProtocolParser(new io.micrometer.core.instrument.simple.SimpleMeterRegistry());
        }
        return instance;
    }

    public static BraceletPacket parse(String raw) throws ProtocolException {
        ProtocolParser parser = getInstance();
        
        // 记录解析开始时间
        long startTime = System.nanoTime();
        
        try {
            if (raw == null) {
                parser.parseErrorCounter.increment();
                throw new ProtocolException("原始报文为空");
            }
            raw = raw.trim();
            if (!raw.startsWith(HEADER)) {
                parser.parseErrorCounter.increment();
                throw new ProtocolException("报文缺少正确的包头 IW", raw);
            }
            if (!raw.endsWith(END_MARKER)) {
                parser.parseErrorCounter.increment();
                throw new ProtocolException("报文缺少正确的结束符 #", raw);
            }
            if (raw.length() < 6) {
                parser.parseErrorCounter.increment();
                throw new ProtocolException("报文长度不足", raw);
            }

            String protocol = raw.substring(2, 6);
            
            // 提取协议版本号（假设版本号位于协议号之后，格式为&V=1.0）
            String version = "1.0"; // 默认版本号
            int versionIndex = raw.indexOf("&V=");
            if (versionIndex != -1) {
                int endIndex = raw.indexOf('&', versionIndex + 3);
                if (endIndex == -1) {
                    endIndex = raw.indexOf('#', versionIndex + 3);
                }
                if (endIndex != -1) {
                    version = raw.substring(versionIndex + 3, endIndex);
                }
            }
            
            // 使用协议处理器工厂获取相应的协议处理器
            ProtocolHandler handler = ProtocolHandlerFactory.getHandler(protocol, version);
            
            // 验证校验和
            if (handler instanceof BaseProtocolHandler) {
                BaseProtocolHandler baseHandler = (BaseProtocolHandler) handler;
                String checksum = baseHandler.extractChecksum(raw);
                if (checksum != null) {
                    String dataWithoutChecksum = baseHandler.extractDataWithoutChecksum(raw);
                    if (baseHandler.validateChecksum(dataWithoutChecksum, checksum)) {
                        parser.checksumSuccessCounter.increment();
                    } else {
                        parser.checksumErrorCounter.increment();
                        throw new ProtocolException("校验和验证失败", raw, protocol);
                    }
                }
            }
            
            // 调用协议处理器进行解析
            BraceletPacket packet = handler.parse(raw);
            if (packet == null) {
                parser.parseErrorCounter.increment();
                throw new ProtocolException("协议解析失败", raw, protocol);
            }
            
            // 设置协议版本号
            packet.setVersion(version);
            
            // 记录解析成功
            parser.parseSuccessCounter.increment();
            
            return packet;
        } catch (ProtocolException e) {
            // 已经是 ProtocolException，直接抛出
            parser.parseErrorCounter.increment();
            throw e;
        } catch (Exception e) {
            // 其他异常转换为 ProtocolException
            parser.parseErrorCounter.increment();
            throw new ProtocolException("协议解析异常", raw, e);
        } finally {
            // 记录解析耗时
            long endTime = System.nanoTime();
            long durationNanos = endTime - startTime;
            parser.parseTimer.record(durationNanos, TimeUnit.NANOSECONDS);
        }
    }
}