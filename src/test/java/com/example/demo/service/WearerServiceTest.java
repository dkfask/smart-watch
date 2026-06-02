package com.example.demo.service;

import com.example.demo.model.Wearer;
import com.example.demo.model.dto.PageResponse;
import com.example.demo.model.dto.WearerDto;
import com.example.demo.repository.WearerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WearerServiceTest {

    @Mock
    private WearerRepository wearerRepository;

    private WearerService wearerService;

    @BeforeEach
    void setUp() {
        wearerService = new WearerService(wearerRepository);
    }

    private Wearer makeWearer(Long id, String name, String phone, Long deviceId) {
        Wearer w = new Wearer();
        w.setId(id);
        w.setName(name);
        w.setPhone(phone);
        w.setDeviceId(deviceId);
        return w;
    }

    @Test
    void getWearers_returnsPageResponse() {
        Wearer w = makeWearer(1L, "张三", "13800000001", null);
        Page<Wearer> page = new PageImpl<>(List.of(w));
        when(wearerRepository.findAll(any(Pageable.class))).thenReturn(page);

        PageResponse<WearerDto> result = wearerService.getWearers(0, 20);

        assertEquals(1, result.getContent().size());
        assertEquals("张三", result.getContent().get(0).getName());
    }

    @Test
    void getWearer_found_returnsDto() {
        Wearer w = makeWearer(1L, "李四", "13800000002", 10L);
        when(wearerRepository.findById(1L)).thenReturn(Optional.of(w));

        WearerDto dto = wearerService.getWearer(1L);

        assertEquals(1L, dto.getId());
        assertEquals("李四", dto.getName());
        assertEquals(10L, dto.getDeviceId());
    }

    @Test
    void getWearer_notFound_throws404() {
        when(wearerRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> wearerService.getWearer(99L));
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void createWearer_savesAndReturnsDto() {
        Wearer saved = makeWearer(5L, "王五", "13900000001", null);
        when(wearerRepository.save(any(Wearer.class))).thenReturn(saved);

        Map<String, Object> body = Map.of("name", "王五", "phone", "13900000001");
        WearerDto dto = wearerService.createWearer(body);

        assertEquals(5L, dto.getId());
        assertEquals("王五", dto.getName());
        verify(wearerRepository).save(any(Wearer.class));
    }

    @Test
    void updateWearer_found_updatesFields() {
        Wearer existing = makeWearer(1L, "旧名", "111", null);
        Wearer saved = makeWearer(1L, "新名", "222", null);
        when(wearerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(wearerRepository.save(any(Wearer.class))).thenReturn(saved);

        WearerDto dto = wearerService.updateWearer(1L, Map.of("name", "新名", "phone", "222"));

        assertEquals("新名", dto.getName());
    }

    @Test
    void updateWearer_notFound_throws404() {
        when(wearerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> wearerService.updateWearer(99L, Map.of()));
    }

    @Test
    void deleteWearer_found_deletes() {
        when(wearerRepository.existsById(1L)).thenReturn(true);

        wearerService.deleteWearer(1L);

        verify(wearerRepository).deleteById(1L);
    }

    @Test
    void deleteWearer_notFound_throws404() {
        when(wearerRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResponseStatusException.class, () -> wearerService.deleteWearer(99L));
    }

    @Test
    void assignDevice_setsDeviceId() {
        Wearer existing = makeWearer(1L, "赵六", "000", null);
        Wearer saved = makeWearer(1L, "赵六", "000", 7L);
        when(wearerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(wearerRepository.save(any(Wearer.class))).thenReturn(saved);

        WearerDto dto = wearerService.assignDevice(1L, 7L);

        assertEquals(7L, dto.getDeviceId());
    }

    @Test
    void unassignDevice_clearsDeviceId() {
        Wearer existing = makeWearer(1L, "赵六", "000", 7L);
        Wearer saved = makeWearer(1L, "赵六", "000", null);
        when(wearerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(wearerRepository.save(any(Wearer.class))).thenReturn(saved);

        WearerDto dto = wearerService.unassignDevice(1L);

        assertNull(dto.getDeviceId());
    }

    @Test
    void getWearerByDeviceId_found_returnsDto() {
        Wearer w = makeWearer(2L, "钱七", "999", 10L);
        when(wearerRepository.findByDeviceId(10L)).thenReturn(Optional.of(w));

        WearerDto dto = wearerService.getWearerByDeviceId(10L);

        assertEquals(2L, dto.getId());
        assertEquals(10L, dto.getDeviceId());
    }

    @Test
    void getWearerByDeviceId_notFound_throws404() {
        when(wearerRepository.findByDeviceId(99L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> wearerService.getWearerByDeviceId(99L));
    }
}
