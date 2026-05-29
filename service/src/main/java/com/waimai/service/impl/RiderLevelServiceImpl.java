package com.waimai.service.impl;

import com.waimai.common.constant.RiderLevel;
import com.waimai.common.entity.Rider;
import com.waimai.service.mapper.RiderMapper;
import com.waimai.service.service.RiderLevelService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class RiderLevelServiceImpl implements RiderLevelService {

    private final RiderMapper riderMapper;

    public RiderLevelServiceImpl(RiderMapper riderMapper) {
        this.riderMapper = riderMapper;
    }

    @Override
    public void recalculateLevel(Long riderId) {
        Rider rider = riderMapper.selectById(riderId);
        if (rider == null) return;

        // Level score = completedOrders * 2 + avgRating * 20
        int ordersScore = (rider.getTotalOrders() != null ? rider.getTotalOrders() : 0) * 2;
        int ratingScore = (int) ((rider.getScore() != null ? rider.getScore().doubleValue() : 5.0) * 20);
        int levelScore = ordersScore + ratingScore;

        rider.setLevelScore(levelScore);
        rider.setLevel(RiderLevel.computeLevel(levelScore));
        riderMapper.updateById(rider);
    }
}
