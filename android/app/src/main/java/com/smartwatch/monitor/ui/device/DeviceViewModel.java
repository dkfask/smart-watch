package com.smartwatch.monitor.ui.device;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.smartwatch.monitor.api.ApiClient;
import com.smartwatch.monitor.api.DownlinkService;
import com.smartwatch.monitor.model.DeviceInfoDto;
import com.smartwatch.monitor.model.PageResponse;
import com.smartwatch.monitor.repository.DeviceRepository;
import com.smartwatch.monitor.utils.Constants;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 设备ViewModel，处理设备列表、详情、下行指令等数据逻辑
 */
public class DeviceViewModel extends AndroidViewModel {

    private final DeviceRepository deviceRepository;
    private final DownlinkService downlinkService;
    private final MutableLiveData<PageResponse<DeviceInfoDto>> devices = new MutableLiveData<>();
    private final MutableLiveData<DeviceInfoDto> deviceDetail = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Object>> commandResult = new MutableLiveData<>();

    private int currentPage = 0;
    private String currentSearch = "";
    private List<DeviceInfoDto> allDevices = new ArrayList<>();
    private boolean hasMore = true;

    /**
     * 构造函数，初始化设备仓库和下行指令服务
     * @param application 应用实例
     */
    public DeviceViewModel(@NonNull Application application) {
        super(application);
        deviceRepository = new DeviceRepository();
        downlinkService = ApiClient.getInstance().createService(DownlinkService.class);
    }

    /**
     * 加载设备列表，支持搜索和分页
     * @param search 搜索关键词
     * @param page 页码
     * @param size 每页数量
     */
    public void loadDevices(String search, int page, int size) {
        if (isLoading.getValue() != null && isLoading.getValue()) return;

        isLoading.setValue(true);
        errorMessage.setValue(null);

        if (page == 0) {
            allDevices.clear();
            hasMore = true;
        }
        currentPage = page;
        currentSearch = search;

        deviceRepository.getDevices(search, page).observeForever(result -> {
            isLoading.setValue(false);
            if (result != null) {
                if (page == 0) {
                    allDevices.clear();
                }
                if (result.getContent() != null) {
                    allDevices.addAll(result.getContent());
                }
                devices.setValue(result);
                hasMore = result.getContent() != null && result.getContent().size() >= Constants.DEFAULT_PAGE_SIZE;
            } else {
                errorMessage.setValue("加载设备列表失败");
            }
        });
    }

    /**
     * 加载设备详情
     * @param id 设备ID
     */
    public void loadDeviceDetail(long id) {
        isLoading.setValue(true);
        deviceRepository.getDevice(id).observeForever(result -> {
            isLoading.setValue(false);
            if (result != null) {
                deviceDetail.setValue(result);
            } else {
                errorMessage.setValue("加载设备详情失败");
            }
        });
    }

    /**
     * 发送下行指令到设备
     * @param imei 设备IMEI号
     * @param command 指令类型（如 bp16, bpxl, bpxy 等）
     * @param params 指令参数（可选）
     */
    public void sendDownlinkCommand(String imei, String command, Map<String, Object> params) {
        isLoading.setValue(true);
        Call<Map<String, Object>> call = buildCommandCall(imei, command, params);
        if (call == null) {
            isLoading.setValue(false);
            errorMessage.setValue("不支持的指令类型: " + command);
            return;
        }

        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    commandResult.setValue(response.body());
                } else {
                    errorMessage.setValue("指令发送失败");
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                isLoading.setValue(false);
                errorMessage.setValue("指令发送失败: " + t.getMessage());
            }
        });
    }

    /**
     * 根据指令类型构建对应的Retrofit Call对象
     * @param imei 设备IMEI号
     * @param command 指令类型
     * @param params 指令参数
     * @return Retrofit Call对象
     */
    private Call<Map<String, Object>> buildCommandCall(String imei, String command, Map<String, Object> params) {
        switch (command) {
            case "bp16":
                return downlinkService.sendBp16(imei);
            case "bpxl":
                return downlinkService.sendBpxl(imei);
            case "bpxy":
                return downlinkService.sendBpxy(imei);
            case "bpxz":
                return downlinkService.sendBpxz(imei);
            case "bpxx":
                return downlinkService.sendBpxx(imei);
            case "bp17":
                return downlinkService.sendBp17(imei);
            case "bp18":
                return downlinkService.sendBp18(imei);
            case "bp31":
                return downlinkService.sendBp31(imei);
            default:
                return null;
        }
    }

    /**
     * 刷新当前列表（重置到第一页）
     */
    public void refresh() {
        loadDevices(currentSearch, 0, Constants.DEFAULT_PAGE_SIZE);
    }

    /**
     * 加载下一页数据
     */
    public void loadNextPage() {
        if (hasMore) {
            loadDevices(currentSearch, currentPage + 1, Constants.DEFAULT_PAGE_SIZE);
        }
    }

    /**
     * 获取设备列表LiveData
     * @return 设备分页数据
     */
    public LiveData<PageResponse<DeviceInfoDto>> getDevices() { return devices; }

    /**
     * 获取设备详情LiveData
     * @return 设备详情数据
     */
    public LiveData<DeviceInfoDto> getDeviceDetail() { return deviceDetail; }

    /**
     * 获取加载状态LiveData
     * @return 是否正在加载
     */
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    /**
     * 获取错误信息LiveData
     * @return 错误信息字符串
     */
    public LiveData<String> getErrorMessage() { return errorMessage; }

    /**
     * 获取指令执行结果LiveData
     * @return 指令执行结果
     */
    public LiveData<Map<String, Object>> getCommandResult() { return commandResult; }

    /**
     * 是否还有更多数据可加载
     * @return 是否有更多数据
     */
    public boolean hasMore() { return hasMore; }

    /**
     * 获取当前页码
     * @return 当前页码
     */
    public int getCurrentPage() { return currentPage; }

    /**
     * 获取所有已加载的设备列表
     * @return 设备列表
     */
    public List<DeviceInfoDto> getAllDevices() { return allDevices; }
}
