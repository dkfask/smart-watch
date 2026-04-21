package com.example.demo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AmapLocationService单元测试
 */
class AmapLocationServiceTest {

    private AmapLocationService serviceWithEmptyKey;
    private AmapLocationService serviceWithNullKey;
    private AmapLocationService serviceWithKey;
    private RestTemplate mockRestTemplate;

    @BeforeEach
    void setUp() {
        serviceWithEmptyKey = new AmapLocationService("");
        serviceWithNullKey = new AmapLocationService(null);

        // 创建带有效key的service，注入mock RestTemplate
        serviceWithKey = new AmapLocationService("test-amap-key");
        mockRestTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(serviceWithKey, "restTemplate", mockRestTemplate);
    }

    // === regeoAddress 测试 ===

    /**
     * webKey为空时regeoAddress返回null
     */
    @Test
    void regeoAddress_emptyKey_returnsNull() {
        assertNull(serviceWithEmptyKey.regeoAddress(39.9087, 116.3975));
        assertNull(serviceWithNullKey.regeoAddress(39.9087, 116.3975));
    }

    /**
     * regeoAddress API返回成功状态时返回地址
     */
    @SuppressWarnings("unchecked")
    @Test
    void regeoAddress_success_returnsAddress() {
        Map<String, Object> regeocode = new HashMap<>();
        regeocode.put("formatted_address", "北京市东城区天安门");

        Map<String, Object> body = new HashMap<>();
        body.put("status", "1");
        body.put("regeocode", regeocode);

        when(mockRestTemplate.getForEntity(any(), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));

        String result = serviceWithKey.regeoAddress(39.9087, 116.3975);

        assertEquals("北京市东城区天安门", result);
    }

    /**
     * regeoAddress API返回非1状态时返回null
     */
    @SuppressWarnings("unchecked")
    @Test
    void regeoAddress_apiReturnsNon1Status_returnsNull() {
        Map<String, Object> body = new HashMap<>();
        body.put("status", "0");

        when(mockRestTemplate.getForEntity(any(), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));

        assertNull(serviceWithKey.regeoAddress(39.9087, 116.3975));
    }

    /**
     * regeoAddress API返回非2xx时返回null
     */
    @SuppressWarnings("unchecked")
    @Test
    void regeoAddress_apiReturnsNon2xx_returnsNull() {
        when(mockRestTemplate.getForEntity(any(), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));

        assertNull(serviceWithKey.regeoAddress(39.9087, 116.3975));
    }

    /**
     * regeoAddress API抛出异常时返回null
     */
    @Test
    void regeoAddress_apiThrowsException_returnsNull() {
        when(mockRestTemplate.getForEntity(any(), eq(Map.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        assertNull(serviceWithKey.regeoAddress(39.9087, 116.3975));
    }

    /**
     * regeoAddress API返回null body时返回null
     */
    @SuppressWarnings("unchecked")
    @Test
    void regeoAddress_nullBody_returnsNull() {
        when(mockRestTemplate.getForEntity(any(), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        assertNull(serviceWithKey.regeoAddress(39.9087, 116.3975));
    }

    // === addressToLocation 测试 ===

    /**
     * webKey为空时addressToLocation返回null
     */
    @Test
    void addressToLocation_emptyKey_returnsNull() {
        assertNull(serviceWithEmptyKey.addressToLocation("北京市东城区"));
        assertNull(serviceWithNullKey.addressToLocation("北京市东城区"));
    }

    /**
     * address为空时addressToLocation返回null
     */
    @Test
    void addressToLocation_emptyAddress_returnsNull() {
        assertNull(serviceWithKey.addressToLocation(""));
        assertNull(serviceWithKey.addressToLocation(null));
    }

    /**
     * addressToLocation API返回成功时返回经纬度
     */
    @SuppressWarnings("unchecked")
    @Test
    void addressToLocation_success_returnsLocation() {
        Map<String, Object> geocode = new HashMap<>();
        geocode.put("location", "116.3975,39.9087");

        Map<String, Object> body = new HashMap<>();
        body.put("status", "1");
        body.put("geocodes", List.of(geocode));

        when(mockRestTemplate.getForEntity(any(), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));

        Map<String, Double> result = serviceWithKey.addressToLocation("北京市东城区天安门");

        assertNotNull(result);
        assertEquals(116.3975, result.get("lng"), 0.001);
        assertEquals(39.9087, result.get("lat"), 0.001);
    }

    /**
     * addressToLocation API返回空geocodes时返回null
     */
    @SuppressWarnings("unchecked")
    @Test
    void addressToLocation_emptyGeocodes_returnsNull() {
        Map<String, Object> body = new HashMap<>();
        body.put("status", "1");
        body.put("geocodes", List.of());

        when(mockRestTemplate.getForEntity(any(), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));

        assertNull(serviceWithKey.addressToLocation("不存在的地方"));
    }

    /**
     * addressToLocation API返回非1状态时返回null
     */
    @SuppressWarnings("unchecked")
    @Test
    void addressToLocation_apiReturnsNon1Status_returnsNull() {
        Map<String, Object> body = new HashMap<>();
        body.put("status", "0");

        when(mockRestTemplate.getForEntity(any(), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));

        assertNull(serviceWithKey.addressToLocation("北京市"));
    }

    /**
     * addressToLocation API抛出异常时返回null
     */
    @Test
    void addressToLocation_apiThrowsException_returnsNull() {
        when(mockRestTemplate.getForEntity(any(), eq(Map.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        assertNull(serviceWithKey.addressToLocation("北京市"));
    }

    // === searchPoi 测试 ===

    /**
     * webKey为空时searchPoi返回null
     */
    @Test
    void searchPoi_emptyKey_returnsNull() {
        assertNull(serviceWithEmptyKey.searchPoi("医院", null, null, null));
        assertNull(serviceWithNullKey.searchPoi("医院", null, null, null));
    }

    /**
     * keyword为空时searchPoi返回null
     */
    @Test
    void searchPoi_emptyKeyword_returnsNull() {
        assertNull(serviceWithKey.searchPoi("", null, null, null));
        assertNull(serviceWithKey.searchPoi(null, null, null, null));
    }

    /**
     * searchPoi API返回成功时返回POI列表
     */
    @SuppressWarnings("unchecked")
    @Test
    void searchPoi_success_returnsPoiList() {
        Map<String, Object> poi = new HashMap<>();
        poi.put("name", "北京协和医院");
        poi.put("address", "北京市东城区帅府园1号");
        poi.put("tel", "010-69156699");
        poi.put("type", "医疗保健服务;综合医院");
        poi.put("location", "116.4181,39.9126");

        Map<String, Object> body = new HashMap<>();
        body.put("status", "1");
        body.put("pois", List.of(poi));

        when(mockRestTemplate.getForEntity(any(), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));

        List<Map<String, Object>> result = serviceWithKey.searchPoi("协和医院", "北京", 10, 1);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("北京协和医院", result.get(0).get("name"));
        assertNotNull(result.get(0).get("location"));
    }

    /**
     * searchPoi API返回非1状态时返回null
     */
    @SuppressWarnings("unchecked")
    @Test
    void searchPoi_apiReturnsNon1Status_returnsNull() {
        Map<String, Object> body = new HashMap<>();
        body.put("status", "0");

        when(mockRestTemplate.getForEntity(any(), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));

        assertNull(serviceWithKey.searchPoi("医院", null, null, null));
    }

    /**
     * searchPoi API抛出异常时返回null
     */
    @Test
    void searchPoi_apiThrowsException_returnsNull() {
        when(mockRestTemplate.getForEntity(any(), eq(Map.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        assertNull(serviceWithKey.searchPoi("医院", null, null, null));
    }
}
