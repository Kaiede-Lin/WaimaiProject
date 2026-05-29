package com.waimai.service.impl;

import com.waimai.common.entity.SystemConfig;
import com.waimai.common.exception.BusinessException;
import com.waimai.service.mapper.SystemConfigMapper;
import com.waimai.service.service.SystemConfigService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class SystemConfigServiceImpl implements SystemConfigService {

    private static final String CACHE_PREFIX = "waimai:config:";
    private static final long CACHE_TTL = 3600;

    private final SystemConfigMapper systemConfigMapper;
    private final RedisTemplate<String, String> redisTemplate;

    public SystemConfigServiceImpl(SystemConfigMapper systemConfigMapper,
                                   RedisTemplate<String, String> redisTemplate) {
        this.systemConfigMapper = systemConfigMapper;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public List<SystemConfig> listAll() {
        return systemConfigMapper.selectList(null);
    }

    @Override
    public void updateConfig(Long id, String configValue) {
        SystemConfig config = systemConfigMapper.selectById(id);
        if (config == null) throw new BusinessException("配置不存在");
        config.setConfigValue(configValue);
        systemConfigMapper.updateById(config);
        // Update cache
        redisTemplate.opsForValue().set(CACHE_PREFIX + config.getConfigKey(), configValue, CACHE_TTL, TimeUnit.SECONDS);
    }

    @Override
    public String getConfigValue(String key) {
        String cached = redisTemplate.opsForValue().get(CACHE_PREFIX + key);
        if (cached != null) return cached;

        SystemConfig config = systemConfigMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SystemConfig>()
                        .eq(SystemConfig::getConfigKey, key)).stream().findFirst().orElse(null);
        if (config == null) return null;

        redisTemplate.opsForValue().set(CACHE_PREFIX + key, config.getConfigValue(), CACHE_TTL, TimeUnit.SECONDS);
        return config.getConfigValue();
    }

    @Override
    public void reloadCache() {
        List<SystemConfig> all = systemConfigMapper.selectList(null);
        for (SystemConfig config : all) {
            redisTemplate.opsForValue().set(CACHE_PREFIX + config.getConfigKey(), config.getConfigValue(), CACHE_TTL, TimeUnit.SECONDS);
        }
    }

    @Override
    public int fixEncoding() {
        int count = 0;
        List<SystemConfig> all = systemConfigMapper.selectList(null);
        for (SystemConfig config : all) {
            String desc = config.getDescription();
            if (desc == null) continue;
            String fixed = fixGarbledText(config.getConfigKey());
            if (fixed != null && !fixed.equals(desc)) {
                config.setDescription(fixed);
                systemConfigMapper.updateById(config);
                count++;
            }
        }
        return count;
    }

    private String fixGarbledText(String configKey) {
        return switch (configKey) {
            case "delivery_fee_default" -> "默认配送费(元)";
            case "auto_cancel_minutes" -> "未支付自动取消时间(分钟)";
            case "max_delivery_radius" -> "最大配送半径(公里)";
            case "rider_income_per_order" -> "骑手每单基础收入(元)";
            default -> null;
        };
    }
}
