package com.example.demo.service;

import com.example.demo.model.FenceDeviceState;
import com.example.demo.model.FenceEvent;
import com.example.demo.model.FenceEvent.EventType;
import com.example.demo.model.FenceGeometry;
import com.example.demo.model.FenceGeometry.GeometryType;
import com.example.demo.model.FencePolicy;
import com.example.demo.model.GeoFence;
import com.example.demo.model.PatientDevice;
import com.example.demo.repository.FenceDeviceStateRepository;
import com.example.demo.repository.FenceEventRepository;
import com.example.demo.repository.FenceGeometryRepository;
import com.example.demo.repository.FencePolicyRepository;
import com.example.demo.repository.FenceSubscriptionRepository;
import com.example.demo.repository.GeoFenceRepository;
import com.example.demo.repository.PatientDeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link FenceEngine} state machine.
 *
 * <p>Covers the four key transitions (enter / exit / debounce / dwell)
 * and the cooldown suppression path. Uses Mockito to stub the seven
 * repositories the engine depends on — no Spring context, no MySQL.
 */
class FenceEngineTest {

    // Subject under test
    private FenceEngine engine;

    // Collaborators
    private GeoFenceRepository fenceRepo;
    private FenceGeometryRepository geometryRepo;
    private FencePolicyRepository policyRepo;
    private FenceEventRepository eventRepo;
    private FenceDeviceStateRepository stateRepo;
    private FenceSubscriptionRepository subscriptionRepo;
    private PatientDeviceRepository patientDeviceRepo;

    private static final long DEVICE_ID = 100L;
    private static final long PATIENT_ID = 7L;
    private static final long FENCE_ID  = 42L;

    @BeforeEach
    void setUp() {
        fenceRepo            = mock(GeoFenceRepository.class);
        geometryRepo         = mock(FenceGeometryRepository.class);
        policyRepo           = mock(FencePolicyRepository.class);
        eventRepo            = mock(FenceEventRepository.class);
        stateRepo            = mock(FenceDeviceStateRepository.class);
        subscriptionRepo     = mock(FenceSubscriptionRepository.class);
        patientDeviceRepo    = mock(PatientDeviceRepository.class);
        engine = new FenceEngine(fenceRepo, geometryRepo, policyRepo,
                                 eventRepo, stateRepo, subscriptionRepo,
                                 patientDeviceRepo);
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private GeoFence circleFence() {
        GeoFence f = new GeoFence();
        f.setId(FENCE_ID);
        f.setType("circle");
        f.setStatus("active");
        return f;
    }

    private FenceGeometry circleGeom() {
        FenceGeometry g = new FenceGeometry();
        g.setFenceId(FENCE_ID);
        g.setGeometryType(GeometryType.CIRCLE);
        g.setCenterLat(39.9087);
        g.setCenterLng(116.3975);
        g.setRadiusM(500);
        return g;
    }

    /** Default policy: 3-debounce, 300 s cooldown, no dwell. */
    private FencePolicy defaultPolicy() {
        FencePolicy p = new FencePolicy();
        p.setFenceId(FENCE_ID);
        p.setEnterAlert(true);
        p.setExitAlert(true);
        p.setDwellAlert(false);
        p.setDwellSeconds(300);
        p.setDebounceCount(3);
        p.setCooldownSec(300);
        return p;
    }

    private PatientDevice patientBinding() {
        PatientDevice pd = new PatientDevice();
        pd.setPatientId(PATIENT_ID);
        pd.setDeviceId(DEVICE_ID);
        return pd;
    }

    private FenceDeviceState noState() {
        return null;
    }

    private FenceDeviceState outsideState(int outsideCount) {
        FenceDeviceState s = new FenceDeviceState();
        s.setFenceId(FENCE_ID);
        s.setDeviceId(DEVICE_ID);
        s.setIsInside(false);
        s.setLastChangedAt(new java.util.Date(0));
        s.setOutsideCount(outsideCount);
        s.setLastLat(40.0);
        s.setLastLng(116.5);
        return s;
    }

    private FenceDeviceState insideState(java.util.Date insideSince) {
        FenceDeviceState s = new FenceDeviceState();
        s.setFenceId(FENCE_ID);
        s.setDeviceId(DEVICE_ID);
        s.setIsInside(true);
        s.setLastChangedAt(insideSince);
        s.setInsideSince(insideSince);
        s.setLastLat(39.9087);
        s.setLastLng(116.3975);
        s.setOutsideCount(0);
        return s;
    }

    private void primeForEnter() {
        when(patientDeviceRepo.findByDeviceId(DEVICE_ID))
                .thenReturn(List.of(patientBinding()));
        when(fenceRepo.listByPatient(PATIENT_ID))
                .thenReturn(List.of(circleFence()));
        when(fenceRepo.listActiveByFencePatients(PATIENT_ID))
                .thenReturn(List.of());
        when(geometryRepo.findByFenceId(FENCE_ID)).thenReturn(circleGeom());
        when(policyRepo.effectivePolicyFor(FENCE_ID)).thenReturn(defaultPolicy());
    }

    // =====================================================================
    // Tests
    // =====================================================================

    @Test
    void firstContact_initialisesState_doesNotEmit() {
        primeForEnter();
        when(stateRepo.findOne(FENCE_ID, DEVICE_ID)).thenReturn(noState());

        engine.onLocationReport(DEVICE_ID, 39.9087, 116.3975);  // centre

        verify(stateRepo).upsert(any(FenceDeviceState.class));
        verify(eventRepo, never()).insert(any(FenceEvent.class));
    }

    @Test
    void outsideToInside_emitsEnterEvent() {
        primeForEnter();
        when(stateRepo.findOne(FENCE_ID, DEVICE_ID))
                .thenReturn(outsideState(3));   // already over debounce threshold

        engine.onLocationReport(DEVICE_ID, 39.9087, 116.3975);  // centre

        ArgumentCaptor<FenceEvent> cap = ArgumentCaptor.forClass(FenceEvent.class);
        verify(eventRepo).insert(cap.capture());
        FenceEvent ev = cap.getValue();
        assertEquals(EventType.ENTER, ev.getEventType());
        assertEquals(FENCE_ID, ev.getFenceId());
        assertEquals(DEVICE_ID, ev.getDeviceId());
        assertEquals(PATIENT_ID, ev.getPatientId());
        assertEquals(FenceEvent.AckStatus.NEW, ev.getAckStatus());
        verify(stateRepo, atLeastOnce()).upsert(any(FenceDeviceState.class));
    }

    @Test
    void insideToOutside_emitsExitEvent() {
        primeForEnter();
        when(stateRepo.findOne(FENCE_ID, DEVICE_ID))
                .thenReturn(outsideState(2));   // 1 hit from going out
        when(eventRepo.findLatestByFenceDevice(FENCE_ID, DEVICE_ID))
                .thenReturn(null);

        // 1st outside hit → debounce counter to 3 → commit, no event yet
        engine.onLocationReport(DEVICE_ID, 40.0, 116.5);
        verify(eventRepo, never()).insert(any(FenceEvent.class));

        // Reset mock so we can assert on the second call only.
        reset(stateRepo, eventRepo);
        when(stateRepo.findOne(FENCE_ID, DEVICE_ID))
                .thenReturn(outsideState(3));

        engine.onLocationReport(DEVICE_ID, 40.0, 116.5);
        ArgumentCaptor<FenceEvent> cap = ArgumentCaptor.forClass(FenceEvent.class);
        verify(eventRepo).insert(cap.capture());
        assertEquals(EventType.EXIT, cap.getValue().getEventType());
    }

    @Test
    void debounceSuppressesTransition() {
        primeForEnter();
        when(stateRepo.findOne(FENCE_ID, DEVICE_ID))
                .thenReturn(outsideState(1));   // only 1 outside hit so far

        engine.onLocationReport(DEVICE_ID, 40.0, 116.5);

        verify(eventRepo, never()).insert(any(FenceEvent.class));
        // State should still be persisted (provisional) with bumped counter.
        ArgumentCaptor<FenceDeviceState> cap =
                ArgumentCaptor.forClass(FenceDeviceState.class);
        verify(stateRepo).upsert(cap.capture());
        assertEquals(2, cap.getValue().getOutsideCount());
    }

    @Test
    void cooldownSuppressesDuplicateEvent() {
        primeForEnter();
        when(stateRepo.findOne(FENCE_ID, DEVICE_ID))
                .thenReturn(outsideState(3));
        // Pretend we just emitted an EXIT 10 seconds ago.
        FenceEvent recent = new FenceEvent();
        recent.setEventType(EventType.EXIT);
        recent.setOccurredAt(new java.util.Date(
                System.currentTimeMillis() - 10_000L));
        when(eventRepo.findLatestByFenceDevice(FENCE_ID, DEVICE_ID))
                .thenReturn(recent);

        engine.onLocationReport(DEVICE_ID, 39.9087, 116.3975);

        verify(eventRepo, never()).insert(any(FenceEvent.class));
        // State still committed.
        verify(stateRepo, atLeastOnce()).upsert(any(FenceDeviceState.class));
    }

    @Test
    void dwell_emitsWhenInsideLongEnough() {
        FencePolicy dwellPolicy = defaultPolicy();
        dwellPolicy.setDwellAlert(true);
        dwellPolicy.setDwellSeconds(60);

        primeForEnter();
        when(policyRepo.effectivePolicyFor(FENCE_ID)).thenReturn(dwellPolicy);

        // Inside state, inside_since 5 minutes ago → trigger dwell.
        java.util.Date fiveMinAgo = new java.util.Date(
                System.currentTimeMillis() - 5 * 60 * 1000L);
        when(stateRepo.findOne(FENCE_ID, DEVICE_ID))
                .thenReturn(insideState(fiveMinAgo));
        when(eventRepo.findLatestByFenceDevice(FENCE_ID, DEVICE_ID))
                .thenReturn(null);

        engine.onLocationReport(DEVICE_ID, 39.9087, 116.3975);

        ArgumentCaptor<FenceEvent> cap = ArgumentCaptor.forClass(FenceEvent.class);
        verify(eventRepo).insert(cap.capture());
        assertEquals(EventType.DWELL, cap.getValue().getEventType());
        assertNotNull(cap.getValue().getDwellSeconds());
        assertTrue(cap.getValue().getDwellSeconds() >= 300,
                "dwell seconds should be ~300 (5 min)");
    }

    @Test
    void dwell_doesNotEmitWhenNotInsideLongEnough() {
        FencePolicy dwellPolicy = defaultPolicy();
        dwellPolicy.setDwellAlert(true);
        dwellPolicy.setDwellSeconds(600);   // 10 min

        primeForEnter();
        when(policyRepo.effectivePolicyFor(FENCE_ID)).thenReturn(dwellPolicy);

        // Inside state, inside_since 30 seconds ago → too soon.
        java.util.Date thirtySecAgo = new java.util.Date(
                System.currentTimeMillis() - 30 * 1000L);
        when(stateRepo.findOne(FENCE_ID, DEVICE_ID))
                .thenReturn(insideState(thirtySecAgo));

        engine.onLocationReport(DEVICE_ID, 39.9087, 116.3975);

        verify(eventRepo, never()).insert(any(FenceEvent.class));
    }

    @Test
    void dwell_doesNotReEmitIfAlreadyEmittedThisStay() {
        FencePolicy dwellPolicy = defaultPolicy();
        dwellPolicy.setDwellAlert(true);
        dwellPolicy.setDwellSeconds(60);

        primeForEnter();
        when(policyRepo.effectivePolicyFor(FENCE_ID)).thenReturn(dwellPolicy);

        java.util.Date fiveMinAgo = new java.util.Date(
                System.currentTimeMillis() - 5 * 60 * 1000L);
        when(stateRepo.findOne(FENCE_ID, DEVICE_ID))
                .thenReturn(insideState(fiveMinAgo));
        // Pretend DWELL was already emitted 1 minute ago (after inside_since).
        FenceEvent priorDwell = new FenceEvent();
        priorDwell.setEventType(EventType.DWELL);
        priorDwell.setOccurredAt(new java.util.Date(
                System.currentTimeMillis() - 60 * 1000L));
        when(eventRepo.findLatestByFenceDevice(FENCE_ID, DEVICE_ID))
                .thenReturn(priorDwell);

        engine.onLocationReport(DEVICE_ID, 39.9087, 116.3975);

        verify(eventRepo, never()).insert(any(FenceEvent.class));
    }

    @Test
    void multiplePatients_eachCheckedIndependently() {
        PatientDevice pd1 = new PatientDevice();
        pd1.setPatientId(1L);
        pd1.setDeviceId(DEVICE_ID);
        PatientDevice pd2 = new PatientDevice();
        pd2.setPatientId(2L);
        pd2.setDeviceId(DEVICE_ID);

        GeoFence fenceA = new GeoFence();
        fenceA.setId(11L); fenceA.setType("circle"); fenceA.setStatus("active");
        GeoFence fenceB = new GeoFence();
        fenceB.setId(22L); fenceB.setType("circle"); fenceB.setStatus("active");

        when(patientDeviceRepo.findByDeviceId(DEVICE_ID))
                .thenReturn(List.of(pd1, pd2));
        when(fenceRepo.listByPatient(1L)).thenReturn(List.of(fenceA));
        when(fenceRepo.listActiveByFencePatients(1L)).thenReturn(List.of());
        when(fenceRepo.listByPatient(2L)).thenReturn(List.of(fenceB));
        when(fenceRepo.listActiveByFencePatients(2L)).thenReturn(List.of());

        FenceGeometry gA = new FenceGeometry();
        gA.setFenceId(11L); gA.setGeometryType(GeometryType.CIRCLE);
        gA.setCenterLat(39.9); gA.setCenterLng(116.4); gA.setRadiusM(100);
        FenceGeometry gB = new FenceGeometry();
        gB.setFenceId(22L); gB.setGeometryType(GeometryType.CIRCLE);
        gB.setCenterLat(40.0); gB.setCenterLng(117.0); gB.setRadiusM(100);

        when(geometryRepo.findByFenceId(11L)).thenReturn(gA);
        when(geometryRepo.findByFenceId(22L)).thenReturn(gB);
        when(policyRepo.effectivePolicyFor(anyLong()))
                .thenReturn(defaultPolicy());
        when(stateRepo.findOne(anyLong(), eq(DEVICE_ID))).thenReturn(noState());

        // Point inside fence A (39.9, 116.4) but outside fence B.
        engine.onLocationReport(DEVICE_ID, 39.9, 116.4);

        // Both fences get an initial-state upsert (first contact).
        verify(stateRepo, times(2)).upsert(any(FenceDeviceState.class));
        verify(eventRepo, never()).insert(any(FenceEvent.class));
    }

    @Test
    void noPatientBinding_doesNothing() {
        when(patientDeviceRepo.findByDeviceId(DEVICE_ID))
                .thenReturn(List.of());

        engine.onLocationReport(DEVICE_ID, 39.9, 116.4);

        verifyNoInteractions(geometryRepo, policyRepo, eventRepo, stateRepo);
    }

    @Test
    void naNCoordinates_areIgnored() {
        engine.onLocationReport(DEVICE_ID, Double.NaN, 116.4);
        engine.onLocationReport(DEVICE_ID, 39.9, Double.NaN);

        verifyNoInteractions(patientDeviceRepo, geometryRepo, eventRepo);
    }

    @Test
    void exceptionInsideCheck_isSwallowed() {
        when(patientDeviceRepo.findByDeviceId(DEVICE_ID))
                .thenThrow(new RuntimeException("db down"));

        // Must not propagate.
        assertDoesNotThrow(() ->
                engine.onLocationReport(DEVICE_ID, 39.9, 116.4));
    }
}
