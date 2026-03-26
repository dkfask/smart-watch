import api from './axios'
import { handleApiResponse, handleApiError } from './utils'

export const downlinkApi = {
  // 1. 下发BP00指令（设置时区）
  sendBP00(imei, timezone = 8) {
    return api.post(`/downlink/bp00?imei=${imei}&timezone=${timezone}`)
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 2. 下发BP12指令（设置SOS码）
  sendBP12(imei, sosNumbers) {
    return api.post(`/downlink/bp12?imei=${imei}`, { sosNumbers })
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 3. 下发BP14指令（设置联系人白名单）
  sendBP14(imei, names, phones, seq = "1") {
    return api.post(`/downlink/bp14?imei=${imei}`, { names, phones, seq })
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 4. 下发BP15指令（GPS定位数据上传时间间隔）
  sendBP15(imei, interval, seq = "1") {
    return api.post(`/downlink/bp15?imei=${imei}&interval=${interval}&seq=${seq}`)
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 5. 下发BP16指令（立即定位）
  sendBP16(imei, seq = "1") {
    return api.post(`/downlink/bp16?imei=${imei}&seq=${seq}`)
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 6. 下发BP17指令（恢复出厂设置）
  sendBP17(imei, seq = "1") {
    return api.post(`/downlink/bp17?imei=${imei}&seq=${seq}`)
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 7. 下发BP18指令（重启设备）
  sendBP18(imei, seq = "1") {
    return api.post(`/downlink/bp18?imei=${imei}&seq=${seq}`)
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 8. 下发BP19指令（设置服务器信息）
  sendBP19(imei, domainFlag, hostOrIp, port, seq = "1") {
    return api.post(`/downlink/bp19?imei=${imei}&domainFlag=${domainFlag}&hostOrIp=${hostOrIp}&port=${port}&seq=${seq}`)
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 9. 下发BP20指令（设置设备语言与时区）
  sendBP20(imei, language, timezone, seq = "1") {
    return api.post(`/downlink/bp20?imei=${imei}&language=${language}&timezone=${timezone}&seq=${seq}`)
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 10. 下发BP31指令（关机）
  sendBP31(imei, seq = "1") {
    return api.post(`/downlink/bp31?imei=${imei}&seq=${seq}`)
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 11. 下发BP32指令（拨打电话）
  sendBP32(imei, phone, seq = "1") {
    return api.post(`/downlink/bp32?imei=${imei}&phone=${phone}&seq=${seq}`)
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 12. 下发BP33指令（工作模式）
  sendBP33(imei, workMode, seq = "1") {
    return api.post(`/downlink/bp33?imei=${imei}&mode=${workMode}&seq=${seq}`)
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 13. 下发BP34指令（自定义定位模式）
  sendBP34(imei, mode, intervalSec, gpsFlag, seq = "1") {
    return api.post(`/downlink/bp34?imei=${imei}&mode=${mode}&intervalSec=${intervalSec}&gpsFlag=${gpsFlag}&seq=${seq}`)
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 14. 下发BP40指令（快捷指令下发）
  sendBP40(imei, payload, seq = "1") {
    return api.post(`/downlink/bp40?imei=${imei}`, { payload, seq })
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 15. 下发BP46指令（立即拍照）
  sendBP46(imei, cmdValue = 1, param = "", seq = "1") {
    return api.post(`/downlink/bp46?imei=${imei}&cmdValue=${cmdValue}&param=${param}&seq=${seq}`)
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 16. 下发BP50指令（下发心跳检测指令）
  sendBP50(imei, seq = "1") {
    return api.post(`/downlink/bp50?imei=${imei}&seq=${seq}`)
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 17. 下发BP51指令（下发电话本，单条）
  sendBP51(imei, name, phone, seq = "1") {
    return api.post(`/downlink/bp51?imei=${imei}&name=${name}&phone=${phone}&seq=${seq}`)
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 18. 下发BP52指令（删除电话本，单条）
  sendBP52(imei, phone, seq = "1") {
    return api.post(`/downlink/bp52?imei=${imei}&phone=${phone}&seq=${seq}`)
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 19. 下发BP84指令（白名单开关）
  sendBP84(imei, setting, seq = "1") {
    return api.post(`/downlink/bp84?imei=${imei}&setting=${setting}&seq=${seq}`)
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 20. 下发BP86指令（健康监测间隔设置）
  sendBP86(imei, onOff, minutes, seq = "1") {
    return api.post(`/downlink/bp86?imei=${imei}&onOff=${onOff}&minutes=${minutes}&seq=${seq}`)
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 21. 下发BP88指令（寻找设备）
  sendBP88(imei, seq = "1") {
    return api.post(`/downlink/bp88?imei=${imei}&seq=${seq}`)
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 22. 下发BPMC指令（运动检测控制）
  sendBPMC(imei, setting, seq = "1") {
    return api.post(`/downlink/bpmc?imei=${imei}&setting=${setting}&seq=${seq}`)
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 23. 下发BPPH指令（SOS呼叫开关）
  sendBPPH(imei, setting, seq = "1") {
    return api.post(`/downlink/bpph?imei=${imei}&setting=${setting}&seq=${seq}`)
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 24. 下发BPSM指令（短信指令）
  sendBPSM(imei, content, seq = "1") {
    return api.post(`/downlink/bpsm?imei=${imei}`, { content, seq })
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 25. 下发BPTF指令（时间制度）
  sendBPTF(imei, setting) {
    return api.post(`/downlink/bptf?imei=${imei}&setting=${setting}`)
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 26. 下发BPWL指令（设置与设备绑定的联系人白名单）
  sendBPWL(imei, contacts, seq = "1") {
    return api.post(`/downlink/bpwl?imei=${imei}`, { contacts, seq })
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 27. 下发BPXL指令（测量心率）
  sendBPXL(imei, seq = "1") {
    return api.post(`/downlink/bpxl?imei=${imei}&seq=${seq}`)
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 28. 下发BPXY指令（测量血压）
  sendBPXY(imei, seq = "1") {
    return api.post(`/downlink/bpxy?imei=${imei}&seq=${seq}`)
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 29. 下发BPXZ指令（测量血氧）
  sendBPXZ(imei, seq = "1") {
    return api.post(`/downlink/bpxz?imei=${imei}&seq=${seq}`)
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 30. 下发BPXX指令（测量体温）
  sendBPXX(imei, seq = "1") {
    return api.post(`/downlink/bpxx?imei=${imei}&seq=${seq}`)
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 下发短命令
  sendShortCommand(imei, command) {
    return api.post(`/downlink/short-command?imei=${imei}`, { command })
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 获取日志
  getLogs(imei) {
    return api.get(`/downlink/logs?imei=${imei}`)
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 初始配置
  sendInitialConfig(imei, config) {
    return api.post(`/downlink/initial-config?imei=${imei}`, config)
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 发信息（已改为使用BP40协议）
  sendMessage(imei, message) {
    // 兼容旧调用，重定向到sendBP40
    return this.sendBP40(imei, message)
  },

  // 获取电池报告
  getBatteryReport(imei) {
    return api.get(`/downlink/battery-report?imei=${imei}`)
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 设置闹钟
  setAlarm(imei, alarmConfig) {
    return api.post(`/downlink/alarm?imei=${imei}`, alarmConfig)
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 实时追踪
  startRealTimeTracking(imei, interval = 5) {
    return api.post(`/downlink/real-time-tracking?imei=${imei}&interval=${interval}`)
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 停止实时追踪
  stopRealTimeTracking(imei) {
    return api.post(`/downlink/stop-real-time-tracking?imei=${imei}`)
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 获取历史轨迹
  getHistoryTrack(imei, startTime, endTime) {
    return api.get(`/downlink/history-track?imei=${imei}&startTime=${startTime}&endTime=${endTime}`)
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 获取报警日历
  getAlarmCalendar(imei, year, month) {
    return api.get(`/downlink/alarm-calendar?imei=${imei}&year=${year}&month=${month}`)
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 获取原始日志
  getRawLogs(imei, startTime, endTime, page = 1, size = 100, keyword = '') {
    return api.get(`/downlink/raw-logs?imei=${imei}&startTime=${startTime}&endTime=${endTime}&page=${page}&size=${size}&keyword=${encodeURIComponent(keyword)}`)
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 导出状态历史
  exportStatusHistory(imei, startTime, endTime) {
    return api.get(`/downlink/export-status-history?imei=${imei}&startTime=${startTime}&endTime=${endTime}`, {
      responseType: 'blob'
    })
      .then(handleApiResponse)
      .catch(handleApiError)
  }
}
