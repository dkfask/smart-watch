import com.example.demo.socket.MpbandServer;
import com.example.demo.model.LocationRecord;
import com.example.demo.repository.LocationRecordRepository;
import com.example.demo.repository.DeviceRepository;
import com.example.demo.repository.HeartbeatRecordRepository;
import com.example.demo.repository.HealthRecordRepository;
import com.example.demo.socket.downlink.DownlinkManager;

import java.lang.reflect.Field;
import java.util.*;

public class SaveLocationTest {
    public static void main(String[] args) throws Exception {
        // 创建 MpbandServer 的实例，使用 nulls 或简单替代作为依赖，然后用反射注入一个 mock LocationRecordRepository
        MpbandServer server = createServerWithMocks();

        // 构造 params，包含 wifiGeoLat/wifiGeoLon（且不包含 lat/lon）
        Map<String, String> params = new LinkedHashMap<>();
        params.put("payload", "...");
        params.put("wifiGeoLat", "30.886935");
        params.put("wifiGeoLon", "103.594551");
        params.put("speed", "0");
        params.put("gpsTime", "065617");

        // 调用 saveLocationData via reflection since it's private
        java.lang.reflect.Method m = MpbandServer.class.getDeclaredMethod("saveLocationData", Map.class, String.class);
        m.setAccessible(true);
        m.invoke(server, params, "test-imei-00001");

        // 获取 mock 存储的记录并打印 latitude/longitude
        MockLocationRepo mock = getMockLocationRepo(server);
        if (mock.savedRecords.isEmpty()) {
            System.out.println("No record saved");
            return;
        }
        LocationRecord rec = mock.savedRecords.get(mock.savedRecords.size() - 1);
        System.out.println("Saved LocationRecord: latitude=" + rec.getLatitude() + ", longitude=" + rec.getLongitude());
        System.out.println("ExtraRaw=" + rec.getExtraRaw());
        System.out.println("Imei=" + rec.getImei());
    }

    // helper: create server and inject mock repositories
    private static MpbandServer createServerWithMocks() throws Exception {
        // Create mock dependencies (some repositories aren't used in saveLocationData but constructor requires them)
        DeviceRepository devRepo = null;
        DownlinkManager downlink = null;
        LocationRecordRepository locRepo = new MockLocationRepo();
        HeartbeatRecordRepository hbRepo = null;
        HealthRecordRepository healthRepo = null;

        // Use reflection to find constructor and instantiate
        MpbandServer server = new MpbandServer(devRepo, downlink, locRepo, hbRepo, healthRepo);
        return server;
    }

    private static MockLocationRepo getMockLocationRepo(MpbandServer server) throws Exception {
        Field f = MpbandServer.class.getDeclaredField("locationRecordRepository");
        f.setAccessible(true);
        Object val = f.get(server);
        return (MockLocationRepo) val;
    }

    // a very small mock implementation of LocationRecordRepository that captures saved records
    public static class MockLocationRepo implements LocationRecordRepository {
        public List<LocationRecord> savedRecords = new ArrayList<>();

        @Override
        public <S extends LocationRecord> S save(S entity) {
            savedRecords.add(entity);
            // emulate JPA behavior: return the entity
            return entity;
        }

        // other methods of CrudRepository are unused in this test; implement minimal stubs
        @Override public <S extends LocationRecord> Iterable<S> saveAll(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public Optional<LocationRecord> findById(Long aLong) { return Optional.empty(); }
        @Override public boolean existsById(Long aLong) { return false; }
        @Override public Iterable<LocationRecord> findAll() { return Collections.emptyList(); }
        @Override public Iterable<LocationRecord> findAllById(Iterable<Long> longs) { return Collections.emptyList(); }
        @Override public long count() { return savedRecords.size(); }
        @Override public void deleteById(Long aLong) { }
        @Override public void delete(LocationRecord entity) { }
        @Override public void deleteAllById(Iterable<? extends Long> longs) { }
        @Override public void deleteAll(Iterable<? extends LocationRecord> entities) { }
        @Override public void deleteAll() { savedRecords.clear(); }
    }
}

