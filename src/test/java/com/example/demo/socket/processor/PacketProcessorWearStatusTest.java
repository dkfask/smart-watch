package com.example.demo.socket.processor;

import com.example.demo.model.HeartbeatRecord;
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
import com.example.demo.socket.protocol.BraceletPacket;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PacketProcessorWearStatusTest {

    @Test
    void apwrPayloadWithoutImeiSavesWearFlagForSocketImei() {
        HeartbeatRecordRepository heartbeatRecordRepository = mock(HeartbeatRecordRepository.class);
        when(heartbeatRecordRepository.save(any(HeartbeatRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        PacketProcessor processor = new PacketProcessor(
                mock(DeviceRepository.class),
                mock(DownlinkManager.class),
                mock(LocationRecordRepository.class),
                heartbeatRecordRepository,
                mock(HealthRecordRepository.class),
                mock(PatientDeviceRepository.class),
                mock(DeviceStatusRepository.class),
                mock(TiandituLocationService.class),
                mock(HealthMonitorService.class),
                mock(TrackingService.class)
        );

        BraceletPacket packet = new BraceletPacket() {};
        packet.setProtocol("APWR");

        processor.processPacket(packet, "IWAPWR,0,2026-07-09 09:00:00#", "test-client", null, "359999000000001");

        ArgumentCaptor<HeartbeatRecord> captor = ArgumentCaptor.forClass(HeartbeatRecord.class);
        verify(heartbeatRecordRepository).save(captor.capture());
        HeartbeatRecord record = captor.getValue();
        assertThat(record.getImei()).isEqualTo("359999000000001");
        assertThat(record.getRawPayload()).contains("wear_flag=0");
    }
}
