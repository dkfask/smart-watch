package com.example.demo.socket;

import com.example.demo.repository.DeviceRepository;
import com.example.demo.repository.DeviceStatusRepository;
import com.example.demo.repository.HealthRecordRepository;
import com.example.demo.repository.HeartbeatRecordRepository;
import com.example.demo.repository.LocationRecordRepository;
import com.example.demo.repository.PatientDeviceRepository;
import com.example.demo.service.HealthMonitorService;
import com.example.demo.service.TiandituLocationService;
import com.example.demo.service.TrackingService;
import com.example.demo.socket.downlink.DownlinkManager;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MpbandServerImeiExtractionTest {

    @Test
    void ap01StatusBlockDigitsAreNotTreatedAsDeviceImei() throws Exception {
        MpbandServer server = newServer();
        String raw = "IWAP01260708A3052.1533N10335.5094E001.5181202330.3806700601500008,"
                + "460,04,32768,217820546,AP1|d4:24:93:2a:1e:c0|-58&AP2|d4:24:93:29:ba:b0|-60#";

        assertThat(extractImei(server, raw)).isNull();
    }

    @Test
    void ap00LoginStillExtractsValidDeviceImei() throws Exception {
        MpbandServer server = newServer();

        assertThat(extractImei(server, "IWAP00355932600124999#")).isEqualTo("355932600124999");
    }

    private static String extractImei(MpbandServer server, String raw) throws Exception {
        Method method = MpbandServer.class.getDeclaredMethod("getImeiFromRaw", String.class);
        method.setAccessible(true);
        return (String) method.invoke(server, raw);
    }

    private static MpbandServer newServer() {
        return new MpbandServer(
                mock(DeviceRepository.class),
                mock(DownlinkManager.class),
                mock(LocationRecordRepository.class),
                mock(HeartbeatRecordRepository.class),
                mock(HealthRecordRepository.class),
                mock(PatientDeviceRepository.class),
                mock(DeviceStatusRepository.class),
                mock(TiandituLocationService.class),
                mock(HealthMonitorService.class),
                mock(TrackingService.class)
        );
    }
}
