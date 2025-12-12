package com.example.demo.socket.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 日志处理器，负责处理日志记录和文件保存功能
 */
public class LogProcessor {
    private static final Logger log = LoggerFactory.getLogger(LogProcessor.class);
    private final Path saveBasePath;
    private final Path rawDirPath;
    private final Path deviceDirPath;
    private final Map<String, Object> deviceLocks = new ConcurrentHashMap<>();
    private final Object rawFileLock = new Object();

    public LogProcessor(String saveDirName) {
        // 处理空值情况，使用默认目录名
        String dirName = (saveDirName == null || saveDirName.isEmpty()) ? "mpband_data" : saveDirName;
        this.saveBasePath = Paths.get(System.getProperty("user.dir")).resolve(dirName);
        this.rawDirPath = saveBasePath.resolve("raw");
        this.deviceDirPath = saveBasePath.resolve("devices");
        
        // 启动时确保磁盘保存目录存在
        try {
            Files.createDirectories(rawDirPath);
            Files.createDirectories(deviceDirPath);
        } catch (Exception ex) {
            log.warn("无法创建保存目录 {}: {}", dirName, ex.getMessage());
        }
    }

    /**
     * 保存原始报文到磁盘并追加到设备日志
     */
    public void saveRawAndDeviceLog(String message, String imei, String clientInfo) {
        try {
            // 1. 按天保存原始报文到 raw 目录
            String todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            Path dailyRawFile = rawDirPath.resolve("raw_" + todayStr + ".log");
            String logLine = String.format("[%s] %s\n", getCurrentTime(), message);
            
            synchronized (rawFileLock) {
                Files.writeString(dailyRawFile, logLine, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
            
            // 2. 如果解析出了 IMEI，则追加到设备专属日志
            if (imei != null && !imei.isEmpty()) {
                // 设备日志文件名格式：{imei}_yyyyMMdd.log，每个设备每天一个文件
                String day = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                Path deviceLogFile = deviceDirPath.resolve(String.format("%s_%s.log", imei, day));
                String deviceLogLine = String.format("[%s] %s\n", getCurrentTime(), message);
                
                // 使用设备专属锁保证线程安全
                Object lock = deviceLocks.computeIfAbsent(imei, k -> new Object());
                synchronized (lock) {
                    Files.writeString(deviceLogFile, deviceLogLine, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                }
            }
        } catch (IOException e) {
            log.warn("保存原始报文失败: {}", e.getMessage());
        }
    }

    /**
     * 保存响应日志
     */
    public void saveResponseLog(String response, String imei, String clientInfo) {
        try {
            if (imei != null && !imei.isEmpty()) {
                // 设备日志文件名格式：{imei}_yyyyMMdd.log，每个设备每天一个文件
                String day = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                Path deviceLogFile = deviceDirPath.resolve(String.format("%s_%s.log", imei, day));
                String logLine = String.format("[%s] RESPONSE: %s\n", getCurrentTime(), response);
                
                // 使用设备专属锁保证线程安全
                Object lock = deviceLocks.computeIfAbsent(imei, k -> new Object());
                synchronized (lock) {
                    Files.writeString(deviceLogFile, logLine, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                }
            }
        } catch (IOException e) {
            log.warn("保存响应日志失败: {}", e.getMessage());
        }
    }

    /**
     * 获取当前时间字符串
     */
    private String getCurrentTime() {
        return java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
    }
}
