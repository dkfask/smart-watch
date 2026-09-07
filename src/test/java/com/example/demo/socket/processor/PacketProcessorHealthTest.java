package com.example.demo.socket.processor;

import com.example.demo.model.Device;
import com.example.demo.model.HealthRecord;
import com.example.demo.model.PatientDevice;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PacketProcessorHealthTest {

    @Test
    void apjkFrameWithLeadingCommaPayloadSavesTypedHealthRecord() {
        HealthRecordRepository healthRecordRepository = mock(HealthRecordRepository.class);
        PacketProcessor processor = newProcessor(healthRecordRepository);

        processor.processPacket(
                packet("APJK"),
                "IWAPJK,2026-06-05 23:33:19,2,79#",
                "test-client",
                null,
                "359999000000001"
        );

        ArgumentCaptor<HealthRecord> captor = ArgumentCaptor.forClass(HealthRecord.class);
        verify(healthRecordRepository).save(captor.capture());
        HealthRecord record = captor.getValue();
        assertThat(record.getDataType()).isEqualTo("heart_rate");
        assertThat(record.getValue()).isEqualTo("79");
        assertThat(record.getDeviceId()).isEqualTo(59L);
        assertThat(record.getPatientId()).isEqualTo(40L);
    }

    @Test
    void aptpFrameWithLeadingCommaPayloadSavesBodyTemperature() {
        HealthRecordRepository healthRecordRepository = mock(HealthRecordRepository.class);
        PacketProcessor processor = newProcessor(healthRecordRepository);

        processor.processPacket(
                packet("APTP"),
                "IWAPTP,37.3,32.3#",
                "test-client",
                null,
                "359999000000001"
        );

        ArgumentCaptor<HealthRecord> captor = ArgumentCaptor.forClass(HealthRecord.class);
        verify(healthRecordRepository).save(captor.capture());
        HealthRecord record = captor.getValue();
        assertThat(record.getDataType()).isEqualTo("body_temperature");
        assertThat(record.getValue()).isEqualTo("37.3");
    }

    @Test
    void apjkBloodPressurePayloadSavesStructuredRecord() {
        HealthRecordRepository healthRecordRepository = mock(HealthRecordRepository.class);
        PacketProcessor processor = newProcessor(healthRecordRepository);

        processor.processPacket(
                packet("APJK"),
                "IWAPJK,2026-06-05 23:33:19,1,75|120#",
                "test-client",
                null,
                "359999000000001"
        );

        ArgumentCaptor<HealthRecord> captor = ArgumentCaptor.forClass(HealthRecord.class);
        verify(healthRecordRepository).save(captor.capture());
        HealthRecord record = captor.getValue();
        assertThat(record.getDataType()).isEqualTo("blood_pressure");
        assertThat(record.getValue()).isEqualTo("75|120");
    }

    @Test
    void apjkSpo2PayloadSavesBloodOxygenRecord() {
        HealthRecordRepository healthRecordRepository = mock(HealthRecordRepository.class);
        PacketProcessor processor = newProcessor(healthRecordRepository);

        processor.processPacket(
                packet("APJK"),
                "IWAPJK,2026-06-05 23:33:19,4,98#",
                "test-client",
                null,
                "359999000000001"
        );

        ArgumentCaptor<HealthRecord> captor = ArgumentCaptor.forClass(HealthRecord.class);
        verify(healthRecordRepository).save(captor.capture());
        HealthRecord record = captor.getValue();
        assertThat(record.getDataType()).isEqualTo("blood_oxygen");
        assertThat(record.getValue()).isEqualTo("98");
    }

    @Test
    void ap02TimestampTypeValuePayloadSavesHeartRateRecord() {
        HealthRecordRepository healthRecordRepository = mock(HealthRecordRepository.class);
        PacketProcessor processor = newProcessor(healthRecordRepository);

        processor.processPacket(
                packet("AP02"),
                "IWAP02,2026-06-05 23:33:19,2,79#",
                "test-client",
                null,
                "359999000000001"
        );

        ArgumentCaptor<HealthRecord> captor = ArgumentCaptor.forClass(HealthRecord.class);
        verify(healthRecordRepository).save(captor.capture());
        HealthRecord record = captor.getValue();
        assertThat(record.getDataType()).isEqualTo("heart_rate");
        assertThat(record.getValue()).isEqualTo("79");
    }

    @Test
    void ap02UnknownPayloadFallsBackToUnknown() {
        HealthRecordRepository healthRecordRepository = mock(HealthRecordRepository.class);
        PacketProcessor processor = newProcessor(healthRecordRepository);

        processor.processPacket(
                packet("AP02"),
                "IWAP02,garbled-payload#",
                "test-client",
                null,
                "359999000000001"
        );

        ArgumentCaptor<HealthRecord> captor = ArgumentCaptor.forClass(HealthRecord.class);
        verify(healthRecordRepository).save(captor.capture());
        HealthRecord record = captor.getValue();
        assertThat(record.getDataType()).isEqualTo("unknown");
        assertThat(record.getValue()).isEqualTo("garbled-payload");
    }

    private static PacketProcessor newProcessor(HealthRecordRepository healthRecordRepository) {
        DeviceRepository deviceRepository = mock(DeviceRepository.class);
        PatientDeviceRepository patientDeviceRepository = mock(PatientDeviceRepository.class);

        Device device = new Device();
        device.setId(59L);
        device.setImei("359999000000001");
        when(deviceRepository.findByImei("359999000000001")).thenReturn(Optional.of(device));

        PatientDevice binding = new PatientDevice();
        binding.setPatientId(40L);
        binding.setDeviceId(59L);
        binding.setIsActive(true);
        when(patientDeviceRepository.findByDeviceId(59L)).thenReturn(List.of(binding));
        when(healthRecordRepository.save(any(HealthRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        return new PacketProcessor(
                deviceRepository,
                mock(DownlinkManager.class),
                mock(LocationRecordRepository.class),
                mock(HeartbeatRecordRepository.class),
                healthRecordRepository,
                patientDeviceRepository,
                mock(DeviceStatusRepository.class),
                mock(TiandituLocationService.class),
                mock(HealthMonitorService.class),
                mock(TrackingService.class)
        );
    }

    private static BraceletPacket packet(String protocol) {
        BraceletPacket packet = new BraceletPacket() {};
        packet.setProtocol(protocol);
        return packet;
    }
}
