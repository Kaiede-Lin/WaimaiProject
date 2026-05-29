package com.waimai.api.controller;

import com.waimai.common.Result;
import com.waimai.common.dto.CreateDisputeDTO;
import com.waimai.common.entity.OrderDispute;
import com.waimai.common.utils.UserContext;
import com.waimai.service.service.DisputeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dispute")
public class DisputeController {

    private final DisputeService disputeService;

    public DisputeController(DisputeService disputeService) {
        this.disputeService = disputeService;
    }

    @PostMapping("/create")
    public Result<OrderDispute> create(@Valid @RequestBody CreateDisputeDTO dto) {
        return Result.ok(disputeService.createDispute(UserContext.getUserId(), dto));
    }

    @GetMapping("/my")
    public Result<List<OrderDispute>> my() {
        return Result.ok(disputeService.listByUser(UserContext.getUserId()));
    }

    @GetMapping("/admin/list")
    public Result<List<OrderDispute>> adminList(@RequestParam(required = false) String status) {
        return Result.ok(disputeService.listAll(status));
    }

    @PutMapping("/admin/{id}/resolve")
    public Result<?> resolve(@PathVariable Long id, @RequestBody Map<String, String> body) {
        disputeService.resolve(id,
                body.get("status"),
                body.get("adminRemark"),
                body.get("resolution"));
        return Result.ok();
    }
}
