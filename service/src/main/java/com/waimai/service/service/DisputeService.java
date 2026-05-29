package com.waimai.service.service;

import com.waimai.common.dto.CreateDisputeDTO;
import com.waimai.common.entity.OrderDispute;

import java.util.List;

public interface DisputeService {
    OrderDispute createDispute(Long userId, CreateDisputeDTO dto);
    List<OrderDispute> listByUser(Long userId);
    List<OrderDispute> listAll(String status);
    void resolve(Long disputeId, String status, String remark, String resolution);
}
