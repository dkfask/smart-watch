package com.example.demo.tools;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * BraceletClientSimulator - 设备端模拟器（纯 Java）
 *
 * 用途：
 * - 本地或联机测试 MpbandServer 的连接稳定性与协议收发；
 * - 可选择是否在上行报文末尾追加 CRLF，用于复现“仅发送 # 不换行”的设备行为；
 * - 会读取服务端应答（按 '#' 作为结束）并打印到控制台。
 *
 * 运行示例（Windows cmd）：
 * 1) 先在一个窗口启动 Spring Boot（确保 MpbandServer 已启动）：
 *    java -jar build\libs\demo2.jar
 * 2) 在另一个窗口编译并运行模拟器：
 *    gradlew.bat classes && java -cp build\classes\java\main com.example.demo.tools.BraceletClientSimulator
 * 3) 指定参数（可选）：
 *    java -cp build\classes\java\main com.example.demo.tools.BraceletClientSimulator localhost 9000 353456789012345 false 3 2000
 *    含义：host port imei appendCRLF heartbeats intervalMillis
 */
public class BraceletClientSimulator {

    public static void main(String[] args) throws Exception {
        // 参数解析
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 9000;
        String imei = args.length > 2 ? args[2] : "353456789012345"; // 15 位
        boolean appendCRLF = args.length > 3 ? Boolean.parseBoolean(args[3]) : false;
        int heartbeatTimes = args.length > 4 ? Integer.parseInt(args[4]) : 2; // 发送心跳次数
        long intervalMs = args.length > 5 ? Long.parseLong(args[5]) : 3000; // 心跳间隔

        System.out.printf("连接到服务器 %s:%d...%n", host, port);
        try (Socket sock = new Socket()) {
            // 设置连接与读超时，便于观测
            sock.connect(new InetSocketAddress(host, port), 5000);
            sock.setSoTimeout(15000);
            try (BufferedInputStream in = new BufferedInputStream(sock.getInputStream());
                 BufferedOutputStream out = new BufferedOutputStream(sock.getOutputStream())) {

                // 1) 发送 AP00 登录（可选是否在结尾追加 CRLF）
                String login = "IWAP00" + imei + "#";
                writeFrame(out, login, appendCRLF);
                System.out.println("[SIM] 已发送登录: " + login + (appendCRLF ? "\\r\\n" : ""));

                String resp1 = readUntilHash(in);
                System.out.println("[SIM] 收到应答: " + resp1);

                // 2) 连续发送若干 AP03 心跳以验证连接是否保持
                for (int i = 1; i <= heartbeatTimes; i++) {
                    TimeUnit.MILLISECONDS.sleep(intervalMs);
                    String hb = "IWAP03SIM," + i + "#"; // 简化心跳报文
                    writeFrame(out, hb, appendCRLF);
                    System.out.println("[SIM] 已发送心跳: " + hb + (appendCRLF ? "\\r\\n" : ""));
                    String r = readUntilHash(in);
                    System.out.println("[SIM] 收到应答: " + r);
                }

                System.out.println("[SIM] 测试完成，保持 2 秒后断开...");
                TimeUnit.SECONDS.sleep(2);
            }
        }
    }

    /**
     * 写出一帧（以 '#' 结尾，按需追加 CRLF）。
     */
    private static void writeFrame(OutputStream out, String s, boolean appendCRLF) throws IOException {
        byte[] data = s.getBytes(StandardCharsets.UTF_8);
        out.write(data);
        if (appendCRLF) {
            out.write('\r');
            out.write('\n');
        }
        out.flush();
    }

    /**
     * 简单读取直到遇到 '#'（不处理多字节编码场景，协议为 ASCII 范围即可）。
     */
    private static String readUntilHash(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        int b;
        while ((b = in.read()) != -1) {
            char c = (char) (b & 0xFF);
            if (c == '\r' || c == '\n') continue; // 过滤 CRLF
            sb.append(c);
            if (c == '#') break;
        }
        return sb.toString();
    }
}

