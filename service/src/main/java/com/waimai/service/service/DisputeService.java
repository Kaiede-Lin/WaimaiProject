package com.waimai.service.service;

import com.waimai.common.dto.CreateDisputeDTO;
import com.waimai.common.dto.ReportExceptionDTO;
import com.waimai.common.dto.RequestRefundDTO;
import com.waimai.common.entity.DeliveryException;
import com.waimai.common.entity.OrderDispute;

import java.util.List;

public interface DisputeService {
    OrderDispute createDispute(Long userId, CreateDisputeDTO dto);
    List<OrderDispute> listByUser(Long userId);
    List<OrderDispute> listAll(String status);
    void resolve(Long disputeId, String status, String remark, String resolution);

    // Refund
    OrderDispute requestRefund(Long userId, RequestRefundDTO dto);
    void cancelRefund(Long userId, Long disputeId);

    // Merchant
    List<OrderDispute> listByMerchant(Long merchantId, String refundStatus);
    void handleByMerchant(Long merchantId, Long disputeId, boolean approved, String remark);

    // Rider exception
    DeliveryException reportException(Long riderId, ReportExceptionDTO dto);
    List<DeliveryException> listExceptionsByRider(Long riderId);
}
