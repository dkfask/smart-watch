package com.example.demo.socket.downlink;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * DownlinkService - 服务器下发到设备的命令生成与发送器
 *
 * 说明（功能与设计原则）：
 * - 本类负责按照设备协议格式（IWBPxx,...#）构建下行指令字符串。
 * - 提供常见 BP00/BP12/BP14/BP15 等命令的构建方法。
 * - 提供发送方法将构建好的指令通过已有的 Socket 连接发送给设备。
 * - 对需要特殊编码（UNICODE / GB2312）的字段提供编码工具函数。
 *
 * 使用示例：
 *   DownlinkService svc = new DownlinkService();
 *   String msg = svc.buildBP00(new Date(), 8);
 *   svc.send(socket, msg);
 *
 * 注意事项：
 * - 发送操作会在发送后立即 flush 输出流；调用者应保证 socket 处于已连接状态。
 * - 按协议要求，所有包以字符 '#' 结束；包头固定为 "IW"。
 */
public class DownlinkService {

    /** 包头标识符 */
    public static final String HEADER = "IW";

    /** 包尾标识符 */
    public static final String END_MARKER = "#";

    /** 默认用于时间格式化的格式：yyyyMMddHHmmss（协议中使用的时间格式） */
    private static final SimpleDateFormat TIME_FMT = new SimpleDateFormat("yyyyMMddHHmmss", Locale.ENGLISH);

    /**
     * 将给定时间和时区构造成 BP00（设置时区）下行包。
     * 示例：IWBP00,20140818064408,8#
     *
     * @param time   要下发的时间（通常为服务器时间）
     * @param zone   时区值，示例：8 或 -8
     * @return 构建好的下行包字符串（包含头尾）
     */
    public String buildBP00(Date time, int zone) {
        String t = TIME_FMT.format(time);
        String body = String.format("BP00,%s,%d", t, zone);
        return HEADER + body + END_MARKER;
    }

    /**
     * 构建 BP12（设置 SOS 号码，最多 3 个）的下行包。
     * 示例：IWBP12,353456789012345,080835,1XXXXXXXX,2XXXXXXXX,3XXXXXXXX#
     *
     * @param imei    设备 IMEI（唯一）
     * @param seq     指令流水号，如 6 位字符串（如果传入数字会自动补零到6位）
     * @param sosList 最多 3 个 SOS 电话号码，未填写位置传空字符串
     * @return 构建好的下行包字符串
     */
    public String buildBP12(String imei, String seq, List<String> sosList) {
        String seq6 = normalizeSeq(seq);
        String s1 = (sosList != null && !sosList.isEmpty()) ? sosList.get(0) : "";
        String s2 = (sosList != null && sosList.size() > 1) ? sosList.get(1) : "";
        String s3 = (sosList != null && sosList.size() > 2) ? sosList.get(2) : "";
        String body = String.format("BP12,%s,%s,%s,%s,%s", imei, seq6, emptyToPlaceholder(s1), emptyToPlaceholder(s2), emptyToPlaceholder(s3));
        return HEADER + body + END_MARKER;
    }

    /**
     * 构建 BP14（设置联系人白名单，最多 10 个，名称使用 UNICODE 编码）下行包。
     * 名称需要以 UNICODE（UTF-16BE）字节转为十六进制字符串下发，电话号码保留原文。
     *
     * 示例（简化）：IWBP14,IMEI,080835,NameHex|135xxxx,NameHex2|136xxxx#
     *
     * @param imei   设备 imei
     * @param seq    指令流水号
     * @param names  联系人名称列表（长度最多 10，未填写置空字符串）
     * @param phones 联系人电话列表（长度最多 10，未填写置空字符串）
     * @return 构建好的 BP14 包
     */
    public String buildBP14(String imei, String seq, List<String> names, List<String> phones) {
        String seq6 = normalizeSeq(seq);
        StringBuilder sb = new StringBuilder();
        sb.append("BP14,").append(imei).append(',').append(seq6).append(',');
        // 组成最多10组，每组 nameHex|phone
        for (int i = 0; i < 10; i++) {
            String name = (names != null && i < names.size()) ? names.get(i) : "";
            String phone = (phones != null && i < phones.size()) ? phones.get(i) : "";
            String nameHex = name.isEmpty() ? "" : toUnicodeHex(name);
            sb.append(nameHex).append('|').append(emptyToPlaceholder(phone));
            if (i < 9) sb.append(',');
        }
        return HEADER + sb.toString() + END_MARKER;
    }

    /**
     * 构建 BP15（定位间隔设置）下行包。
     * 示例：IWBP15,353456789012345,080835,300#
     *
     * @param imei     设备 imei
     * @param seq      指令流水号
     * @param interval 定位间隔（秒）
     * @return 构建好的字符串
     */
    public String buildBP15(String imei, String seq, int interval) {
        String seq6 = normalizeSeq(seq);
        String body = String.format("BP15,%s,%s,%d", imei, seq6, interval);
        return HEADER + body + END_MARKER;
    }

    /**
     * 构建 BP16（立即定位指令）下行包。
     * 示例：IWBP16,353456789012345,080835#
     */
    public String buildBP16(String imei, String seq) {
        String seq6 = normalizeSeq(seq);
        return HEADER + String.format("BP16,%s,%s", imei, seq6) + END_MARKER;
    }

    /**
     * 构建 BP17（恢复出厂设置）下行包。
     */
    public String buildBP17(String imei, String seq) {
        String seq6 = normalizeSeq(seq);
        return HEADER + String.format("BP17,%s,%s", imei, seq6) + END_MARKER;
    }

    /**
     * 构建 BP18（重启设备）下行包。
     */
    public String buildBP18(String imei, String seq) {
        String seq6 = normalizeSeq(seq);
        return HEADER + String.format("BP18,%s,%s", imei, seq6) + END_MARKER;
    }

    /**
     * 构建 BP19（设置服务器信息）下行包。
     * 示例：IWBP19,353456789012345,080835,0,127.0.0.1,8011#
     */
    public String buildBP19(String imei, String seq, int domainFlag, String hostOrIp, int port) {
        String seq6 = normalizeSeq(seq);
        return HEADER + String.format("BP19,%s,%s,%d,%s,%d", imei, seq6, domainFlag, hostOrIp, port) + END_MARKER;
    }

    /**
     * 构建 BP20（设置设备语言与时区）下行包。
     * 示例：IWBP20,353456789012345,080835,0,8#
     */
    public String buildBP20(String imei, String seq, int language, int timezone) {
        String seq6 = normalizeSeq(seq);
        return HEADER + String.format("BP20,%s,%s,%d,%d", imei, seq6, language, timezone) + END_MARKER;
    }

    /**
     * 构建 BP31（关机）下行包。
     */
    public String buildBP31(String imei, String seq) {
        String seq6 = normalizeSeq(seq);
        return HEADER + String.format("BP31,%s,%s", imei, seq6) + END_MARKER;
    }

    /**
     * 构建 BP32（拨打电话）下行包。
     */
    public String buildBP32(String imei, String seq, String phone) {
        String seq6 = normalizeSeq(seq);
        return HEADER + String.format("BP32,%s,%s,%s", imei, seq6, phone) + END_MARKER;
    }

    /**
     * 构建 BP33（工作模式）下行包。
     */
    public String buildBP33(String imei, String seq, int workMode) {
        String seq6 = normalizeSeq(seq);
        return HEADER + String.format("BP33,%s,%s,%d", imei, seq6, workMode) + END_MARKER;
    }

    /**
     * 构建 BP34（自定义定位模式）下行包：示例 IWBP34,IMEI,seq,mode,interval,gpsFlag#
     */
    public String buildBP34(String imei, String seq, int mode, int intervalSec, int gpsFlag) {
        String seq6 = normalizeSeq(seq);
        return HEADER + String.format("BP34,%s,%s,%d,%d,%d", imei, seq6, mode, intervalSec, gpsFlag) + END_MARKER;
    }

    /**
     * 构建 BP40（快捷指令下发），内容需使用 GB2312 编码后转为十六进制字符串下发。
     * 示例：IWBP40,IMEI,seq,HEXSTRING#
     *
     * @param imei   设备 imei
     * @param seq    流水号
     * @param payload 原始指令（明文，如 ">*photo@1*<"）
     * @return 包字符串
     */
    public String buildBP40(String imei, String seq, String payload) {
        String seq6 = normalizeSeq(seq);
        // 使用 Charset.forName 获取 GB2312 编码（StandardCharsets 没有 GB2312 常量）
        String hex = toHex(payload, java.nio.charset.Charset.forName("GB2312"));
        return HEADER + String.format("BP40,%s,%s,%s", imei, seq6, hex) + END_MARKER;
    }

    /**
     * 构建 BP46（立即拍照）下行包：IWBP46,IMEI,seq,指令值,参数#
     */
    public String buildBP46(String imei, String seq, int cmdValue, String param) {
        String seq6 = normalizeSeq(seq);
        return HEADER + String.format("BP46,%s,%s,%d,%s", imei, seq6, cmdValue, param == null ? "" : param) + END_MARKER;
    }

    /**
     * 构建 BP50（下发心跳检测指令）
     */
    public String buildBP50(String imei, String seq) {
        String seq6 = normalizeSeq(seq);
        return HEADER + String.format("BP50,%s,%s", imei, seq6) + END_MARKER;
    }

    /**
     * 构建 BP51（下发电话本，单条）
     */
    public String buildBP51(String imei, String seq, String name, String phone) {
        String seq6 = normalizeSeq(seq);
        String nameHex = name == null || name.isEmpty() ? "" : toUnicodeHex(name);
        return HEADER + String.format("BP51,%s,%s,%s,%s", imei, seq6, nameHex, phone == null ? "" : phone) + END_MARKER;
    }

    /**
     * 构建 BP52（删除电话本，单条）
     */
    public String buildBP52(String imei, String seq, String phone) {
        String seq6 = normalizeSeq(seq);
        return HEADER + String.format("BP52,%s,%s,%s", imei, seq6, phone == null ? "" : phone) + END_MARKER;
    }

    /**
     * 构建 BP84（白名单开关）
     */
    public String buildBP84(String imei, String seq, int value) {
        String seq6 = normalizeSeq(seq);
        return HEADER + String.format("BP84,%s,%s,%d", imei, seq6, value) + END_MARKER;
    }

    /**
     * 构建 BP86（健康监测间隔设置）
     */
    public String buildBP86(String imei, String seq, int onOff, int minutes) {
        String seq6 = normalizeSeq(seq);
        return HEADER + String.format("BP86,%s,%s,%d,%d", imei, seq6, onOff, minutes) + END_MARKER;
    }

    /**
     * 构建 BP88（寻找设备）
     */
    public String buildBP88(String imei, String seq) {
        String seq6 = normalizeSeq(seq);
        return HEADER + String.format("BP88,%s,%s", imei, seq6) + END_MARKER;
    }

    /**
     * 构建 BPMC（运动检测控制）下行包
     * 示例：IWBPMC,353456789012345,0808351,1#
     */
    public String buildBPMC(String imei, String seq, int setting) {
        String seq6 = normalizeSeq(seq);
        return HEADER + String.format("BPMC,%s,%s,%d", imei, seq6, setting) + END_MARKER;
    }

    /**
     * 构建 BPPH（SOS呼叫开关）下行包
     * 示例：IWBPPH,353456789012345,0808351,1#
     */
    public String buildBPPH(String imei, String seq, int setting) {
        String seq6 = normalizeSeq(seq);
        return HEADER + String.format("BPPH,%s,%s,%d", imei, seq6, setting) + END_MARKER;
    }

    /**
     * 构建 BPSM（短信指令）下行包
     * 示例：IWBPSM,355932600021328,680835,@wifictl@=connect-123-12345678-psk#
     */
    public String buildBPSM(String imei, String seq, String commandContent) {
        String seq6 = normalizeSeq(seq);
        return HEADER + String.format("BPSM,%s,%s,%s", imei, seq6, commandContent) + END_MARKER;
    }

    /**
     * 构建 BPTF（时间制度）下行包
     * 示例：IWBPTF,353456789012345,2#
     */
    public String buildBPTF(String imei, int setting) {
        return HEADER + String.format("BPTF,%s,%d", imei, setting) + END_MARKER;
    }

    /**
     * 构建 BPWL（设置与设备绑定的联系人白名单，10个）下行包
     * 格式：IWBPWL,IMEI,指令流水号,联系人1名称|联系人1电话|绑定设备的IMEI,...,联系人10名称|联系人10电话|绑定设备的IMEI#
     */
    public String buildBPWL(String imei, String seq, List<String> contactInfoList) {
        String seq6 = normalizeSeq(seq);
        StringBuilder sb = new StringBuilder();
        sb.append("BPWL,").append(imei).append(',').append(seq6).append(',');
        
        // 组成最多10组，每组 name|phone|deviceImei
        for (int i = 0; i < 10; i++) {
            String contactInfo = (contactInfoList != null && i < contactInfoList.size()) ? contactInfoList.get(i) : "";
            sb.append(contactInfo);
            if (i < 9) sb.append(',');
        }
        return HEADER + sb.toString() + END_MARKER;
    }

    /**
     * 构建 BPXL（测量心率）下行包
     * 示例：IWBPXL,353456789012345,080835#
     */
    public String buildBPXL(String imei, String seq) {
        String seq6 = normalizeSeq(seq);
        return HEADER + String.format("BPXL,%s,%s", imei, seq6) + END_MARKER;
    }

    /**
     * 构建 BPXY（测量血压）下行包
     * 示例：IWBPXY,353456789012345,080835#
     */
    public String buildBPXY(String imei, String seq) {
        String seq6 = normalizeSeq(seq);
        return HEADER + String.format("BPXY,%s,%s", imei, seq6) + END_MARKER;
    }

    /**
     * 构建 BPXZ（测量血氧）下行包
     * 示例：IWBPXZ,353456789012345,080835#
     */
    public String buildBPXZ(String imei, String seq) {
        String seq6 = normalizeSeq(seq);
        return HEADER + String.format("BPXZ,%s,%s", imei, seq6) + END_MARKER;
    }

    /**
     * 构建 BPXX（测量体温）下行包
     * 示例：IWBPXX,353456789012345,080835#
     */
    public String buildBPXX(String imei, String seq) {
        String seq6 = normalizeSeq(seq);
        return HEADER + String.format("BPXX,%s,%s", imei, seq6) + END_MARKER;
    }

    /**
     * 通用发送方法：将构建的下行命令通过 socket 发送到设备。
     * 使用 PrintWriter 且默认编码为 UTF-8 写入并 flush。
     *
     * @param socket  已连接的设备 socket
     * @param message 要发送的完整消息（如 IWBP00,....#）
     * @throws IOException 发送失败时抛出
     */
    public void send(Socket socket, String message) throws IOException {
        if (socket == null || socket.isClosed()) {
            throw new IOException("Socket is null or closed");
        }
        // 使用与设备约定的字符编码：通常设备使用 ASCII/GB2312/UTF-8；这里默认使用 UTF-8 发送明文包
        PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)), true);
        writer.print(message);
        // 一些设备期待换行或 CRLF 作为包结束的分割；协议以 # 结束，通常不需要额外换行。
        // 为兼容性仍然写入换行符并 flush。
        writer.print("\n");
        writer.flush();
    }

    // -------------------- 工具方法 --------------------

    /**
     * 将给定字符串按照指定编码（例如 GB2312/UTF-8）转为十六进制字符串（大写）
     * 可用于把文本内容转换为十六进制下发（BP40 等）。
     *
     * @param s       原始字符串
     * @param charset 目标字符集
     * @return 十六进制大写表示
     */
    public static String toHex(String s, java.nio.charset.Charset charset) {
        if (s == null || s.isEmpty()) return "";
        byte[] bytes = s.getBytes(charset);
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    /**
     * 将字符串编码为 UNICODE（UTF-16BE）并输出为大写十六进制字符串（无前缀）
     * 设备手册中 "UNICODE编码直接下发byte" 的场景可使用此函数。
     *
     * @param s 原始字符串
     * @return 大写十六进制字符串
     */
    public static String toUnicodeHex(String s) {
        if (s == null || s.isEmpty()) return "";
        return toHex(s, StandardCharsets.UTF_16BE);
    }

    /**
     * 将序列号规范化为 6 位字符串，不足左侧补零；如果为 null 返回 "000000"。
     *
     * @param seq 原始流水号（可能是数字或字符串）
     * @return 6 位流水号字符串
     */
    public static String normalizeSeq(String seq) {
        if (seq == null) return "000000";
        String s = seq.trim();
        if (s.matches("\\d+")) {
            // 只有数字则补零
            return String.format("%06d", Long.parseLong(s));
        }
        // 如果已有字符长度大于6，则截断为右侧6位
        if (s.length() > 6) return s.substring(s.length() - 6);
        // 否则左侧补零
        return String.format("%6s", s).replace(' ', '0');
    }

    /**
     * 占位转换：协议中未填写的字段需要保留位置但可为空，这里确保不会返回 null
     */
    private static String emptyToPlaceholder(String s) {
        return s == null ? "" : s;
    }

}
