package com.finance.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finance.api.entity.PriceAlert;
import org.apache.ibatis.annotations.Mapper;

/**
 * 价格提醒 Mapper
 */
@Mapper
public interface PriceAlertMapper extends BaseMapper<PriceAlert> {
}
