package com.waimai.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.waimai.common.entity.Merchant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface MerchantMapper extends BaseMapper<Merchant> {

    @Select("SELECT * FROM merchant WHERE status = 1 " +
            "AND latitude BETWEEN #{minLat} AND #{maxLat} " +
            "AND longitude BETWEEN #{minLng} AND #{maxLng}")
    List<Merchant> selectNearby(@Param("minLng") double minLng, @Param("maxLng") double maxLng,
                                @Param("minLat") double minLat, @Param("maxLat") double maxLat);
}
