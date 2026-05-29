package com.waimai.service.service;

import com.waimai.common.entity.SystemConfig;
import java.util.List;

public interface SystemConfigService {
    List<SystemConfig> listAll();
    void updateConfig(Long id, String configValue);
    String getConfigValue(String key);
    void reloadCache();
    int fixEncoding();
}
