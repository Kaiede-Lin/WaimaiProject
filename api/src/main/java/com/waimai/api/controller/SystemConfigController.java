package com.waimai.api.controller;

import com.waimai.common.Result;
import com.waimai.common.entity.SystemConfig;
import com.waimai.service.service.SystemConfigService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/config")
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    public SystemConfigController(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    @GetMapping
    public Result<List<SystemConfig>> listAll() {
        return Result.ok(systemConfigService.listAll());
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        systemConfigService.updateConfig(id, body.get("configValue"));
        return Result.ok();
    }

    @PostMapping("/reload")
    public Result<?> reloadCache() {
        systemConfigService.reloadCache();
        return Result.ok();
    }
}
