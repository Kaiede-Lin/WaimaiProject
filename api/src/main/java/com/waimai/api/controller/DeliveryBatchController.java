package com.waimai.api.controller;

import com.waimai.common.Result;
import com.waimai.common.dto.SplitOrderDTO;
import com.waimai.common.entity.DeliveryBatch;
import com.waimai.common.entity.DeliverySubTask;
import com.waimai.service.service.DeliveryBatchService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/delivery")
public class DeliveryBatchController {

    private final DeliveryBatchService deliveryBatchService;

    public DeliveryBatchController(DeliveryBatchService deliveryBatchService) {
        this.deliveryBatchService = deliveryBatchService;
    }

    // ── Admin: split a large order into sub-tasks ──────────────

    @PostMapping("/batch/split")
    public Result<DeliveryBatch> splitOrder(@RequestBody SplitOrderDTO dto) {
        return Result.ok(deliveryBatchService.splitOrder(dto));
    }

    @GetMapping("/batch/progress/{orderId}")
    public Result<Map<String, Object>> batchProgress(@PathVariable Long orderId) {
        return Result.ok(deliveryBatchService.getBatchProgress(orderId));
    }

    @GetMapping("/batch/{batchId}/subtasks")
    public Result<List<DeliverySubTask>> subTasks(@PathVariable Long batchId) {
        return Result.ok(deliveryBatchService.listSubTasksByBatch(batchId));
    }

    // ── Rider: manage assigned sub-tasks ──────────────────────

    @GetMapping("/subtask/my")
    public Result<List<DeliverySubTask>> mySubTasks(@RequestParam Long riderId) {
        return Result.ok(deliveryBatchService.listSubTasksByRider(riderId));
    }

    @PostMapping("/subtask/{subTaskId}/assign")
    public Result<?> assignRider(@PathVariable Long subTaskId, @RequestBody Map<String, Long> body) {
        deliveryBatchService.assignRiderToSubTask(subTaskId, body.get("riderId"));
        return Result.ok();
    }

    @PostMapping("/subtask/{subTaskId}/complete")
    public Result<?> completeSubTask(@PathVariable Long subTaskId, @RequestBody Map<String, Long> body) {
        deliveryBatchService.completeSubTask(subTaskId, body.get("riderId"));
        return Result.ok();
    }
}
