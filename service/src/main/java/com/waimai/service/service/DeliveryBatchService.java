package com.waimai.service.service;

import com.waimai.common.dto.SplitOrderDTO;
import com.waimai.common.entity.DeliveryBatch;
import com.waimai.common.entity.DeliverySubTask;

import java.util.List;
import java.util.Map;

public interface DeliveryBatchService {
    DeliveryBatch splitOrder(SplitOrderDTO dto);
    void assignRiderToSubTask(Long subTaskId, Long riderId);
    void completeSubTask(Long subTaskId, Long riderId);
    List<DeliverySubTask> listSubTasksByBatch(Long batchId);
    List<DeliverySubTask> listSubTasksByRider(Long riderId);
    Map<String, Object> getBatchProgress(Long orderId);
}
