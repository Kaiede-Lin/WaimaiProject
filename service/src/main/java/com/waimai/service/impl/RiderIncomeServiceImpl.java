package com.waimai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.waimai.common.entity.Rider;
import com.waimai.common.entity.RiderIncome;
import com.waimai.common.entity.RiderWithdrawal;
import com.waimai.common.exception.BusinessException;
import com.waimai.service.mapper.RiderIncomeMapper;
import com.waimai.service.mapper.RiderMapper;
import com.waimai.service.mapper.RiderWithdrawalMapper;
import com.waimai.service.service.RiderIncomeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RiderIncomeServiceImpl implements RiderIncomeService {

    private final RiderIncomeMapper riderIncomeMapper;
    private final RiderWithdrawalMapper riderWithdrawalMapper;
    private final RiderMapper riderMapper;

    public RiderIncomeServiceImpl(RiderIncomeMapper riderIncomeMapper,
                                  RiderWithdrawalMapper riderWithdrawalMapper,
                                  RiderMapper riderMapper) {
        this.riderIncomeMapper = riderIncomeMapper;
        this.riderWithdrawalMapper = riderWithdrawalMapper;
        this.riderMapper = riderMapper;
    }

    @Override
    @Transactional
    public void recordIncome(Long riderId, Long orderId, BigDecimal amount) {
        RiderIncome income = new RiderIncome();
        income.setRiderId(riderId);
        income.setOrderId(orderId);
        income.setAmount(amount);
        income.setType("DELIVERY");
        riderIncomeMapper.insert(income);

        Rider rider = riderMapper.selectById(riderId);
        if (rider != null) {
            BigDecimal newBalance = (rider.getBalance() != null ? rider.getBalance() : BigDecimal.ZERO).add(amount);
            rider.setBalance(newBalance);
            riderMapper.updateById(rider);
        }
    }

    @Override
    public Map<String, Object> getIncomeSummary(Long riderId) {
        Rider rider = riderMapper.selectById(riderId);
        if (rider == null) throw new BusinessException("骑手不存在");

        List<RiderIncome> allIncome = riderIncomeMapper.selectList(
                new LambdaQueryWrapper<RiderIncome>().eq(RiderIncome::getRiderId, riderId));

        BigDecimal totalIncome = allIncome.stream()
                .map(RiderIncome::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // This month income
        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        BigDecimal monthIncome = allIncome.stream()
                .filter(i -> i.getCreateTime() != null && i.getCreateTime().isAfter(monthStart))
                .map(RiderIncome::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Today income
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        BigDecimal todayIncome = allIncome.stream()
                .filter(i -> i.getCreateTime() != null && i.getCreateTime().isAfter(todayStart))
                .map(RiderIncome::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalIncome", totalIncome);
        summary.put("balance", rider.getBalance() != null ? rider.getBalance() : BigDecimal.ZERO);
        summary.put("monthIncome", monthIncome);
        summary.put("todayIncome", todayIncome);
        summary.put("totalOrders", rider.getTotalOrders());
        summary.put("totalDeliveries", allIncome.size());
        return summary;
    }

    @Override
    public List<RiderIncome> listIncome(Long riderId, int page, int size) {
        Page<RiderIncome> result = riderIncomeMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<RiderIncome>()
                        .eq(RiderIncome::getRiderId, riderId)
                        .orderByDesc(RiderIncome::getCreateTime));
        return result.getRecords();
    }

    @Override
    @Transactional
    public RiderWithdrawal requestWithdrawal(Long riderId, BigDecimal amount) {
        Rider rider = riderMapper.selectById(riderId);
        if (rider == null) throw new BusinessException("骑手不存在");

        BigDecimal balance = rider.getBalance() != null ? rider.getBalance() : BigDecimal.ZERO;
        if (balance.compareTo(amount) < 0) {
            throw new BusinessException("余额不足，当前余额: " + balance);
        }

        rider.setBalance(balance.subtract(amount));
        riderMapper.updateById(rider);

        RiderWithdrawal withdrawal = new RiderWithdrawal();
        withdrawal.setRiderId(riderId);
        withdrawal.setAmount(amount);
        withdrawal.setStatus("PENDING");
        riderWithdrawalMapper.insert(withdrawal);

        return withdrawal;
    }

    @Override
    public List<RiderWithdrawal> listWithdrawals(Long riderId) {
        return riderWithdrawalMapper.selectList(
                new LambdaQueryWrapper<RiderWithdrawal>()
                        .eq(RiderWithdrawal::getRiderId, riderId)
                        .orderByDesc(RiderWithdrawal::getCreateTime));
    }
}
