package com.example.demo.service;

import com.example.demo.model.Wearer;
import com.example.demo.model.dto.PageResponse;
import com.example.demo.model.dto.WearerDto;
import com.example.demo.repository.WearerRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.Map;

@Service
public class WearerService {

    private final WearerRepository wearerRepository;

    public WearerService(WearerRepository wearerRepository) {
        this.wearerRepository = wearerRepository;
    }

    public PageResponse<WearerDto> getWearers(int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.from(wearerRepository.findAll(pageable).map(WearerDto::from));
    }

    public WearerDto getWearer(Long id) {
        return wearerRepository.findById(id)
                .map(WearerDto::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wearer not found: " + id));
    }

    public WearerDto createWearer(Map<String, Object> body) {
        Wearer w = new Wearer();
        w.setName((String) body.get("name"));
        w.setPhone((String) body.get("phone"));
        if (body.get("deviceId") != null) {
            w.setDeviceId(Long.valueOf(body.get("deviceId").toString()));
        }
        return WearerDto.from(wearerRepository.save(w));
    }

    public WearerDto updateWearer(Long id, Map<String, Object> body) {
        Wearer existing = wearerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wearer not found: " + id));
        Wearer updated = new Wearer();
        updated.setId(existing.getId());
        updated.setName(body.get("name") != null ? (String) body.get("name") : existing.getName());
        updated.setPhone(body.get("phone") != null ? (String) body.get("phone") : existing.getPhone());
        updated.setDeviceId(body.get("deviceId") != null
                ? Long.valueOf(body.get("deviceId").toString())
                : existing.getDeviceId());
        updated.setCreatedAt(existing.getCreatedAt());
        updated.setUpdatedAt(new Date());
        return WearerDto.from(wearerRepository.save(updated));
    }

    public void deleteWearer(Long id) {
        if (!wearerRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Wearer not found: " + id);
        }
        wearerRepository.deleteById(id);
    }

    public WearerDto assignDevice(Long wearerId, Long deviceId) {
        Wearer existing = wearerRepository.findById(wearerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wearer not found: " + wearerId));
        Wearer updated = new Wearer();
        updated.setId(existing.getId());
        updated.setName(existing.getName());
        updated.setPhone(existing.getPhone());
        updated.setDeviceId(deviceId);
        updated.setCreatedAt(existing.getCreatedAt());
        updated.setUpdatedAt(new Date());
        return WearerDto.from(wearerRepository.save(updated));
    }

    public WearerDto unassignDevice(Long wearerId) {
        Wearer existing = wearerRepository.findById(wearerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wearer not found: " + wearerId));
        Wearer updated = new Wearer();
        updated.setId(existing.getId());
        updated.setName(existing.getName());
        updated.setPhone(existing.getPhone());
        updated.setDeviceId(null);
        updated.setCreatedAt(existing.getCreatedAt());
        updated.setUpdatedAt(new Date());
        return WearerDto.from(wearerRepository.save(updated));
    }

    public WearerDto getWearerByDeviceId(Long deviceId) {
        return wearerRepository.findByDeviceId(deviceId)
                .map(WearerDto::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No wearer assigned to device: " + deviceId));
    }
}
