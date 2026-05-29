package com.waimai.service.service;

import com.waimai.common.entity.RiderIncome;
import com.waimai.common.entity.RiderWithdrawal;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface RiderIncomeService {
    void recordIncome(Long riderId, Long orderId, BigDecimal amount);
    Map<String, Object> getIncomeSummary(Long riderId);
    List<RiderIncome> listIncome(Long riderId, int page, int size);
    RiderWithdrawal requestWithdrawal(Long riderId, BigDecimal amount);
    List<RiderWithdrawal> listWithdrawals(Long riderId);
}
