package com.example.demo.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import com.example.demo.model.Device;

import java.util.List;
import java.util.Optional;

/**
 * DeviceRepository 接口 - 设备数据访问层
 *
 * <p>
 * 该接口继承自 Spring Data 的 CrudRepository，能够自动为 Device 实体
 * 提供常见的 CRUD（Create/Read/Update/Delete）操作，无需手动实现实现类。
 *
 * CrudRepository<T, ID> 的泛型参数说明：
 * - T: 实体类型（这里为 com.example.demo.model.Device）
 * - ID: 实体主键类型（这里为 Long）
 *
 * <p>常用方法（来自 CrudRepository）示例：
 * - <T> S save(S entity)
 * - Optional<T> findById(ID id)
 * - boolean existsById(ID id)
 * - Iterable<T> findAll()
 * - long count()
 * - void deleteById(ID id)
 *
 * <p>本接口额外定义了按 IMEI 查找设备的方法 findByImei(String imei)，
 * Spring Data 会根据方法名自动解析并生成对应的查询实现。
 *
 * 重要说明：
 * - 请确保项目中已引入 Spring Data JPA 或 Spring Data 的相关依赖，
 *   并正确配置数据源，否则运行时会出现 Bean 或包找不到的错误。
 * - IMEI 在本系统中被视为设备唯一标识，应在数据库层设置唯一约束。
 */
@Repository
public interface DeviceRepository extends PagingAndSortingRepository<Device, Long> {

    /**
     * 根据设备 IMEI 查找单个设备。
     *
     * <p>方法名遵循 Spring Data 的命名约定（findBy + 属性名），
     * 框架会自动根据实体的 imei 字段产生对应的查询语句。
     *
     * @param imei 设备的 IMEI 字符串（通常为 15 位数字）
     * @return 如果找到则返回包含 Device 的 Optional，否则返回 Optional.empty()
     */
    Optional<Device> findByImei(String imei);

    // ----------------- 以下为常见 CRUD 方法的显式签名（可选，增强可读性） -----------------

    /**
     * 保存或更新设备实体（如果实体包含 id 则为更新，否则为插入）。
     *
     * @param device 要保存的设备实体
     * @param <S>    Device 或其子类型
     * @return 保存后的实体（包含生成的 id）
     */
    <S extends Device> S save(S device);

    /**
     * 根据 ID 查找设备。
     *
     * @param id 设备主键
     * @return 包含设备的 Optional（若不存在返回 Optional.empty()）
     */
    Optional<Device> findById(Long id);

    /**
     * 查询所有设备（返回可迭代集合）。
     *
     * @return 所有设备的迭代器集合
     */
    Iterable<Device> findAll();

    /**
     * 根据 ID 删除设备（若不存在则忽略）。
     *
     * @param id 要删除的设备主键
     */
    void deleteById(Long id);

    /**
     * 判断指定 ID 的设备是否存在。
     *
     * @param id 设备主键
     * @return 存在返回 true，否则 false
     */
    boolean existsById(Long id);

    /**
     * 返回设备总数。
     *
     * @return 设备数量
     */
    long count();
    
    /**
     * 查询所有未关联病人的设备。
     *
     * @return 未关联设备列表
     */
    @Query(value = "SELECT d.* FROM devices d WHERE d.id NOT IN (SELECT device_id FROM patient_devices WHERE is_active = true)", nativeQuery = true)
    List<Device> findAvailableDevices();
    
    /**
     * 根据IMEI、ICCID或IMSI模糊搜索设备
     * @param imei IMEI搜索关键词
     * @param iccid ICCID搜索关键词
     * @param imsi IMSI搜索关键词
     * @param pageable 分页参数
     * @return 分页设备列表
     */
    org.springframework.data.domain.Page<Device> findByImeiContainingOrIccidContainingOrImsiContaining(String imei, String iccid, String imsi, org.springframework.data.domain.Pageable pageable);
}
