package com.waimai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.waimai.common.dto.SplitOrderDTO;
import com.waimai.common.entity.*;
import com.waimai.common.exception.BusinessException;
import com.waimai.common.utils.SnowflakeUtil;
import com.waimai.service.mapper.DeliveryBatchMapper;
import com.waimai.service.mapper.DeliverySubTaskMapper;
import com.waimai.service.mapper.OrderMapper;
import com.waimai.service.mapper.RiderMapper;
import com.waimai.service.service.DeliveryBatchService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DeliveryBatchServiceImpl implements DeliveryBatchService {

    private final DeliveryBatchMapper batchMapper;
    private final DeliverySubTaskMapper subTaskMapper;
    private final OrderMapper orderMapper;
    private final RiderMapper riderMapper;
    private final SnowflakeUtil snowflakeUtil;

    public DeliveryBatchServiceImpl(DeliveryBatchMapper batchMapper,
                                    DeliverySubTaskMapper subTaskMapper,
                                    OrderMapper orderMapper,
                                    RiderMapper riderMapper,
                                    SnowflakeUtil snowflakeUtil) {
        this.batchMapper = batchMapper;
        this.subTaskMapper = subTaskMapper;
        this.orderMapper = orderMapper;
        this.riderMapper = riderMapper;
        this.snowflakeUtil = snowflakeUtil;
    }

    @Override
    @Transactional
    public DeliveryBatch splitOrder(SplitOrderDTO dto) {
        Order order = orderMapper.selectById(dto.getOrderId());
        if (order == null) throw new BusinessException("订单不存在");

        DeliveryBatch batch = new DeliveryBatch();
        batch.setOrderId(dto.getOrderId());
        batch.setBatchNo(String.valueOf(snowflakeUtil.nextId()));
        batch.setTotalSubCount(dto.getSubTasks().size());
        batch.setCompletedSubCount(0);
        batch.setStatus("DISPATCHING");
        batchMapper.insert(batch);

        for (SplitOrderDTO.SubTaskInfo info : dto.getSubTasks()) {
            DeliverySubTask sub = new DeliverySubTask();
            sub.setBatchId(batch.getId());
            sub.setOrderId(dto.getOrderId());
            sub.setSubAddress(info.getAddress());
            sub.setSubAddressLng(info.getAddressLng());
            sub.setSubAddressLat(info.getAddressLat());
            sub.setItemsJson(info.getItemsJson());
            sub.setStatus("PENDING");
            subTaskMapper.insert(sub);
        }

        return batch;
    }

    @Override
    @Transactional
    public void assignRiderToSubTask(Long subTaskId, Long riderId) {
        DeliverySubTask sub = subTaskMapper.selectById(subTaskId);
        if (sub == null) throw new BusinessException("子任务不存在");
        if (!"PENDING".equals(sub.getStatus())) throw new BusinessException("子任务已被分配");

        Rider rider = riderMapper.selectById(riderId);
        if (rider == null) throw new BusinessException("骑手不存在");

        sub.setRiderId(riderId);
        sub.setStatus("ASSIGNED");
        sub.setDeliverTime(LocalDateTime.now());
        subTaskMapper.updateById(sub);

        DeliveryBatch batch = batchMapper.selectById(sub.getBatchId());
        if (batch != null && "DISPATCHING".equals(batch.getStatus())) {
            batch.setStatus("IN_PROGRESS");

            // Assign remaining sub tasks to the same rider if needed
            batchMapper.updateById(batch);
        }
    }

    @Override
    @Transactional
    public void completeSubTask(Long subTaskId, Long riderId) {
        DeliverySubTask sub = subTaskMapper.selectById(subTaskId);
        if (sub == null) throw new BusinessException("子任务不存在");
        if (!riderId.equals(sub.getRiderId())) throw new BusinessException("您不是该子任务的配送骑手");

        sub.setStatus("COMPLETED");
        sub.setCompleteTime(LocalDateTime.now());
        subTaskMapper.updateById(sub);

        // Check if all sub-tasks done
        DeliveryBatch batch = batchMapper.selectById(sub.getBatchId());
        if (batch != null) {
            long completed = subTaskMapper.selectCount(new LambdaQueryWrapper<DeliverySubTask>()
                    .eq(DeliverySubTask::getBatchId, batch.getId())
                    .eq(DeliverySubTask::getStatus, "COMPLETED"));
            batch.setCompletedSubCount((int) completed);
            if (completed >= batch.getTotalSubCount()) {
                batch.setStatus("COMPLETED");
            }
            batchMapper.updateById(batch);

            // If all done, complete the main order too
            if (batch.getStatus().equals("COMPLETED")) {
                Order order = orderMapper.selectById(batch.getOrderId());
                if (order != null) {
                    order.setStatus(com.waimai.common.constant.OrderStatus.COMPLETED);
                    order.setCompleteTime(LocalDateTime.now());
                    orderMapper.updateById(order);
                }
            }
        }
    }

    @Override
    public List<DeliverySubTask> listSubTasksByBatch(Long batchId) {
        return subTaskMapper.selectList(new LambdaQueryWrapper<DeliverySubTask>()
                .eq(DeliverySubTask::getBatchId, batchId)
                .orderByAsc(DeliverySubTask::getCreateTime));
    }

    @Override
    public List<DeliverySubTask> listSubTasksByRider(Long riderId) {
        return subTaskMapper.selectList(new LambdaQueryWrapper<DeliverySubTask>()
                .eq(DeliverySubTask::getRiderId, riderId)
                .orderByDesc(DeliverySubTask::getCreateTime));
    }

    @Override
    public Map<String, Object> getBatchProgress(Long orderId) {
        List<DeliveryBatch> batches = batchMapper.selectList(new LambdaQueryWrapper<DeliveryBatch>()
                .eq(DeliveryBatch::getOrderId, orderId)
                .orderByDesc(DeliveryBatch::getCreateTime));

        Map<String, Object> result = new HashMap<>();
        if (batches.isEmpty()) {
            result.put("hasBatch", false);
            return result;
        }

        DeliveryBatch batch = batches.get(0);
        result.put("hasBatch", true);
        result.put("batch", batch);

        List<DeliverySubTask> subs = listSubTasksByBatch(batch.getId());
        result.put("subTasks", subs);
        result.put("progress", batch.getTotalSubCount() > 0
                ? (batch.getCompletedSubCount() * 100 / batch.getTotalSubCount()) + "%"
                : "0%");

        return result;
    }
}
